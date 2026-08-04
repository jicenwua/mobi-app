package com.xcz.member.feign.dto.shop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 店铺统计数据 Feign DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopStatisticsFeign implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long todayNewUsers;
    private Long todayOrders;
    private BigDecimal totalPointsUsed;
    private BigDecimal todayRechargeAmount;
    private List<TrendPoint> trend;
    private Boolean headShop;
    private List<BranchShop> branches;
    private String scope;
    private Long filterShopId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint implements Serializable {
        private static final long serialVersionUID = 1L;
        private String date;
        private Long newUsers;
        private Long orders;
        private BigDecimal pointsUsed;
        private BigDecimal rechargeAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchShop implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String shopName;
    }
}
