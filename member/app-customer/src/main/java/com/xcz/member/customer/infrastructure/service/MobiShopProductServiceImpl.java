package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.domain.dto.product.ProductCategoryBriefDTO;
import com.xcz.member.customer.domain.enums.ProductStatus;
import com.xcz.member.customer.domain.service.MobiShopProductCategoryService;
import com.xcz.member.customer.domain.service.MobiShopProductService;
import com.xcz.member.customer.infrastructure.cache.ProductCache;
import com.xcz.member.customer.infrastructure.cache.ProductStockCache;
import com.xcz.member.customer.infrastructure.entity.MobiShopProduct;
import com.xcz.member.customer.infrastructure.entity.MobiShopProductCategory;
import com.xcz.member.customer.infrastructure.mapper.MobiShopProductMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;

/**
 * 店铺商品持久化服务实现。
 * <p>
 * 写操作成功后自动维护 {@link ProductCache} 与 {@link ProductStockCache}，应用层无需手动失效缓存。
 */
@Service
public class MobiShopProductServiceImpl extends ServiceImpl<MobiShopProductMapper, MobiShopProduct>
        implements MobiShopProductService {

    @Resource
    private MobiShopProductCategoryService mobiShopProductCategoryService;

    /** 新增商品后初始化库存缓存并失效店铺商品目录缓存。 */
    @Override
    public boolean save(MobiShopProduct entity) {
        boolean saved = super.save(entity);
        if (saved) {
            initStockCache(entity);
            ProductCache.evict(entity.getShopId());
        }
        return saved;
    }

    /** 更新商品后失效店铺商品目录缓存。 */
    @Override
    public boolean updateById(MobiShopProduct entity) {
        boolean updated = super.updateById(entity);
        if (updated) {
            ProductCache.evict(entity.getShopId());
        }
        return updated;
    }

    /** 删除商品后清理库存缓存并失效店铺商品目录缓存。 */
    @Override
    public boolean removeById(Serializable id) {
        MobiShopProduct existing = getById(id);
        boolean removed = super.removeById(id);
        if (removed && existing != null) {
            ProductStockCache.evict(existing.getProductId());
            ProductCache.evict(existing.getShopId());
        }
        return removed;
    }

    /**
     * 查询店铺下全部商品实体（含已下架），按创建时间降序。
     *
     * @param shopId 店铺 ID
     * @return 商品实体列表
     */
    @Override
    public List<MobiShopProduct> listEntitiesByShopId(Long shopId) {
        return list(new LambdaQueryWrapper<MobiShopProduct>()
                .eq(MobiShopProduct::getShopId, shopId)
                .orderByDesc(MobiShopProduct::getCreateTime));
    }

    /**
     * 按商品 ID 批量查询实体。
     *
     * @param productIds 商品 ID 集合
     * @return 商品实体列表；空集合时返回空列表
     */
    @Override
    public List<MobiShopProduct> listByProductIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        return listByIds(productIds);
    }

    /**
     * 确保单个商品库存已加载到 Redis 缓存（懒加载）。
     *
     * @param productId 商品 ID
     */
    @Override
    public void ensureStockCached(Long productId) {
        if (productId == null) {
            throw new ServiceException("商品ID不能为空");
        }
        // 缓存未命中时从数据库加载并写入 Redis
        if (ProductStockCache.isNotExists(productId)) {
            MobiShopProduct product = getById(productId);
            if (product == null) {
                return;
            }
            initStockCache(product);
        }
    }

    /**
     * 批量确保商品库存已加载到 Redis 缓存。
     *
     * @param productIds 商品 ID 集合
     */
    @Override
    public void ensureStockCachedBatch(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        List<Long> missing = productIds.stream()
                .filter(Objects::nonNull)
                .filter(ProductStockCache::isNotExists)
                .distinct()
                .toList();
        if (missing.isEmpty()) {
            return;
        }
        List<MobiShopProduct> products = listByIds(missing);
        for (MobiShopProduct product : products) {
            initStockCache(product);
        }
    }

    /**
     * 追加商品库存：支持有限库存累加与无限库存切换。
     *
     * @param product  商品实体（须含 productId）
     * @param quantity 追加数量；{@link ProductStockCache#UNLIMITED_STOCK} 表示设为无限库存
     */
    @Override
    public void addStock(MobiShopProduct product, long quantity) {
        long available = ProductStockCache.getAvailableStock(product.getProductId());
        // 无限库存与有限库存之间的切换需重建缓存
        if (available == ProductStockCache.UNLIMITED_STOCK || quantity == ProductStockCache.UNLIMITED_STOCK) {
            product.setStock(quantity);
            this.updateById(product);
            ProductStockCache.evict(product.getProductId());
            long soldCount = product.getSoldCount() == null ? 0L : product.getSoldCount();
            ProductStockCache.init(product.getProductId(), quantity, soldCount);
        } else {
            // 有限库存：Redis 原子累加后回写数据库
            long stock = ProductStockCache.addStock(product.getProductId(), quantity);
            product.setStock(stock);
            this.updateById(product);
        }
        restoreOnSaleIfRestocked(product);
    }

    /**
     * 扣减单个商品库存；库存归零时自动标记售罄并失效商品目录缓存。
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量
     */
    @Override
    public void deductStock(Long productId, long quantity) {
        MobiShopProduct product = getById(productId);
        if (product == null) {
            throw new ServiceException("商品不存在");
        }
        ensureStockCached(productId);
        long remaining = ProductStockCache.deductStock(productId, quantity);
        markSoldOutIfNeeded(product, remaining);
    }

    /**
     * 批量扣减库存，deductions 每项为 [productId, quantity]。
     *
     * @param deductions 扣减明细列表
     */
    @Override
    public void deductStockBatch(List<long[]> deductions) {
        if (deductions == null || deductions.isEmpty()) {
            return;
        }
        Set<Long> productIds = new HashSet<>();
        for (long[] item : deductions) {
            productIds.add(item[0]);
        }
        List<MobiShopProduct> products = listByIds(productIds);
        if (products.size() != productIds.size()) {
            throw new ServiceException("商品不存在");
        }
        ensureStockCachedBatch(productIds);
        Map<Long, Long> remainingMap = ProductStockCache.deductStockBatch(deductions);
        if (remainingMap.isEmpty()) {
            return;
        }
        for (MobiShopProduct product : products) {
            Long remaining = remainingMap.get(product.getProductId());
            if (remaining != null) {
                markSoldOutIfNeeded(product, remaining);
            }
        }
    }

    private void markSoldOutIfNeeded(MobiShopProduct product, long remaining) {
        if (remaining == 0 && product.getStatus() != ProductStatus.OFF_SHELF.getCode()) {
            update(new LambdaUpdateWrapper<MobiShopProduct>()
                    .eq(MobiShopProduct::getProductId, product.getProductId())
                    .set(MobiShopProduct::getStatus, ProductStatus.SOLD_OUT.getCode()));
            ProductCache.evict(product.getShopId());
        }
    }

    /**
     * 售完商品补货后自动恢复为「出售中」。
     */
    private void restoreOnSaleIfRestocked(MobiShopProduct product) {
        if (product.getStatus() == null || product.getStatus() != ProductStatus.SOLD_OUT.getCode()) {
            return;
        }
        ensureStockCached(product.getProductId());
        long available = ProductStockCache.getAvailableStock(product.getProductId());
        if (available == ProductStockCache.UNLIMITED_STOCK || available > 0) {
            product.setStatus(ProductStatus.ON_SALE.getCode());
            updateById(product);
        }
    }

    /**
     * 回滚 Redis 中的库存扣减（事务补偿场景）。
     *
     * @param productId 商品 ID
     * @param quantity  回滚数量
     */
    @Override
    public void rollbackDeduct(Long productId, long quantity) {
        ProductStockCache.rollbackDeduct(productId, quantity);
    }

    /**
     * 按店铺 ID 组装商品分类与商品的读模型列表。
     *
     * @param shopId 店铺 ID
     * @return 分类维度的商品简要信息列表
     */
    @Override
    public List<ProductCategoryBriefDTO> listProductsByShopId(Long shopId) {
        List<MobiShopProductCategory> categories = mobiShopProductCategoryService.listByShopId(shopId);
        List<MobiShopProduct> products = this.list(new LambdaQueryWrapper<MobiShopProduct>()
                .eq(MobiShopProduct::getShopId, shopId)
                .orderByDesc(MobiShopProduct::getCreateTime));
        return ProductCategoryBriefDTO.ofList(categories, products);
    }

    private static void initStockCache(MobiShopProduct product) {
        if (product == null || product.getProductId() == null) {
            return;
        }
        long stock = product.getStock() == null ? 0L : product.getStock();
        long soldCount = product.getSoldCount() == null ? 0L : product.getSoldCount();
        ProductStockCache.init(product.getProductId(), stock, soldCount);
    }
}
