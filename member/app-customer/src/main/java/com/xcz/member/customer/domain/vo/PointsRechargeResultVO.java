package com.xcz.member.customer.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 线下充值结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsRechargeResultVO {

    private Long logId;
    private BigDecimal basePoints;
    private BigDecimal giftPoints;
    private BigDecimal totalPoints;
    private BigDecimal remainingPoints;
}
