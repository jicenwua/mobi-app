package com.xcz.member.user.service.admin;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.ticket.MobiTicketReplyFeign;
import com.xcz.member.feign.service.MobiTicketFeignService;
import com.xcz.member.user.annotation.OperateLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 客服工单后台写操作。
 */
@Service
@RequiredArgsConstructor
public class TicketAdminService {

    private final MobiTicketFeignService mobiTicketFeignService;

    @OperateLog(module = "客服工单", operation = "认领工单")
    public ResponseEntity<Void> claim(Long ticketId) {
        return mobiTicketFeignService.claim(ticketId);
    }

    public ResponseEntity<Long> reply(Long ticketId, MobiTicketReplyFeign body) {
        return mobiTicketFeignService.reply(ticketId, body);
    }

    @OperateLog(module = "客服工单", operation = "完成工单")
    public ResponseEntity<Void> complete(Long ticketId) {
        return mobiTicketFeignService.complete(ticketId);
    }
}
