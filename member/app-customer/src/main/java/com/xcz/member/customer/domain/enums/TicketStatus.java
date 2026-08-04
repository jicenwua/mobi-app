package com.xcz.member.customer.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 工单状态
 */
@Getter
@RequiredArgsConstructor
public enum TicketStatus {
    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    COMPLETED(2, "已完成");

    private final int code;
    private final String label;

    public static TicketStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TicketStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}
