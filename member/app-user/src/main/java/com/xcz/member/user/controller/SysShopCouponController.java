package com.xcz.member.user.controller;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.coupon.MobiCouponFeign;
import com.xcz.member.feign.service.MobiShopCouponFeignService;
import com.xcz.member.user.service.admin.ShopCouponAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台店铺折扣券模板管理（代理 app-customer）
 */
@RestController
@RequestMapping("/shop/coupon")
@RequiredArgsConstructor
public class SysShopCouponController {

    private final MobiShopCouponFeignService mobiShopCouponFeignService;
    private final ShopCouponAdminService shopCouponAdminService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:shop:coupon:list')")
    public ResponseEntity<List<MobiCouponFeign>> list(
            @RequestParam(required = false) Long shopId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return mobiShopCouponFeignService.list(shopId, pageNum, pageSize);
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("@ss.hasPermi('system:shop:coupon:query')")
    public ResponseEntity<MobiCouponFeign> get(@PathVariable Long templateId) {
        return mobiShopCouponFeignService.get(templateId);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:shop:coupon:add')")
    public ResponseEntity<Boolean> add(@RequestBody MobiCouponFeign body) {
        return shopCouponAdminService.add(body);
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:shop:coupon:edit')")
    public ResponseEntity<Boolean> edit(@RequestBody MobiCouponFeign body) {
        return shopCouponAdminService.edit(body);
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("@ss.hasPermi('system:shop:coupon:remove')")
    public ResponseEntity<Void> remove(@PathVariable Long templateId) {
        return shopCouponAdminService.remove(templateId);
    }

    @PutMapping("/{templateId}/stop")
    @PreAuthorize("@ss.hasPermi('system:shop:coupon:edit')")
    public ResponseEntity<Void> stop(@PathVariable Long templateId) {
        return shopCouponAdminService.stop(templateId);
    }

    @PutMapping("/{templateId}/resume")
    @PreAuthorize("@ss.hasPermi('system:shop:coupon:edit')")
    public ResponseEntity<Void> resume(@PathVariable Long templateId) {
        return shopCouponAdminService.resume(templateId);
    }
}
