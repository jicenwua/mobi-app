package com.xcz.member.user.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 审核状态枚举 */
@Getter
@AllArgsConstructor
public enum AuditStatus {
    PENDING(0, "待审核"),
    APPROVED(1, "审核通过"),
    REJECTED(2, "审核驳回");

    /***状态编码**/
    private final int code;
    /***状态描述**/
    private final String msg;
}
