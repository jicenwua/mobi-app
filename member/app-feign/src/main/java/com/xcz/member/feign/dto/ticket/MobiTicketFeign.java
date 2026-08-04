package com.xcz.member.feign.dto.ticket;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MobiTicketFeign {
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
}
