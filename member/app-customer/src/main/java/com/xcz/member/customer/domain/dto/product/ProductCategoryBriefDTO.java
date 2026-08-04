package com.xcz.member.customer.domain.dto.product;

import com.xcz.member.customer.infrastructure.cache.dto.ProductBriefCacheDTO;
import com.xcz.member.customer.infrastructure.cache.dto.ProductCategoryCacheDTO;
import com.xcz.member.customer.infrastructure.entity.MobiShopProduct;
import com.xcz.member.customer.infrastructure.entity.MobiShopProductCategory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品分类读模型（含分类下商品列表）。
 */
public record ProductCategoryBriefDTO(
        Long categoryId,
        Long shopId,
        String categoryName,
        Integer sortOrder,
        LocalDateTime createTime,
        List<ProductBriefDTO> products
) {
    /**
     * 将分类实体与下属商品列表组装为分类简要读模型。
     *
     * @param category 分类实体
     * @param products 下属商品列表
     * @return 分类简要读模型；分类为 null 时返回 null
     */
    public static ProductCategoryBriefDTO of(MobiShopProductCategory category, List<ProductBriefDTO> products) {
        if (category == null) {
            return null;
        }
        return new ProductCategoryBriefDTO(
                category.getCategoryId(),
                category.getShopId(),
                category.getCategoryName(),
                category.getSortOrder() == null ? 0 : category.getSortOrder(),
                category.getCreateTime(),
                products == null ? List.of() : products
        );
    }

    public static ProductCategoryBriefDTO toDto(ProductCategoryCacheDTO  cache) {
        return new ProductCategoryBriefDTO(
               cache.getCategoryId(),
               cache.getShopId(),
               cache.getCategoryName(),
               cache.getSortOrder(),
               cache.getCreateTime(),
                ProductBriefCacheDTO.toDtoList(cache.getProducts())
        );
    }

    public static List<ProductCategoryBriefDTO> toDtoList(Collection<ProductCategoryCacheDTO> caches) {
        if (caches == null || caches.isEmpty()) {
            return List.of();
        }
        return caches.stream().map(ProductCategoryBriefDTO::toDto).toList();
    }

    /**
     * 将商品按分类分组；未分类商品归入虚拟「默认分类」。
     *
     * @param categories 店铺分类列表
     * @param products   店铺商品列表
     * @return 按分类分组的读模型列表
     */
    public static List<ProductCategoryBriefDTO> ofList(
            List<MobiShopProductCategory> categories,
            List<MobiShopProduct> products) {
        //按照商品的创建时间排序
        List<ProductBriefDTO> productBriefs = products.stream()
                .map(ProductBriefDTO::of)
                .sorted(Comparator.comparing(ProductBriefDTO::createTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        //按照商品的分类分组
        Map<Long, List<ProductBriefDTO>> grouped = productBriefs.stream()
                .collect(Collectors.groupingBy(
                        p -> p.categoryId() == null ? -1L : p.categoryId(),
                        Collectors.toList()
                ));
        //将分组后的商品，根据分类 ID 保存信息
        List<ProductCategoryBriefDTO> result = new ArrayList<>();
        for (MobiShopProductCategory category : categories) {
            List<ProductBriefDTO> items = grouped.getOrDefault(category.getCategoryId(), List.of());
            result.add(of(category, items));
        }
        //是否有默认分类
        List<ProductBriefDTO> uncategorized = grouped.getOrDefault(-1L, List.of());
        if (!uncategorized.isEmpty()) {
            MobiShopProductCategory defaultCategory = MobiShopProductCategory.builder()
                    .categoryId(0L)
                    .shopId(uncategorized.getFirst().shopId())
                    .categoryName("默认分类")
                    .sortOrder(Integer.MAX_VALUE)
                    .build();
            result.add(of(defaultCategory, uncategorized));
        }
        //按照分类的排序、分类 ID 排序
        result.sort(Comparator
                .comparing((ProductCategoryBriefDTO c) -> c.sortOrder() == null ? 0 : c.sortOrder())
                .thenComparing(c -> c.categoryId() == null ? Long.MAX_VALUE : c.categoryId()));
        return result;
    }
}
