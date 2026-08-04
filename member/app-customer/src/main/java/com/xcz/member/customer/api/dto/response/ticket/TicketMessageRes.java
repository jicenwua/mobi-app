package com.xcz.member.customer.api.dto.response.ticket;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TicketMessageRes {
    private Long messageId;
    private Integer senderType;
    private String senderLabel;
    private Long senderId;
    /** 发送方头像 URL */
    private String senderAvatar;
    private String content;
    private LocalDateTime createTime;
    /** 接收方是否已读 */
    private Boolean isRead;
}
