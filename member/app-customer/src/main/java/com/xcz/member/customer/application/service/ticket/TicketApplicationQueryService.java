package com.xcz.member.customer.application.service.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.api.dto.response.ticket.TicketDetailRes;
import com.xcz.member.customer.api.dto.response.ticket.TicketMessageRes;
import com.xcz.member.customer.api.dto.response.ticket.TicketRes;
import com.xcz.member.customer.application.assemblers.TicketAssembler;
import com.xcz.member.customer.domain.enums.TicketSenderType;
import com.xcz.member.customer.domain.enums.TicketStatus;
import com.xcz.member.customer.domain.service.MobiTicketMessageService;
import com.xcz.member.customer.domain.service.MobiTicketService;
import com.xcz.member.customer.domain.service.MobiUserService;
import com.xcz.member.customer.infrastructure.entity.MobiTicket;
import com.xcz.member.customer.infrastructure.entity.MobiTicketMessage;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import com.xcz.member.feign.dto.ticket.MobiTicketDetailFeign;
import com.xcz.member.feign.dto.ticket.MobiTicketMessageFeign;
import com.xcz.member.feign.dto.ticket.MobiTicketUnreadSummaryFeign;
import com.xcz.commons.security.extend.LoginUser;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TicketApplicationQueryService {

    @Resource
    private MobiTicketService mobiTicketService;
    @Resource
    private MobiTicketMessageService mobiTicketMessageService;
    @Resource
    private MobiUserService mobiUserService;
    @Resource
    private TicketApplicationService ticketApplicationService;

    public Page<TicketRes> pageMyTickets(Long userId, Integer pageNum, Integer pageSize) {
        Page<MobiTicket> page = mobiTicketService.page(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<MobiTicket>()
                        .eq(MobiTicket::getUserId, userId)
                        .orderByDesc(MobiTicket::getCreateTime));
        return toTicketResPage(page, TicketSenderType.STAFF.getCode());
    }

    public Page<TicketRes> pageAllTickets(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<MobiTicket> wrapper = new LambdaQueryWrapper<MobiTicket>()
                .eq(userId != null, MobiTicket::getUserId, userId)
                .eq(status != null, MobiTicket::getStatus, status)
                .orderByDesc(MobiTicket::getCreateTime);
        Page<MobiTicket> page = mobiTicketService.page(new Page<>(pageNum, pageSize), wrapper);
        return toTicketResPage(page, TicketSenderType.USER.getCode());
    }

    public Page<TicketRes> pageMyAssignedTickets(Long staffId, Integer status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<MobiTicket> wrapper = new LambdaQueryWrapper<MobiTicket>()
                .eq(MobiTicket::getAssignedStaffId, staffId)
                .ne(MobiTicket::getStatus, TicketStatus.COMPLETED.getCode())
                .eq(status != null, MobiTicket::getStatus, status)
                .orderByDesc(MobiTicket::getUpdateTime);
        Page<MobiTicket> page = mobiTicketService.page(new Page<>(pageNum, pageSize), wrapper);
        return toTicketResPage(page, TicketSenderType.USER.getCode());
    }

    public int countUnreadForUser(Long userId) {
        return mobiTicketMessageService.countUnreadForUser(userId, TicketSenderType.STAFF.getCode());
    }

    public MobiTicketUnreadSummaryFeign unreadSummaryForStaff(Long staffId) {
        return MobiTicketUnreadSummaryFeign.builder()
                .listUnread(mobiTicketMessageService.countListUnreadForStaff(TicketSenderType.USER.getCode()))
                .mineUnread(mobiTicketMessageService.countUnreadForStaff(staffId, TicketSenderType.USER.getCode()))
                .build();
    }

    public TicketDetailRes getDetailForUser(Long ticketId, Long userId) {
        MobiTicket ticket = requireTicket(ticketId);
        if (!Objects.equals(ticket.getUserId(), userId)) {
            throw new ServiceException("无权查看该工单", 403);
        }
        ticketApplicationService.markReadForUser(ticketId);
        return buildDetail(ticket);
    }

    public TicketDetailRes getDetailForStaff(Long ticketId, Long staffId) {
        MobiTicket ticket = requireTicket(ticketId);
        assertStaffCanAccess(ticket, staffId);
        ticketApplicationService.markReadForStaff(ticketId);
        return buildDetail(ticket);
    }

    public MobiTicketDetailFeign getDetailForFeign(Long ticketId, Long staffId) {
        MobiTicket ticket = requireTicket(ticketId);
        assertCanViewTicketDetail(ticket, staffId, SecurityUtils.getLoginUser());
        if (ticket.getAssignedStaffId() != null && Objects.equals(ticket.getAssignedStaffId(), staffId)) {
            ticketApplicationService.markReadForStaff(ticketId);
        }
        return toFeignDetail(buildDetail(ticket));
    }

    private TicketDetailRes buildDetail(MobiTicket ticket) {
        MobiUser user = mobiUserService.getById(ticket.getUserId());
        List<MobiTicketMessage> messages = mobiTicketMessageService.list(
                new LambdaQueryWrapper<MobiTicketMessage>()
                        .eq(MobiTicketMessage::getTicketId, ticket.getTicketId())
                        .orderByAsc(MobiTicketMessage::getCreateTime));
        TicketStatus status = TicketStatus.fromCode(ticket.getStatus());
        List<TicketMessageRes> messageRows = messages.stream()
                .map(m -> TicketAssembler.toMessageRes(m, user))
                .toList();
        int unreadCount = (int) messages.stream()
                .filter(m -> m.getIsRead() != null && m.getIsRead() == 0)
                .count();
        return TicketDetailRes.builder()
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
                .unreadCount(unreadCount)
                .messages(messageRows)
                .build();
    }

    private Page<TicketRes> toTicketResPage(Page<MobiTicket> page, int unreadSenderType) {
        List<Long> ticketIds = page.getRecords().stream().map(MobiTicket::getTicketId).toList();
        Map<Long, Integer> unreadMap = mobiTicketMessageService.countUnreadByTickets(ticketIds, unreadSenderType);
        List<Long> userIds = page.getRecords().stream().map(MobiTicket::getUserId).distinct().toList();
        Map<Long, MobiUser> userMap = userIds.isEmpty()
                ? Map.of()
                : mobiUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(MobiUser::getUserId, u -> u, (a, b) -> a));
        Page<TicketRes> resPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resPage.setRecords(page.getRecords().stream()
                .map(t -> {
                    TicketRes res = TicketAssembler.toRes(t, userMap.get(t.getUserId()));
                    res.setUnreadCount(unreadMap.getOrDefault(t.getTicketId(), 0));
                    return res;
                })
                .toList());
        return resPage;
    }

    private MobiTicket requireTicket(Long ticketId) {
        MobiTicket ticket = mobiTicketService.getById(ticketId);
        if (ticket == null) {
            throw new ServiceException("工单不存在", 404);
        }
        return ticket;
    }

    static void assertStaffCanAccess(MobiTicket ticket, Long staffId) {
        if (ticket.getAssignedStaffId() == null) {
            throw new ServiceException("请先认领工单", 403);
        }
        if (!Objects.equals(ticket.getAssignedStaffId(), staffId)) {
            throw new ServiceException("该工单由其他客服处理中", 403);
        }
    }

    /**
     * 校验查看者是否有权查看工单详情（含消息体）。
     * <ul>
     *   <li>工单所属 C 端用户：拒绝（应走用户接口）</li>
     *   <li>已认领且为归属客服：允许</li>
     *   <li>未认领且具备 system:ticket:query 或 system:ticket:list：允许</li>
     *   <li>其他：拒绝</li>
     * </ul>
     */
    static void assertCanViewTicketDetail(MobiTicket ticket, Long viewerId, LoginUser viewer) {
        if (Objects.equals(ticket.getUserId(), viewerId)) {
            throw new ServiceException("无权查看该工单", 403);
        }
        if (ticket.getAssignedStaffId() != null) {
            assertStaffCanAccess(ticket, viewerId);
            return;
        }
        Set<String> permissions = viewer.getPermissionSet();
        if (permissions.contains("system:ticket:query") || permissions.contains("system:ticket:list")) {
            return;
        }
        throw new ServiceException("无权查看该工单", 403);
    }

    private static MobiTicketDetailFeign toFeignDetail(TicketDetailRes detail) {
        List<MobiTicketMessageFeign> messages = detail.getMessages() == null
                ? List.of()
                : detail.getMessages().stream()
                .map(m -> MobiTicketMessageFeign.builder()
                        .messageId(m.getMessageId())
                        .senderType(m.getSenderType())
                        .senderLabel(m.getSenderLabel())
                        .senderId(m.getSenderId())
                        .senderAvatar(m.getSenderAvatar())
                        .content(m.getContent())
                        .isRead(m.getIsRead())
                        .createTime(m.getCreateTime())
                        .build())
                .toList();
        return MobiTicketDetailFeign.builder()
                .ticketId(detail.getTicketId())
                .userId(detail.getUserId())
                .nickname(detail.getNickname())
                .title(detail.getTitle())
                .description(detail.getDescription())
                .status(detail.getStatus())
                .statusLabel(detail.getStatusLabel())
                .assignedStaffId(detail.getAssignedStaffId())
                .createTime(detail.getCreateTime())
                .updateTime(detail.getUpdateTime())
                .unreadCount(detail.getUnreadCount())
                .messages(messages)
                .build();
    }
}
