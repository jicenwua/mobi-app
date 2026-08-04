package com.xcz.member.customer.api.dto.response.shop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 店铺统计数据响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopStatisticsRes {

    /** 时段内新用户数（未传日期时默认今日） */
    private Long todayNewUsers;
    /** 时段内订单数（未传日期时默认今日） */
    private Long todayOrders;
    /** 时段内使用积分（未传日期时默认今日） */
    private BigDecimal totalPointsUsed;
    /** 时段内充值金额（元，未传日期时默认今日） */
    private BigDecimal todayRechargeAmount;
    /** 所选时段内按日走势 */
    private List<TrendPoint> trend;
    /** 当前上下文店铺是否为总店 */
    private Boolean headShop;
    /** 总店下的分店列表（仅总店返回） */
    private List<BranchShop> branches;
    /** 当前数据范围说明：all / self / shop */
    private String scope;
    /** 当前筛选的店铺 ID（无筛选时为 null） */
    private Long filterShopId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
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
    public static class BranchShop {
        private Long id;
        private String shopName;
    }
}
