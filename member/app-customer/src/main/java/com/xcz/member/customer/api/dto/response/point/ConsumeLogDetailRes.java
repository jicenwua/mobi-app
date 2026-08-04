package com.xcz.member.customer.api.dto.response.point;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 消费记录详情（含商品明细与优惠券抵扣信息）。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConsumeLogDetailRes extends ConsumeLogRes {

    /***商品原价折算积分合计**/
    private Integer originalPoints;
    /***优惠券抵扣积分**/
    private Integer couponDiscountPoints;
    /***优惠券名称**/
    private String couponName;
    /***优惠券类型：1-折扣 2-满减**/
    private Integer couponType;
}
