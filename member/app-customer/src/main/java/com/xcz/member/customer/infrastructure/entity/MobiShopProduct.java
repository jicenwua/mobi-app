package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 店铺商品信息表 mobi_shop_product
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("mobi_shop_product")
public class MobiShopProduct implements Serializable {
    private static final long serialVersionUID = 1L;

    /***商品唯一流水ID(主键)**/
    @TableId(type = IdType.AUTO)
    private Long productId;
    /***所属店铺ID**/
    private Long shopId;
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
    /***剩余库存（当前可售数量）**/
    private Long stock;
    /***累计已售数量（自发布以来的总销量；实时增量在 Redis）**/
    private Long soldCount;
    /***商品创建时间**/
    private LocalDateTime createTime;
}
