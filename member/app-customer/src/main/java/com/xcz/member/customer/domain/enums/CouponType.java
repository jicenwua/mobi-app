package com.xcz.member.customer.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 优惠券类型枚举。
 */
@Getter
@RequiredArgsConstructor
public enum CouponType {

    /** 折扣券（如 85 表示 85 折） */
    DISCOUNT(1, "折扣"),
    /** 满减券（固定减免金额） */
    REDUCTION(2, "满减");

    /** 持久化编码 */
    private final int code;
    /** 展示名称 */
    private final String label;

    /**
     * 按编码解析优惠券类型。
     *
     * @param code 类型编码
     * @return 对应枚举；无法识别时返回 null
     */
    public static CouponType getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
