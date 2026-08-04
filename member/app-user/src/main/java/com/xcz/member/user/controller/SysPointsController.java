package com.xcz.member.user.controller;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.points.PointsAccountFeign;
import com.xcz.member.feign.dto.points.PointsAdjustFeign;
import com.xcz.member.feign.dto.points.PointsLogBriefFeign;
import com.xcz.member.feign.dto.points.PointsShopFeignQuery;
import com.xcz.member.feign.service.MobiPointsFeignService;
import com.xcz.member.user.service.admin.PointsAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台用户积分与消费记录（代理 app-customer）
 */
@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class SysPointsController {

    private final MobiPointsFeignService mobiPointsFeignService;
    private final PointsAdminService pointsAdminService;

    @GetMapping("/account/list")
    @PreAuthorize("@ss.hasPermi('system:user:points:list')")
    public ResponseEntity<List<PointsAccountFeign>> accountList(PointsShopFeignQuery query) {
        return mobiPointsFeignService.accountList(query);
    }

    @PostMapping("/account/adjust")
    @PreAuthorize("@ss.hasPermi('system:user:points:adjust')")
    public ResponseEntity<Void> adjustAccount(@RequestBody PointsAdjustFeign body) {
        return pointsAdminService.adjustAccount(body);
    }

    @GetMapping("/log/list")
    @PreAuthorize("@ss.hasPermi('system:user:consume:list')")
    public ResponseEntity<List<PointsLogBriefFeign>> logList(PointsShopFeignQuery query) {
        return mobiPointsFeignService.logList(query);
    }

    @GetMapping("/log/{logId}")
    @PreAuthorize("@ss.hasPermi('system:user:consume:query')")
    public ResponseEntity<PointsLogBriefFeign> logDetail(
            @PathVariable Long logId,
            @RequestParam(required = false) Long shopId) {
        return mobiPointsFeignService.logDetail(logId, shopId);
    }
}
