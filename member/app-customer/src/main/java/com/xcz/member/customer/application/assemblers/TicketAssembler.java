package com.xcz.member.customer.application.assemblers;

import com.xcz.member.customer.api.dto.response.ticket.TicketMessageRes;
import com.xcz.member.customer.api.dto.response.ticket.TicketRes;
import com.xcz.member.customer.domain.enums.Constants;
import com.xcz.member.customer.domain.enums.TicketSenderType;
import com.xcz.member.customer.domain.enums.TicketStatus;
import com.xcz.member.customer.infrastructure.entity.MobiTicket;
import com.xcz.member.customer.infrastructure.entity.MobiTicketMessage;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import com.xcz.member.feign.dto.ticket.MobiTicketFeign;

public final class TicketAssembler {

    private TicketAssembler() {
    }

    public static TicketRes toRes(MobiTicket ticket, MobiUser user) {
        TicketStatus status = TicketStatus.fromCode(ticket.getStatus());
        return TicketRes.builder()
                .ticketId(ticket.getTicketId())
                .userId(ticket.getUserId())
                .nickname(user != null ? user.getNickname() : null)
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .statusLabel(status != null ? status.getLabel() : null)
                .assignedStaffId(ticket.getAssignedStaffId())
                .createTime(ticket.getCreateTime())
                .updateTime(ticket.getUpdateTime())
                .build();
    }

    public static TicketMessageRes toMessageRes(MobiTicketMessage message, MobiUser ticketUser) {
        TicketSenderType senderType = message.getSenderType() != null && message.getSenderType() == TicketSenderType.STAFF.getCode()
                ? TicketSenderType.STAFF : TicketSenderType.USER;
        String senderLabel;
        String senderAvatar;
        if (senderType == TicketSenderType.STAFF) {
            senderLabel = "客服_" + message.getSenderId();
            senderAvatar = Constants.TICKET_STAFF_AVATAR_URL;
        } else {
            senderLabel = ticketUser != null && ticketUser.getNickname() != null && !ticketUser.getNickname().isBlank()
                    ? ticketUser.getNickname()
                    : "用户";
            senderAvatar = ticketUser != null ? ticketUser.getAvatarUrl() : null;
        }
        return TicketMessageRes.builder()
                .messageId(message.getMessageId())
                .senderType(message.getSenderType())
                .senderLabel(senderLabel)
                .senderId(message.getSenderId())
                .senderAvatar(senderAvatar)
                .content(message.getContent())
                .isRead(message.getIsRead() != null && message.getIsRead() == 1)
                .createTime(message.getCreateTime())
                .build();
    }

    public static MobiTicketFeign toFeign(TicketRes res) {
        return MobiTicketFeign.builder()
                .ticketId(res.getTicketId())
                .userId(res.getUserId())
                .nickname(res.getNickname())
                .title(res.getTitle())
                .description(res.getDescription())
                .status(res.getStatus())
                .statusLabel(res.getStatusLabel())
                .assignedStaffId(res.getAssignedStaffId())
                .createTime(res.getCreateTime())
                .updateTime(res.getUpdateTime())
                .unreadCount(res.getUnreadCount())
                .build();
    }
}
