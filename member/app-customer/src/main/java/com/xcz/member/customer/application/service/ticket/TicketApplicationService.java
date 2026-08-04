package com.xcz.member.customer.application.service.ticket;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.domain.enums.TicketSenderType;
import com.xcz.member.customer.domain.enums.TicketStatus;
import com.xcz.member.customer.domain.service.MobiTicketMessageService;
import com.xcz.member.customer.domain.service.MobiTicketService;
import com.xcz.member.customer.infrastructure.entity.MobiTicket;
import com.xcz.member.customer.infrastructure.entity.MobiTicketMessage;
import com.xcz.member.customer.infrastructure.notify.TicketNotifyPublisher;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class TicketApplicationService {

    private static final int MAX_TITLE_LEN = 100;
    private static final int MAX_CONTENT_LEN = 2000;

    @Resource
    private MobiTicketService mobiTicketService;
    @Resource
    private MobiTicketMessageService mobiTicketMessageService;
    @Resource
    private TicketNotifyPublisher ticketNotifyPublisher;

    @Transactional(rollbackFor = Exception.class)
    public Long create(String title, String description, Long userId) {
        String normalizedTitle = normalizeRequired(title, "工单标题", MAX_TITLE_LEN);
        String normalizedDesc = normalizeRequired(description, "问题描述", MAX_CONTENT_LEN);
        LocalDateTime now = LocalDateTime.now();
        MobiTicket ticket = MobiTicket.builder()
                .userId(userId)
                .title(normalizedTitle)
                .description(normalizedDesc)
                .status(TicketStatus.PENDING.getCode())
                .createTime(now)
                .updateTime(now)
                .build();
        mobiTicketService.save(ticket);
        MobiTicketMessage message = saveMessage(ticket.getTicketId(), TicketSenderType.USER, userId, normalizedDesc, now, false);
        ticketNotifyPublisher.publishNewMessage(ticket, message);
        return ticket.getTicketId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long userReply(Long ticketId, String content, Long userId) {
        MobiTicket ticket = requireTicket(ticketId);
        if (!Objects.equals(ticket.getUserId(), userId)) {
            throw new ServiceException("无权操作该工单", 403);
        }
        if (Objects.equals(ticket.getStatus(), TicketStatus.COMPLETED.getCode())) {
            throw new ServiceException("工单已完成，无法继续回复", 400);
        }
        String normalized = normalizeRequired(content, "消息内容", MAX_CONTENT_LEN);
        LocalDateTime now = LocalDateTime.now();
        MobiTicketMessage message = saveMessage(ticketId, TicketSenderType.USER, userId, normalized, now, false);
        touchTicket(ticket, now);
        ticketNotifyPublisher.publishNewMessage(ticket, message);
        return message.getMessageId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void claim(Long ticketId, Long staffId) {
        MobiTicket ticket = requireTicket(ticketId);
        if (Objects.equals(ticket.getStatus(), TicketStatus.COMPLETED.getCode())) {
            throw new ServiceException("工单已完成", 400);
        }
        if (ticket.getAssignedStaffId() != null) {
            if (Objects.equals(ticket.getAssignedStaffId(), staffId)) {
                return;
            }
            throw new ServiceException("该工单已被其他客服处理", 409);
        }
        if (!Objects.equals(ticket.getStatus(), TicketStatus.PENDING.getCode())) {
            throw new ServiceException("工单状态不可认领", 400);
        }
        boolean claimed = mobiTicketService.claim(ticketId, staffId);
        if (!claimed) {
            throw new ServiceException("该工单已被其他客服处理", 409);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Long staffReply(Long ticketId, String content, Long staffId) {
        MobiTicket ticket = ensureStaffCanOperate(ticketId, staffId);
        if (Objects.equals(ticket.getStatus(), TicketStatus.COMPLETED.getCode())) {
            throw new ServiceException("工单已完成，无法继续回复", 400);
        }
        String normalized = normalizeRequired(content, "回复内容", MAX_CONTENT_LEN);
        LocalDateTime now = LocalDateTime.now();
        MobiTicketMessage message = saveMessage(ticketId, TicketSenderType.STAFF, staffId, normalized, now, false);
        touchTicket(ticket, now);
        ticketNotifyPublisher.publishNewMessage(ticket, message);
        return message.getMessageId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(Long ticketId, Long staffId) {
        MobiTicket ticket = ensureStaffCanOperate(ticketId, staffId);
        if (Objects.equals(ticket.getStatus(), TicketStatus.COMPLETED.getCode())) {
            return;
        }
        ticket.setStatus(TicketStatus.COMPLETED.getCode());
        ticket.setUpdateTime(LocalDateTime.now());
        mobiTicketService.updateById(ticket);
        ticketNotifyPublisher.publishStatusChanged(ticket);
    }

    private MobiTicket ensureStaffCanOperate(Long ticketId, Long staffId) {
        MobiTicket ticket = requireTicket(ticketId);
        if (Objects.equals(ticket.getStatus(), TicketStatus.COMPLETED.getCode())) {
            return ticket;
        }
        if (ticket.getAssignedStaffId() != null) {
            if (!Objects.equals(ticket.getAssignedStaffId(), staffId)) {
                throw new ServiceException("该工单由其他客服处理中", 403);
            }
            return ticket;
        }
        if (!Objects.equals(ticket.getStatus(), TicketStatus.PENDING.getCode())) {
            throw new ServiceException("请先认领工单", 403);
        }
        if (!mobiTicketService.claim(ticketId, staffId)) {
            ticket = requireTicket(ticketId);
            if (!Objects.equals(ticket.getAssignedStaffId(), staffId)) {
                throw new ServiceException("该工单由其他客服处理中", 403);
            }
            return ticket;
        }
        ticket.setAssignedStaffId(staffId);
        ticket.setStatus(TicketStatus.PROCESSING.getCode());
        return ticket;
    }

    private MobiTicket requireTicket(Long ticketId) {
        MobiTicket ticket = mobiTicketService.getById(ticketId);
        if (ticket == null) {
            throw new ServiceException("工单不存在", 404);
        }
        return ticket;
    }

    public void markReadForUser(Long ticketId) {
        mobiTicketMessageService.markReadBySenderType(ticketId, TicketSenderType.STAFF.getCode());
        publishUnreadChanged(ticketId);
    }

    public void markReadForStaff(Long ticketId) {
        mobiTicketMessageService.markReadBySenderType(ticketId, TicketSenderType.USER.getCode());
        publishUnreadChanged(ticketId);
    }

    private void publishUnreadChanged(Long ticketId) {
        MobiTicket ticket = mobiTicketService.getById(ticketId);
        if (ticket != null) {
            ticketNotifyPublisher.publishUnreadChanged(ticket);
        }
    }

    private MobiTicketMessage saveMessage(Long ticketId, TicketSenderType senderType, Long senderId,
                                          String content, LocalDateTime createTime, boolean read) {
        MobiTicketMessage message = MobiTicketMessage.builder()
                .ticketId(ticketId)
                .senderType(senderType.getCode())
                .senderId(senderId)
                .content(content)
                .isRead(read ? 1 : 0)
                .createTime(createTime)
                .build();
        mobiTicketMessageService.save(message);
        return message;
    }

    private void touchTicket(MobiTicket ticket, LocalDateTime now) {
        ticket.setUpdateTime(now);
        mobiTicketService.updateById(ticket);
    }

    private static String normalizeRequired(String value, String fieldName, int maxLen) {
        if (!StringUtils.hasText(value)) {
            throw new ServiceException(fieldName + "不能为空", 400);
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLen) {
            throw new ServiceException(fieldName + "不能超过" + maxLen + "字", 400);
        }
        return trimmed;
    }
}
