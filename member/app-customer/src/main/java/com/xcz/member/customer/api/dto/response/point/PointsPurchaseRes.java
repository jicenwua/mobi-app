package com.xcz.member.customer.api.dto.response.point;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 会员自助购买结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsPurchaseRes {

    /***订单流水ID**/
    private Long logId;
    /***商品原价折算积分**/
    private Integer originalPoints;
    /***优惠券抵扣积分**/
    private BigDecimal discountPoints;
    /***本次消耗积分**/
    private BigDecimal consumePoints;
    /***交易后剩余积分**/
    private Integer remainingPoints;
}
