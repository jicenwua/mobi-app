package com.xcz.member.customer.api.dto.request.product;

import com.xcz.member.customer.api.dto.request.BaseReq;
import lombok.*;

import java.math.BigDecimal;

/**
 * 商品查询 / 维护参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductReq extends BaseReq {

    /***商品ID**/
    private Long productId;
    /***店铺ID**/
    private Long shopId;
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
    /***multipart images 数组下标；未传且 images 与 meta 等长时按顺序匹配**/
    private Integer imageIndex;
}
