package com.xcz.member.customer.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 活动种类枚举。
 */
@Getter
@RequiredArgsConstructor
public enum ActivityKind {

    /** 满赠活动（可配置满赠规则） */
    PROMOTION(1, "满赠活动"),
    /** 纯公告（仅展示描述，不可配置规则） */
    ANNOUNCEMENT(2, "公告");

    /** 持久化编码 */
    private final Integer code;
    /** 展示名称 */
    private final String label;

    /**
     * 按编码解析活动种类。
     *
     * @param code 种类编码
     * @return 对应枚举；无法识别时返回 null
     */
    public static ActivityKind getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ActivityKind kind : values()) {
            if (kind.code == code) {
                return kind;
            }
        }
        return null;
    }
}
