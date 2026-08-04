package com.xcz.member.feign.service;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.coupon.MobiUserCouponFeign;
import com.xcz.member.feign.dto.coupon.MobiUserCouponGrantFeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户折扣券 Feign（管理后台）
 */
@FeignClient(
        value = "app-miniApp",
        path = "/user-coupon/sys",
        contextId = "MobiUserCouponFeignService"
)
public interface MobiUserCouponFeignService {

    @GetMapping("/list")
    ResponseEntity<List<MobiUserCouponFeign>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Integer status);

    @PostMapping("/grant")
    ResponseEntity<Long> grant(@RequestBody MobiUserCouponGrantFeign body);

    @DeleteMapping("/{userCouponId}")
    ResponseEntity<Void> remove(@PathVariable Long userCouponId);
}
