package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单消息 mobi_ticket_message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("mobi_ticket_message")
public class MobiTicketMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long messageId;
    private Long ticketId;
    /** 1-用户 2-客服 */
    private Integer senderType;
    private Long senderId;
    private String content;
    /** 接收方是否已读：0-未读 1-已读 */
    private Integer isRead;
    private LocalDateTime createTime;
}
