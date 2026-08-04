package com.xcz.member.customer.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户持券状态（对应 mobi_user_coupons.status）。
 */
@Getter
@RequiredArgsConstructor
public enum UserCouponStatus {

    /** 未使用 */
    UNUSED(0, "未使用"),
    /** 已使用 */
    USED(1, "已使用"),
    /** 已过期 */
    EXPIRED(2, "已过期");

    /** 持久化编码 */
    private final int code;
    /** 展示名称 */
    private final String label;

    /**
     * 按编码解析用户持券状态。
     *
     * @param code 状态编码
     * @return 对应枚举；无法识别时返回 null
     */
    public static UserCouponStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserCouponStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
