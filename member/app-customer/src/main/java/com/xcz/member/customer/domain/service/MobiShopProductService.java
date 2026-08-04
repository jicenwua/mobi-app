package com.xcz.member.customer.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.customer.domain.dto.product.ProductCategoryBriefDTO;
import com.xcz.member.customer.infrastructure.entity.MobiShopProduct;

import java.util.Collection;
import java.util.List;

/**
 * 店铺商品持久化服务（mobi_shop_product）。
 */
public interface MobiShopProductService extends IService<MobiShopProduct> {

    /**
     * 按店铺 ID 获取全量商品。
     *
     * @param shopId 店铺 ID
     * @return 商品列表
     */
    List<MobiShopProduct> listEntitiesByShopId(Long shopId);

    /**
     * 按商品 ID 批量查询（结算场景，避免 N+1）。
     *
     * @param productIds 商品 ID 集合
     * @return 商品列表
     */
    List<MobiShopProduct> listByProductIds(Collection<Long> productIds);

    /**
     * 确保商品库存 Redis 缓存已加载（读/写统一入口）。
     *
     * @param productId 商品 ID
     */
    void ensureStockCached(Long productId);

    /**
     * 批量预热商品库存缓存，避免目录/结算场景 N+1。
     *
     * @param productIds 商品 ID 集合
     */
    void ensureStockCachedBatch(Collection<Long> productIds);

    /**
     * 添加商品库存（Redis 原子操作）。
     *
     * @param product 商品 ID
     * @param quantity  添加数量
     */
    void addStock(MobiShopProduct product, long quantity);

    /**
     * 扣减商品库存（Redis 原子操作，售罄时更新 DB 状态并失效目录缓存）。
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量
     */
    void deductStock(Long productId, long quantity);

    /**
     * 批量扣减商品库存（同一订单在联锁下整单扣减）。
     *
     * @param deductions 扣减明细，每项为 [productId, quantity]
     */
    void deductStockBatch(List<long[]> deductions);

    /**
     * 回滚一次库存扣减。
     *
     * @param productId 商品 ID
     * @param quantity  回滚数量
     */
    void rollbackDeduct(Long productId, long quantity);

    /**
     * 从数据库加载店铺商品目录（分类 + 商品分组）。
     *
     * @param shopId 店铺 ID
     * @return 商品目录读模型
     */
    List<ProductCategoryBriefDTO> listProductsByShopId(Long shopId);
}
