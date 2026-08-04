package com.xcz.member.user.controller;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.activity.MobiActivityDetailVO;
import com.xcz.member.feign.dto.activity.MobiActivityFeign;
import com.xcz.member.feign.dto.activity.MobiActivitySaveDTO;
import com.xcz.member.feign.service.MobiActivityFeignService;
import com.xcz.member.user.service.admin.ShopActivityAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台店铺活动管理（代理 app-customer）
 */
@RestController
@RequestMapping("/shop/activity")
@RequiredArgsConstructor
public class SysShopActivityController {

    private final MobiActivityFeignService mobiActivityFeignService;
    private final ShopActivityAdminService shopActivityAdminService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:shop:activity:list')")
    public ResponseEntity<List<MobiActivityFeign>> list(
            @RequestParam(required = false) Long shopId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer activityType) {
        return mobiActivityFeignService.list(shopId, pageNum, pageSize, status, activityType);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:shop:activity:query')")
    public ResponseEntity<MobiActivityDetailVO> get(@PathVariable Long id) {
        return mobiActivityFeignService.get(id);
    }

    @PostMapping
    @PreAuthorize("@ss.hasAnyPermi('system:shop:activity:add,system:shop:activity:edit')")
    public ResponseEntity<Long> save(@RequestBody MobiActivitySaveDTO body) {
        return body.getActivityId() == null
                ? shopActivityAdminService.create(body)
                : shopActivityAdminService.update(body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:shop:activity:remove')")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        return shopActivityAdminService.remove(id);
    }

    @PutMapping("/{id}/stop")
    @PreAuthorize("@ss.hasPermi('system:shop:activity:edit')")
    public ResponseEntity<Void> stop(@PathVariable Long id) {
        return shopActivityAdminService.stop(id);
    }

    @PutMapping("/{id}/enable")
    @PreAuthorize("@ss.hasPermi('system:shop:activity:edit')")
    public ResponseEntity<Void> enable(@PathVariable Long id) {
        return shopActivityAdminService.enable(id);
    }
}
