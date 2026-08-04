package com.xcz.member.customer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.api.dto.request.ticket.TicketReq;
import com.xcz.member.customer.api.dto.response.ticket.TicketDetailRes;
import com.xcz.member.customer.api.dto.response.ticket.TicketRes;
import com.xcz.member.customer.application.service.ticket.TicketApplicationQueryService;
import com.xcz.member.customer.application.service.ticket.TicketApplicationService;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小程序端客服工单接口
 */
@RestController
@RequestMapping("/ticket")
public class MobiTicketController {

    @Resource
    private TicketApplicationService ticketApplicationService;
    @Resource
    private TicketApplicationQueryService ticketApplicationQueryService;

    @GetMapping("/unread-count")
    public ResponseEntity<Integer> unreadCount() {
        int count = ticketApplicationQueryService.countUnreadForUser(SecurityUtils.getUserId());
        return ResponseEntityUtils.ok(count, "查询成功");
    }

    @GetMapping("/my")
    public ResponseEntity<List<TicketRes>> myTickets(TicketReq query) {
        int pageNum = query.getPageNum() != null && query.getPageNum() > 0 ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 20;
        Page<TicketRes> page = ticketApplicationQueryService.pageMyTickets(
                SecurityUtils.getUserId(), pageNum, pageSize);
        ResponseEntity<List<TicketRes>> res = ResponseEntityUtils.ok(page.getRecords(), "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody TicketReq body) {
        Long id = ticketApplicationService.create(body.getTitle(), body.getDescription(), SecurityUtils.getUserId());
        return ResponseEntityUtils.ok(id, "创建成功");
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketDetailRes> detail(@PathVariable Long ticketId) {
        TicketDetailRes detail = ticketApplicationQueryService.getDetailForUser(ticketId, SecurityUtils.getUserId());
        return ResponseEntityUtils.ok(detail, "查询成功");
    }

    @PostMapping("/{ticketId}/message")
    public ResponseEntity<Long> reply(@PathVariable Long ticketId, @RequestBody TicketReq body) {
        Long id = ticketApplicationService.userReply(ticketId, body.getContent(), SecurityUtils.getUserId());
        return ResponseEntityUtils.ok(id, "发送成功");
    }
}
