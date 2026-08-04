package com.xcz.member.customer.api.dto.request.coupon;

import com.xcz.member.customer.api.dto.request.BaseReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板 HTTP 请求参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CouponTemplateReq extends BaseReq {

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
    /***领取后有效天数**/
    private Integer validDays;
    /***发放总量**/
    private Long totalQuantity;
    /***追加库存数量（仅追加库存接口使用）**/
    private Long addQuantity;
    /***发放开始时间**/
    private LocalDateTime distributionStartTime;
    /***发放结束时间**/
    private LocalDateTime distributionEndTime;
    /***发放状态筛选（店员列表）：0-禁用 1-未开始 2-发放中 3-已结束 4-手动停止**/
    private Integer distributionStatus;
}
