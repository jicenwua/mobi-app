package com.xcz.member.customer.infrastructure.cache.dto;

import com.xcz.member.customer.domain.dto.product.ProductBriefDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 商品简要信息 Redis 缓存模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBriefCacheDTO implements Serializable {

    private Long productId;
    private Long shopId;
    private Long categoryId;
    private String productName;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private Integer status;
    private Long stock;
    private Long soldCount;
    private LocalDateTime createTime;

    public static ProductBriefCacheDTO from(ProductBriefDTO dto) {
        if (dto == null) {
            return null;
        }
        return ProductBriefCacheDTO.builder()
                .productId(dto.productId())
                .shopId(dto.shopId())
                .categoryId(dto.categoryId())
                .productName(dto.productName())
                .description(dto.description())
                .imageUrl(dto.imageUrl())
                .price(dto.price())
                .status(dto.status())
                .stock(dto.stock())
                .soldCount(dto.soldCount())
                .createTime(dto.createTime())
                .build();
    }



    public static List<ProductBriefCacheDTO> fromList(Collection<ProductBriefDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        return dtos.stream().map(ProductBriefCacheDTO::from).toList();
    }

    public static List<ProductBriefDTO> toDtoList(Collection<ProductBriefCacheDTO> caches) {
        if (caches == null || caches.isEmpty()) {
            return List.of();
        }
        return caches.stream().map(ProductBriefDTO::toDto).toList();
    }
}
