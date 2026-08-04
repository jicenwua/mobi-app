package com.xcz.member.user.controller;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.ticket.MobiTicketDetailFeign;
import com.xcz.member.feign.dto.ticket.MobiTicketFeign;
import com.xcz.member.feign.dto.ticket.MobiTicketReplyFeign;
import com.xcz.member.feign.dto.ticket.MobiTicketUnreadSummaryFeign;
import com.xcz.member.feign.service.MobiTicketFeignService;
import com.xcz.member.user.service.admin.TicketAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台客服工单管理（代理 app-customer）
 */
@RestController
@RequestMapping("/ticket")
@RequiredArgsConstructor
public class SysTicketController {

    private final MobiTicketFeignService mobiTicketFeignService;
    private final TicketAdminService ticketAdminService;

    @GetMapping("/unread-summary")
    @PreAuthorize("@ss.hasPermi('system:ticket:list')")
    public ResponseEntity<MobiTicketUnreadSummaryFeign> unreadSummary() {
        return mobiTicketFeignService.unreadSummary();
    }

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:ticket:list')")
    public ResponseEntity<List<MobiTicketFeign>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        return mobiTicketFeignService.list(pageNum, pageSize, userId, status);
    }

    @GetMapping("/mine")
    @PreAuthorize("@ss.hasPermi('system:ticket:mine')")
    public ResponseEntity<List<MobiTicketFeign>> mine(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        return mobiTicketFeignService.mine(pageNum, pageSize, status);
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("@ss.hasPermi('system:ticket:query')")
    public ResponseEntity<MobiTicketDetailFeign> detail(@PathVariable Long ticketId) {
        return mobiTicketFeignService.detail(ticketId);
    }

    @PostMapping("/{ticketId}/claim")
    @PreAuthorize("@ss.hasPermi('system:ticket:claim')")
    public ResponseEntity<Void> claim(@PathVariable Long ticketId) {
        return ticketAdminService.claim(ticketId);
    }

    @PostMapping("/{ticketId}/reply")
    @PreAuthorize("@ss.hasPermi('system:ticket:reply')")
    public ResponseEntity<Long> reply(@PathVariable Long ticketId, @RequestBody MobiTicketReplyFeign body) {
        return ticketAdminService.reply(ticketId, body);
    }

    @PostMapping("/{ticketId}/complete")
    @PreAuthorize("@ss.hasPermi('system:ticket:complete')")
    public ResponseEntity<Void> complete(@PathVariable Long ticketId) {
        return ticketAdminService.complete(ticketId);
    }
}
