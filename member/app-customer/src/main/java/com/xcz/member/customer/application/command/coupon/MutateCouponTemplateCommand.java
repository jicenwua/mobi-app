package com.xcz.member.customer.application.command.coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板写操作命令载体（创建、更新、调整发放、追加库存共用，校验由 Assembler 按场景执行）。
 *
 * @param templateId             模板 ID，创建时为 null
 * @param shopId                 店铺 ID
 * @param couponName             优惠券名称
 * @param type                   类型：1-折扣 2-满减
 * @param thresholdAmount        使用门槛
 * @param discountValue          折扣力度或减免金额
 * @param validDays              领取后有效天数
 * @param totalQuantity          发放总量
 * @param addQuantity            追加库存数量，仅追加库存场景使用
 * @param distributionStartTime  发放开始时间
 * @param distributionEndTime    发放结束时间
 */
public record MutateCouponTemplateCommand(
        Long templateId,
        Long shopId,
        String couponName,
        Integer type,
        BigDecimal thresholdAmount,
        BigDecimal discountValue,
        Integer validDays,
        Long totalQuantity,
        Long addQuantity,
        LocalDateTime distributionStartTime,
        LocalDateTime distributionEndTime
) {
}
