package com.xcz.member.customer.domain.dto.product;

import com.xcz.member.customer.domain.enums.ProductStatus;
import com.xcz.member.customer.infrastructure.cache.dto.ProductBriefCacheDTO;
import com.xcz.member.customer.infrastructure.entity.MobiShopProduct;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品简要读模型。
 */
public record ProductBriefDTO(
        Long productId,
        Long shopId,
        Long categoryId,
        String productName,
        String description,
        String imageUrl,
        BigDecimal price,
        Integer status,
        Long stock,
        Long soldCount,
        LocalDateTime createTime
) {

    public static ProductBriefDTO toDto(ProductBriefCacheDTO cacheDTO) {
        if (cacheDTO == null) {
            return null;
        }
        return new ProductBriefDTO(
                cacheDTO.getProductId(),
                cacheDTO.getShopId(),
                cacheDTO.getCategoryId(),
                cacheDTO.getProductName(),
                cacheDTO.getDescription(),
                cacheDTO.getImageUrl(),
                cacheDTO.getPrice(),
                cacheDTO.getStatus(),
                cacheDTO.getStock(),
                cacheDTO.getSoldCount(),
                cacheDTO.getCreateTime()
        );
    }


    /**
     * 将商品持久化实体转换为简要读模型。
     *
     * @param entity 商品实体
     * @return 简要读模型；实体为 null 时返回 null
     */
    public static ProductBriefDTO of(MobiShopProduct entity) {
        if (entity == null) {
            return null;
        }
        return new ProductBriefDTO(
                entity.getProductId(),
                entity.getShopId(),
                entity.getCategoryId(),
                entity.getProductName(),
                entity.getDescription(),
                entity.getImageUrl(),
                entity.getPrice(),
                entity.getStatus() == null ? ProductStatus.ON_SALE.getCode() : entity.getStatus(),
                entity.getStock() == null ? 0L : entity.getStock(),
                entity.getSoldCount() == null ? 0L : entity.getSoldCount(),
                entity.getCreateTime()
        );
    }
}
