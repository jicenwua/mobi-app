package com.xcz.member.feign.dto.ticket;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MobiTicketMessageFeign {
    private Long messageId;
    private Integer senderType;
    private String senderLabel;
    private Long senderId;
    /** 发送方头像 URL */
    private String senderAvatar;
    private String content;
    private Boolean isRead;
    private LocalDateTime createTime;
}
