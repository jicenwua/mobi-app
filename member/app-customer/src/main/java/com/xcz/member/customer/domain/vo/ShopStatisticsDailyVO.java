package com.xcz.member.customer.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 店铺统计按日聚合读模型。
 */
@Data
public class ShopStatisticsDailyVO {

    private LocalDate statDate;
    private Long newUsers;
    private Long orders;
    private BigDecimal pointsUsed;
    private BigDecimal increasePoints;
}
