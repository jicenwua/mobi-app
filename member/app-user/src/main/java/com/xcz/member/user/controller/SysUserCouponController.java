package com.xcz.member.user.controller;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.coupon.MobiUserCouponFeign;
import com.xcz.member.feign.dto.coupon.MobiUserCouponGrantFeign;
import com.xcz.member.feign.service.MobiUserCouponFeignService;
import com.xcz.member.user.service.admin.UserCouponAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台用户折扣券管理（代理 app-customer）
 */
@RestController
@RequestMapping("/user/coupon")
@RequiredArgsConstructor
public class SysUserCouponController {

    private final MobiUserCouponFeignService mobiUserCouponFeignService;
    private final UserCouponAdminService userCouponAdminService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:user:coupon:list')")
    public ResponseEntity<List<MobiUserCouponFeign>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Integer status) {
        return mobiUserCouponFeignService.list(pageNum, pageSize, userId, shopId, templateId, status);
    }

    @PostMapping("/grant")
    @PreAuthorize("@ss.hasPermi('system:user:coupon:grant')")
    public ResponseEntity<Long> grant(@RequestBody MobiUserCouponGrantFeign body) {
        return userCouponAdminService.grant(body);
    }

    @DeleteMapping("/{userCouponId}")
    @PreAuthorize("@ss.hasPermi('system:user:coupon:remove')")
    public ResponseEntity<Void> remove(@PathVariable Long userCouponId) {
        return userCouponAdminService.remove(userCouponId);
    }
}
