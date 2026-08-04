package com.xcz.member.customer.domain.enums;

import lombok.Getter;

/**
 * 积分流水状态（对应 mobi_points_log.status）。
 */
@Getter
public enum PointsLogStatus {

    /** 消费成功（店员扫码扣款或会员自助购买完成） */
    VERIFIED(1, "已核销"),
    /** 订单取消 */
    CANCELLED(2, "订单取消"),
    /** 订单过期 */
    EXPIRED(3, "订单过期"),
    /** 积分充值 */
    RECHARGE(4, "积分充值"),
    /** 待使用（会员自助下单，积分已扣，待店员核销） */
    PENDING_VERIFY(5, "待使用"),
    /** 管理后台手动增加积分 */
    ADMIN_ADD(6, "后台添加"),
    /** 管理后台手动扣减积分 */
    ADMIN_DEDUCT(7, "后台扣除");

    private final int code;
    private final String label;

    PointsLogStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
