package com.xcz.member.user.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 行业类目枚举 */
@Getter
@AllArgsConstructor
public enum IndustryCategory {
    RESTAURANT(1, "餐饮"),
    RETAIL(2, "零售"),
    SERVICE(3, "服务"),
    TECHNOLOGY(4, "科技");

    /***类目编码**/
    private final int code;
    /***类目名称**/
    private final String name;
}
