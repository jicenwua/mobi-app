package com.xcz.member.customer.api.dto.response.point;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 线下充值积分 API 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsRechargeRes {

    private Long logId;
    private BigDecimal basePoints;
    private BigDecimal giftPoints;
    private BigDecimal totalPoints;
    private BigDecimal remainingPoints;
}
