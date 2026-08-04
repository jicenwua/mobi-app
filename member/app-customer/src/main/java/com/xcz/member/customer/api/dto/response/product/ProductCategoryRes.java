package com.xcz.member.customer.api.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品分类 VO（含下属商品列表）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryRes {

    /***分类ID**/
    private Long categoryId;

    /***店铺ID**/
    private Long shopId;

    /***分类名称**/
    private String categoryName;

    /***排序序号**/
    private Integer sortOrder;

    /***创建时间**/
    private LocalDateTime createTime;

    /***分类下商品列表**/
    private List<ProductRes> products;

}
