package com.xcz.member.customer.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.customer.infrastructure.entity.MobiTicketMessage;

import java.util.List;
import java.util.Map;

public interface MobiTicketMessageService extends IService<MobiTicketMessage> {

    /** 统计各工单未读数：key=ticketId, value=count */
    Map<Long, Integer> countUnreadByTickets(List<Long> ticketIds, int senderType);

    /** 统计用户全部未读消息数（来自指定发送方类型） */
    int countUnreadForUser(Long userId, int senderType);

    /** 统计当前客服负责工单的未读用户消息数 */
    int countUnreadForStaff(Long staffId, int senderType);

    /** 统计全部未完成工单的未读用户消息数 */
    int countListUnreadForStaff(int senderType);

    /** 将指定发送方类型的消息标记为已读 */
    void markReadBySenderType(Long ticketId, int senderType);
}
