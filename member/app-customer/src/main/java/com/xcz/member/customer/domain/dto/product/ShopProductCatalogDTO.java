package com.xcz.member.customer.domain.dto.product;

import java.util.List;

/**
 * 店铺商品目录读模型：按分类分组，供 C 端菜单页一次性加载。
 */
public record ShopProductCatalogDTO(
        Long shopId,
        List<ProductCategoryBriefDTO> categories
) {
}
