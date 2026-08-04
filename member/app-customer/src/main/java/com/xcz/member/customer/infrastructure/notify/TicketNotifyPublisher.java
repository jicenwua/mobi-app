package com.xcz.member.customer.infrastructure.notify;

import com.alibaba.fastjson2.JSON;
import com.xcz.commons.core.constant.NotifyConstants;
import com.xcz.commons.core.event.TicketMessageNotifyEvent;
import com.xcz.member.customer.api.dto.response.ticket.TicketMessageRes;
import com.xcz.member.customer.application.assemblers.TicketAssembler;
import com.xcz.member.customer.domain.enums.TicketSenderType;
import com.xcz.member.customer.domain.enums.TicketStatus;
import com.xcz.member.customer.domain.service.MobiUserService;
import com.xcz.member.customer.infrastructure.entity.MobiTicket;
import com.xcz.member.customer.infrastructure.entity.MobiTicketMessage;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import com.xcz.commons.redis.extend.DatabaseEnum;
import com.xcz.commons.redis.utils.RedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 工单消息 Redis 广播（供 app-notify WebSocket 推送）
 */
@Slf4j
@Service
public class TicketNotifyPublisher {

    private static final RedissonClient REDISSON = RedisUtil.getRedisson(DatabaseEnum.DATABASE_0);

    @Resource
    private MobiUserService mobiUserService;

    public void publishNewMessage(MobiTicket ticket, MobiTicketMessage message) {
        if (ticket == null || message == null) {
            return;
        }
        try {
            MobiUser user = mobiUserService.getById(ticket.getUserId());
            TicketMessageRes messageRes = TicketAssembler.toMessageRes(message, user);
            TicketMessageNotifyEvent.TicketMessageNotifyEventBuilder builder = TicketMessageNotifyEvent.builder()
                    .type(TicketMessageNotifyEvent.TYPE_NEW_MESSAGE)
                    .ticketId(ticket.getTicketId())
                    .messageId(message.getMessageId())
                    .senderType(message.getSenderType())
                    .senderId(message.getSenderId())
                    .senderLabel(messageRes.getSenderLabel())
                    .senderAvatar(messageRes.getSenderAvatar())
                    .content(message.getContent())
                    .createTime(message.getCreateTime())
                    .ticketUserId(ticket.getUserId())
                    .assignedStaffId(ticket.getAssignedStaffId());

            if (message.getSenderType() != null
                    && message.getSenderType() == TicketSenderType.STAFF.getCode()) {
                builder.targetCustomerUserId(ticket.getUserId());
            } else if (ticket.getAssignedStaffId() != null) {
                builder.targetStaffId(ticket.getAssignedStaffId());
            } else {
                builder.notifyAllStaff(true);
            }

            publish(builder.build());
        } catch (Exception e) {
            log.error("广播工单消息失败, ticketId={}", ticket.getTicketId(), e);
        }
    }

    /**
     * 未读状态变更广播（已读、未读计数变化时）
     */
    public void publishUnreadChanged(MobiTicket ticket) {
        if (ticket == null) {
            return;
        }
        try {
            TicketMessageNotifyEvent.TicketMessageNotifyEventBuilder builder = TicketMessageNotifyEvent.builder()
                    .type(TicketMessageNotifyEvent.TYPE_UNREAD_CHANGED)
                    .ticketId(ticket.getTicketId())
                    .ticketUserId(ticket.getUserId())
                    .assignedStaffId(ticket.getAssignedStaffId())
                    .targetCustomerUserId(ticket.getUserId());
            if (ticket.getAssignedStaffId() != null) {
                builder.targetStaffId(ticket.getAssignedStaffId());
            } else {
                builder.notifyAllStaff(true);
            }
            publish(builder.build());
        } catch (Exception e) {
            log.error("广播未读变更失败, ticketId={}", ticket.getTicketId(), e);
        }
    }

    /**
     * 工单状态变更广播（标记完成等）
     */
    public void publishStatusChanged(MobiTicket ticket) {
        if (ticket == null) {
            return;
        }
        try {
            TicketStatus status = TicketStatus.fromCode(ticket.getStatus());
            TicketMessageNotifyEvent.TicketMessageNotifyEventBuilder builder = TicketMessageNotifyEvent.builder()
                    .type(TicketMessageNotifyEvent.TYPE_STATUS_CHANGED)
                    .ticketId(ticket.getTicketId())
                    .status(ticket.getStatus())
                    .statusLabel(status != null ? status.getLabel() : null)
                    .ticketUserId(ticket.getUserId())
                    .assignedStaffId(ticket.getAssignedStaffId())
                    .targetCustomerUserId(ticket.getUserId());
            if (ticket.getAssignedStaffId() != null) {
                builder.targetStaffId(ticket.getAssignedStaffId());
            } else {
                builder.notifyAllStaff(true);
            }
            publish(builder.build());
        } catch (Exception e) {
            log.error("广播工单状态变更失败, ticketId={}", ticket.getTicketId(), e);
        }
    }

    private void publish(TicketMessageNotifyEvent event) {
        REDISSON.getTopic(NotifyConstants.TICKET_MESSAGE_TOPIC)
                .publish(JSON.toJSONString(event));
    }
}
