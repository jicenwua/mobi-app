package com.xcz.member.feign.service;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.ticket.MobiTicketDetailFeign;
import com.xcz.member.feign.dto.ticket.MobiTicketFeign;
import com.xcz.member.feign.dto.ticket.MobiTicketReplyFeign;
import com.xcz.member.feign.dto.ticket.MobiTicketUnreadSummaryFeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客服工单 Feign（管理后台）
 */
@FeignClient(
        value = "app-miniApp",
        path = "/ticket/sys",
        contextId = "MobiTicketFeignService"
)
public interface MobiTicketFeignService {

    @GetMapping("/unread-summary")
    ResponseEntity<MobiTicketUnreadSummaryFeign> unreadSummary();

    @GetMapping("/list")
    ResponseEntity<List<MobiTicketFeign>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status);

    @GetMapping("/mine")
    ResponseEntity<List<MobiTicketFeign>> mine(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status);

    @GetMapping("/{ticketId}")
    ResponseEntity<MobiTicketDetailFeign> detail(@PathVariable Long ticketId);

    @PostMapping("/{ticketId}/claim")
    ResponseEntity<Void> claim(@PathVariable Long ticketId);

    @PostMapping("/{ticketId}/reply")
    ResponseEntity<Long> reply(@PathVariable Long ticketId, @RequestBody MobiTicketReplyFeign body);

    @PostMapping("/{ticketId}/complete")
    ResponseEntity<Void> complete(@PathVariable Long ticketId);
}
