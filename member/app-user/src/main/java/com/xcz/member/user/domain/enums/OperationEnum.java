package com.xcz.member.user.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OperationEnum {
    LOGIN(1, "登录"),
    UPDATE(2, "更新"),
    DELETE(3, "删除"),
    ADD(4, "添加"),
    OTHER(100, "其他");

    /***操作编码**/
    private final Integer code;
    /***操作描述**/
    private final String message;

    public static OperationEnum getByCode(Integer code) {
        for (OperationEnum value : OperationEnum.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return OTHER;
    }
}
