package com.xcz.member.customer.api.dto.response.ticket;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TicketRes {
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
    /** 当前视角下的未读消息数 */
    private Integer unreadCount;
}
