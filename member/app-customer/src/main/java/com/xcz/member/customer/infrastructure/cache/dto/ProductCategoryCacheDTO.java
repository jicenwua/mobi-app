package com.xcz.member.customer.infrastructure.cache.dto;

import com.xcz.member.customer.domain.dto.product.ProductCategoryBriefDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 商品分类 Redis 缓存模型（含下属商品列表）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryCacheDTO implements Serializable {

    private Long categoryId;
    private Long shopId;
    private String categoryName;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private List<ProductBriefCacheDTO> products;

    public static ProductCategoryCacheDTO from(ProductCategoryBriefDTO dto) {
        if (dto == null) {
            return null;
        }
        return ProductCategoryCacheDTO.builder()
                .categoryId(dto.categoryId())
                .shopId(dto.shopId())
                .categoryName(dto.categoryName())
                .sortOrder(dto.sortOrder())
                .createTime(dto.createTime())
                .products(ProductBriefCacheDTO.fromList(dto.products()))
                .build();
    }



    public static List<ProductCategoryCacheDTO> fromList(Collection<ProductCategoryBriefDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        return dtos.stream().map(ProductCategoryCacheDTO::from).toList();
    }


}
