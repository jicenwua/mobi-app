package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.member.customer.domain.enums.TicketStatus;
import com.xcz.member.customer.domain.service.MobiTicketService;
import com.xcz.member.customer.infrastructure.entity.MobiTicket;
import com.xcz.member.customer.infrastructure.mapper.MobiTicketMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 工单服务实现：客服认领待处理工单。
 */
@Service
public class MobiTicketServiceImpl extends ServiceImpl<MobiTicketMapper, MobiTicket> implements MobiTicketService {

    /**
     * 客服认领待处理工单（乐观条件：仅 pending 且未分配时可成功）。
     *
     * @param ticketId 工单 ID
     * @param staffId  认领客服/店员 ID
     * @return 是否认领成功
     */
    @Override
    public boolean claim(Long ticketId, Long staffId) {
        return update(null, new LambdaUpdateWrapper<MobiTicket>()
                .eq(MobiTicket::getTicketId, ticketId)
                .eq(MobiTicket::getStatus, TicketStatus.PENDING.getCode())
                .isNull(MobiTicket::getAssignedStaffId)
                .set(MobiTicket::getStatus, TicketStatus.PROCESSING.getCode())
                .set(MobiTicket::getAssignedStaffId, staffId)
                .set(MobiTicket::getUpdateTime, LocalDateTime.now()));
    }
}
