package com.xcz.member.customer.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.customer.infrastructure.entity.MobiTicket;

public interface MobiTicketService extends IService<MobiTicket> {

    /**
     * 认领工单：仅待处理且未分配时可成功，防止并发抢单。
     *
     * @return 是否更新成功
     */
    boolean claim(Long ticketId, Long staffId);
}
