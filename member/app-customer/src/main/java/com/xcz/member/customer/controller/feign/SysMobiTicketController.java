package com.xcz.member.customer.controller.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.api.dto.response.ticket.TicketRes;
import com.xcz.member.customer.application.assemblers.TicketAssembler;
import com.xcz.member.customer.application.service.ticket.TicketApplicationQueryService;
import com.xcz.member.customer.application.service.ticket.TicketApplicationService;
import com.xcz.member.feign.dto.ticket.MobiTicketDetailFeign;
import com.xcz.member.feign.dto.ticket.MobiTicketFeign;
import com.xcz.member.feign.dto.ticket.MobiTicketReplyFeign;
import com.xcz.member.feign.dto.ticket.MobiTicketUnreadSummaryFeign;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台客服工单（Feign 入口）
 */
@RestController
@RequestMapping("/ticket/sys")
public class SysMobiTicketController {

    @Resource
    private TicketApplicationQueryService ticketApplicationQueryService;
    @Resource
    private TicketApplicationService ticketApplicationService;

    @GetMapping("/unread-summary")
    public ResponseEntity<MobiTicketUnreadSummaryFeign> unreadSummary() {
        return ResponseEntityUtils.ok(
                ticketApplicationQueryService.unreadSummaryForStaff(SecurityUtils.getUserId()), "查询成功");
    }

    @GetMapping("/list")
    public ResponseEntity<List<MobiTicketFeign>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        Page<TicketRes> page = ticketApplicationQueryService.pageAllTickets(userId, status, pageNum, pageSize);
        List<MobiTicketFeign> rows = page.getRecords().stream().map(TicketAssembler::toFeign).toList();
        ResponseEntity<List<MobiTicketFeign>> res = ResponseEntityUtils.ok(rows, "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    @GetMapping("/mine")
    public ResponseEntity<List<MobiTicketFeign>> mine(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        Page<TicketRes> page = ticketApplicationQueryService.pageMyAssignedTickets(
                SecurityUtils.getUserId(), status, pageNum, pageSize);
        List<MobiTicketFeign> rows = page.getRecords().stream().map(TicketAssembler::toFeign).toList();
        ResponseEntity<List<MobiTicketFeign>> res = ResponseEntityUtils.ok(rows, "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<MobiTicketDetailFeign> detail(@PathVariable Long ticketId) {
        return ResponseEntityUtils.ok(
                ticketApplicationQueryService.getDetailForFeign(ticketId, SecurityUtils.getUserId()), "查询成功");
    }

    @PostMapping("/{ticketId}/claim")
    public ResponseEntity<Void> claim(@PathVariable Long ticketId) {
        ticketApplicationService.claim(ticketId, SecurityUtils.getUserId());
        return ResponseEntityUtils.ok(null, "认领成功");
    }

    @PostMapping("/{ticketId}/reply")
    public ResponseEntity<Long> reply(@PathVariable Long ticketId, @RequestBody MobiTicketReplyFeign body) {
        Long messageId = ticketApplicationService.staffReply(
                ticketId, body != null ? body.getContent() : null, SecurityUtils.getUserId());
        return ResponseEntityUtils.ok(messageId, "回复成功");
    }

    @PostMapping("/{ticketId}/complete")
    public ResponseEntity<Void> complete(@PathVariable Long ticketId) {
        ticketApplicationService.complete(ticketId, SecurityUtils.getUserId());
        return ResponseEntityUtils.ok(null, "已完成");
    }
}
