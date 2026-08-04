package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店铺商品分类表 mobi_shop_product_category
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("mobi_shop_product_category")
public class MobiShopProductCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long categoryId;

    private Long shopId;

    private String categoryName;

    /** 分类展示顺序，越小越靠前 */
    private Integer sortOrder;

    private LocalDateTime createTime;
}
