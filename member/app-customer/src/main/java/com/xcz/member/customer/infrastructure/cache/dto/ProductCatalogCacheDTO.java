package com.xcz.member.customer.infrastructure.cache.dto;

import com.xcz.member.customer.domain.dto.product.ProductCategoryBriefDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 店铺商品目录 Redis 缓存根对象。
 * <p>
 * 使用具体类型作为 {@link org.redisson.api.RBucket} 的值，避免 {@code List<T>} 泛型擦除导致 Jackson 多态反序列化失败。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCatalogCacheDTO implements Serializable {

    private Long shopId;

    private List<ProductCategoryCacheDTO> categories;

    public static ProductCatalogCacheDTO from(Long shopId, Collection<ProductCategoryBriefDTO> categories) {
        return ProductCatalogCacheDTO.builder()
                .shopId(shopId)
                .categories(ProductCategoryCacheDTO.fromList(categories))
                .build();
    }
}
