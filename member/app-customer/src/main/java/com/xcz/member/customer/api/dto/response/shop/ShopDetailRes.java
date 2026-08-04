package com.xcz.member.customer.api.dto.response.shop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 店铺详情 VO，用于列表、进店页与扫码预览等场景。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopDetailRes {
    /***店铺ID**/
    private Long shopId;
    /***父店铺ID**/
    private Long parentId;
    /***店铺名称**/
    private String shopName;
    /***店铺唯一标识**/
    private String shopCode;
    /***积分兑换比率（1元*比率 = 充值一元的积分）不可更改**/
    private Integer ratio;
    /***店铺图片**/
    private List<String> picture;
    /***联系电话**/
    private String phone;
    /***详细地址**/
    private String address;


    /***当前用户在店铺中的角色编码（见 ShopUserRole）**/
    private Integer role;
    /***用户剩余积分**/
    private BigDecimal amount;
    /***已消费总积分**/
    private BigDecimal totalAmount;
    /***商品列表**/
    private List<Product> products;

    /**
     * 将逗号分隔的图片字符串转为列表
     *
     * @param picture 逗号分隔的图片地址
     */
    public void setPicture(String picture) {
        this.picture = Arrays.stream(picture.split(",")).collect(Collectors.toList());
    }

    /**
     * 店铺详情中的商品摘要
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        /***商品ID**/
        private Long productId;
        /***商品分类ID（可为空，归入默认分类）**/
        private Long categoryId;
        /***商品名称**/
        private String productName;
        /***商品描述**/
        private String description;
        /***商品图片**/
        private String imageUrl;
        /***商品价格**/
        private BigDecimal price;
        /***售卖状态：0-售完 1-出售中 **/
        private Integer status;
        /***累计已售数量（自发布以来的总销量；实时增量在 Redis）**/
        private Integer soldCount;
    }
}
