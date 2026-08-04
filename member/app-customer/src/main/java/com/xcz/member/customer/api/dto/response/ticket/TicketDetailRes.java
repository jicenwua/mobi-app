package com.xcz.member.customer.api.dto.response.ticket;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TicketDetailRes {
    private Long ticketId;
    private Long userId;
    private String nickname;
    private String title;
    private String description;
    private Integer status;
    private String statusLabel;
    private Long assignedStaffId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer unreadCount;
    private List<TicketMessageRes> messages;
}
