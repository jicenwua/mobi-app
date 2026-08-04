package com.xcz.member.customer.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 工单消息发送方类型
 */
@Getter
@RequiredArgsConstructor
public enum TicketSenderType {
    USER(1, "用户"),
    STAFF(2, "客服");

    private final int code;
    private final String label;
}
