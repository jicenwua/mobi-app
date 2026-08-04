package com.xcz.member.customer.domain.order.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购买结算结果：商品原价、优惠抵扣与应付积分。
 */
@Builder
public record PurchaseSettlement(
        List<PurchaseLineItem> items,
        BigDecimal originalPoints,
        BigDecimal discountPoints,
        BigDecimal payablePoints,
        Long userCouponId,
        String couponName,
        Integer couponType
) {
}
