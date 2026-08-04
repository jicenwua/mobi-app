package com.xcz.member.customer.api.dto.request.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品分类维护参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryReq {

    /***分类ID**/
    private Long categoryId;
    /***店铺ID**/
    private Long shopId;
    /***分类名称**/
    private String categoryName;
    /***排序序号**/
    private Integer sortOrder;
}
