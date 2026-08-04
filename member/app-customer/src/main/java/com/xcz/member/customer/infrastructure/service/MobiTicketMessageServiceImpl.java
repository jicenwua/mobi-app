package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.member.customer.domain.enums.TicketStatus;
import com.xcz.member.customer.domain.service.MobiTicketMessageService;
import com.xcz.member.customer.domain.service.MobiTicketService;
import com.xcz.member.customer.infrastructure.entity.MobiTicket;
import com.xcz.member.customer.infrastructure.entity.MobiTicketMessage;
import com.xcz.member.customer.infrastructure.mapper.MobiTicketMessageMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工单消息服务实现：未读计数与已读标记。
 */
@Service
public class MobiTicketMessageServiceImpl extends ServiceImpl<MobiTicketMessageMapper, MobiTicketMessage>
        implements MobiTicketMessageService {

    private static final int READ = 1;
    private static final int UNREAD = 0;

    @Resource
    private MobiTicketService mobiTicketService;

    /**
     * 按工单 ID 批量统计指定发送方类型的未读消息数。
     *
     * @param ticketIds  工单 ID 列表
     * @param senderType 发送方类型（用户/客服等）
     * @return key 为 ticketId，value 为未读条数
     */
    @Override
    public Map<Long, Integer> countUnreadByTickets(List<Long> ticketIds, int senderType) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MobiTicketMessage> rows = list(new LambdaQueryWrapper<MobiTicketMessage>()
                .in(MobiTicketMessage::getTicketId, ticketIds)
                .eq(MobiTicketMessage::getSenderType, senderType)
                .eq(MobiTicketMessage::getIsRead, UNREAD));
        return rows.stream().collect(Collectors.groupingBy(
                MobiTicketMessage::getTicketId,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }

    /**
     * 统计用户全部工单中来自指定发送方的未读消息总数。
     *
     * @param userId     用户 ID
     * @param senderType 发送方类型
     * @return 未读消息总数
     */
    @Override
    public int countUnreadForUser(Long userId, int senderType) {
        List<Long> ticketIds = mobiTicketService.list(new LambdaQueryWrapper<MobiTicket>()
                        .eq(MobiTicket::getUserId, userId)
                        .select(MobiTicket::getTicketId))
                .stream()
                .map(MobiTicket::getTicketId)
                .toList();
        if (ticketIds.isEmpty()) {
            return 0;
        }
        return (int) count(new LambdaQueryWrapper<MobiTicketMessage>()
                .in(MobiTicketMessage::getTicketId, ticketIds)
                .eq(MobiTicketMessage::getSenderType, senderType)
                .eq(MobiTicketMessage::getIsRead, UNREAD));
    }

    /**
     * 统计指定客服负责工单中来自指定发送方的未读消息总数（不含已完结工单）。
     *
     * @param staffId    客服/店员 ID
     * @param senderType 发送方类型
     * @return 未读消息总数
     */
    @Override
    public int countUnreadForStaff(Long staffId, int senderType) {
        List<Long> ticketIds = mobiTicketService.list(new LambdaQueryWrapper<MobiTicket>()
                        .eq(MobiTicket::getAssignedStaffId, staffId)
                        .ne(MobiTicket::getStatus, TicketStatus.COMPLETED.getCode())
                        .select(MobiTicket::getTicketId))
                .stream()
                .map(MobiTicket::getTicketId)
                .toList();
        if (ticketIds.isEmpty()) {
            return 0;
        }
        return (int) count(new LambdaQueryWrapper<MobiTicketMessage>()
                .in(MobiTicketMessage::getTicketId, ticketIds)
                .eq(MobiTicketMessage::getSenderType, senderType)
                .eq(MobiTicketMessage::getIsRead, UNREAD));
    }

    /**
     * 统计全部未完结工单中来自指定发送方的未读消息总数（客服列表页角标）。
     *
     * @param senderType 发送方类型
     * @return 未读消息总数
     */
    @Override
    public int countListUnreadForStaff(int senderType) {
        List<Long> ticketIds = mobiTicketService.list(new LambdaQueryWrapper<MobiTicket>()
                        .ne(MobiTicket::getStatus, TicketStatus.COMPLETED.getCode())
                        .select(MobiTicket::getTicketId))
                .stream()
                .map(MobiTicket::getTicketId)
                .toList();
        if (ticketIds.isEmpty()) {
            return 0;
        }
        return (int) count(new LambdaQueryWrapper<MobiTicketMessage>()
                .in(MobiTicketMessage::getTicketId, ticketIds)
                .eq(MobiTicketMessage::getSenderType, senderType)
                .eq(MobiTicketMessage::getIsRead, UNREAD));
    }

    /**
     * 将指定工单下来自某发送方的未读消息全部标记为已读。
     *
     * @param ticketId   工单 ID
     * @param senderType 发送方类型
     */
    @Override
    public void markReadBySenderType(Long ticketId, int senderType) {
        update(null, new LambdaUpdateWrapper<MobiTicketMessage>()
                .eq(MobiTicketMessage::getTicketId, ticketId)
                .eq(MobiTicketMessage::getSenderType, senderType)
                .eq(MobiTicketMessage::getIsRead, UNREAD)
                .set(MobiTicketMessage::getIsRead, READ));
    }
}
