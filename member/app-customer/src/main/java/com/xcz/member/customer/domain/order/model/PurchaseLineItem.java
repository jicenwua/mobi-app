package com.xcz.member.customer.domain.order.model;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * 购买结算单行（领域值对象）。
 */
@Builder
public record PurchaseLineItem(
        Long productId,
        String productName,
        int count,
        BigDecimal unitPrice,
        BigDecimal linePoints
) {
}
