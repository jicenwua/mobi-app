package com.xcz.member.customer.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台商品列表行（联表店铺名称，避免 N+1）。
 */
@Data
public class AdminProductRowVO {

    private Long productId;
    private Long shopId;
    private String shopName;
    private Long categoryId;
    private String productName;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private Integer status;
    private Long stock;
    private Long soldCount;
    private LocalDateTime createTime;
}
