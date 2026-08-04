package com.xcz.member.customer.domain.enums;

import lombok.Getter;

/**
 * 积分流水动作类型（对应 mobi_points_log.action_type）。
 */
@Getter
public enum PointsActionType {

    /** 积分增加（充值、活动等） */
    INCREASE(1, "增加"),
    /** 积分消耗（扫码扣款、自助购买） */
    CONSUME(2, "消耗");

    private final int code;
    private final String label;

    PointsActionType(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
