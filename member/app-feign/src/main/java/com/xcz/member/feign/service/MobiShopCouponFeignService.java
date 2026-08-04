package com.xcz.member.feign.service;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.coupon.MobiCouponFeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 店铺折扣券模板 Feign（管理后台）
 */
@FeignClient(
        value = "app-miniApp",
        path = "/shop/sys/coupon",
        contextId = "MobiShopCouponFeignService"
)
public interface MobiShopCouponFeignService {

    @GetMapping("/list")
    ResponseEntity<List<MobiCouponFeign>> list(
            @RequestParam(required = false) Long shopId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize);

    @GetMapping("/{templateId}")
    ResponseEntity<MobiCouponFeign> get(@PathVariable Long templateId);

    @PostMapping
    ResponseEntity<Boolean> add(@RequestBody MobiCouponFeign body);

    @PutMapping
    ResponseEntity<Boolean> edit(@RequestBody MobiCouponFeign body);

    @DeleteMapping("/{templateId}")
    ResponseEntity<Void> remove(@PathVariable Long templateId);

    @PutMapping("/{templateId}/stop")
    ResponseEntity<Void> stop(@PathVariable Long templateId);

    @PutMapping("/{templateId}/resume")
    ResponseEntity<Void> resume(@PathVariable Long templateId);
}
