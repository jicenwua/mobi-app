package com.xcz.member.customer.infrastructure.task.service;

import com.xcz.member.customer.domain.enums.ProductStatus;
import com.xcz.member.customer.domain.service.MobiShopProductService;
import com.xcz.member.customer.infrastructure.cache.ProductCache;
import com.xcz.member.customer.infrastructure.cache.ProductStockCache;
import com.xcz.member.customer.infrastructure.entity.MobiShopProduct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 将 Redis 中的库存与累计销量同步至数据库。
 */
@Slf4j
@Service
public class ProductSoldSyncService {

    @Resource
    private MobiShopProductService mobiShopProductService;

    /**
     * 同步所有待刷新的商品已售数据。
     */
    public void syncAll(ProductSoldSyncService self) {
        Set<Long> productIds = ProductStockCache.listDirtyProductIds();
        for (Long productId : productIds) {
            try {
                self.syncProductSold(productId);
            } catch (Exception e) {
                log.warn("同步商品已售数量失败, productId={}", productId, e);
            }
        }
    }

    /**
     * 同步单个商品的 Redis 库存与累计销量到数据库。
     *
     * @param productId 商品 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncProductSold(Long productId) {
        MobiShopProduct product = mobiShopProductService.getById(productId);
        if (product == null) {
            ProductStockCache.clearDirty(productId);
            return;
        }
        mobiShopProductService.ensureStockCached(productId);
        long remaining = ProductStockCache.getAvailableStock(productId);
        long soldCount = ProductStockCache.getPendingSold(productId);
        product.setStock(remaining);
        product.setSoldCount(soldCount);
        if (remaining == 0
                && product.getStatus() != null
                && product.getStatus() != ProductStatus.OFF_SHELF.getCode()) {
            product.setStatus(ProductStatus.SOLD_OUT.getCode());
            ProductCache.evict(product.getShopId());
        }
        mobiShopProductService.updateById(product);
        ProductStockCache.clearDirty(productId);
    }
}
