package com.xcz.member.customer.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 满赠活动子类型（仅 kind=PROMOTION 时有效）。
 */
@Getter
@RequiredArgsConstructor
public enum ActivityType {

    /** 充值满赠 */
    RECHARGE_GIFT(1, "充值满赠"),
    /** 消费满赠 */
    CONSUME_GIFT(2, "消费满赠");

    /** 持久化编码 */
    private final int code;
    /** 展示名称 */
    private final String label;

    /**
     * 按编码解析满赠子类型。
     *
     * @param code 类型编码
     * @return 对应枚举；无法识别时返回 null
     */
    public static ActivityType getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ActivityType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
