package com.xcz.member.customer.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 会员自助购买结果读模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsPurchaseResultVO {

    private Long logId;
    /***商品原价折算积分**/
    private BigDecimal originalPoints;
    /***优惠券抵扣积分**/
    private BigDecimal discountPoints;
    /***实际消耗积分**/
    private BigDecimal consumePoints;
    private Integer remainingPoints;
}
