package com.xcz.member.customer.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 按店铺汇总的新增积分（用于折算充值金额）。
 */
@Data
public class ShopIncreasePointsVO {

    private Long shopId;
    private BigDecimal increasePoints;
}
