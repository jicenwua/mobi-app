package com.xcz.member.customer.api.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品详情 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRes {

    /***商品ID**/
    private Long productId;

    /***店铺ID**/
    private Long shopId;

    /***店铺名称**/
    private String shopName;

    /***分类ID**/
    private Long categoryId;

    /***商品名称**/
    private String productName;

    /***商品描述**/
    private String description;

    /***商品图片地址**/
    private String imageUrl;

    /***商品单价**/
    private BigDecimal price;

    /***售卖状态：0-售完 1-出售中**/
    private Integer status;

    /***库存数量**/
    private Long stock;

    /***累计销量**/
    private Long soldCount;

    /***创建时间**/
    private LocalDateTime createTime;

}
