package com.xcz.member.user.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 日志类型枚举
 */
@Getter
@AllArgsConstructor
public enum LogTypeEnum {
    LOGIN(1, "登录日志"),
    MENU(2, "菜单日志"),
    ROLE(3, "角色日志"),
    USER(4, "用户日志");

    /***日志类型编码**/
    private final Integer code;
    /***日志类型描述**/
    private final String desc;

    public static LogTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (LogTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
