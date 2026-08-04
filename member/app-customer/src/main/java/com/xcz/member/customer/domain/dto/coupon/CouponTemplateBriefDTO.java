package com.xcz.member.customer.domain.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板简要读模型（应用层内部使用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponTemplateBriefDTO {

    /***模板 ID**/
    private Long templateId;
    /***店铺 ID**/
    private Long shopId;
    /***优惠券名称**/
    private String couponName;
    /***类型：1-折扣 2-满减**/
    private Integer type;
    /***使用门槛金额**/
    private BigDecimal thresholdAmount;
    /***折扣力度或减免金额**/
    private BigDecimal discountValue;
    /***领取后有效天数；为空表示永不过期**/
    private Integer validDays;
    /***发放总量**/
    private Long totalQuantity;
    /***已发放数量**/
    private Long issuedQuantity;
    /***剩余可领数量**/
    private Long remainingQuantity;
    /***发放开始时间**/
    private LocalDateTime distributionStartTime;
    /***发放结束时间**/
    private LocalDateTime distributionEndTime;
    /***发放控制状态：0-禁用 1-未开始 2-发放中 3-已结束 4-手动停止**/
    private Integer distributionStatus;
    /***创建时间**/
    private LocalDateTime createTime;
}
