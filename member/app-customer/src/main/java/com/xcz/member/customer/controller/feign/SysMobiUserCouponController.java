package com.xcz.member.customer.controller.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.api.dto.response.coupon.UserCouponRes;
import com.xcz.member.customer.application.assemblers.CouponAssembler;
import com.xcz.member.customer.application.service.coupon.CouponApplicationQueryService;
import com.xcz.member.customer.application.service.coupon.CouponApplicationService;
import com.xcz.member.feign.dto.coupon.MobiUserCouponFeign;
import com.xcz.member.feign.dto.coupon.MobiUserCouponGrantFeign;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台用户优惠券（Feign 入口）
 */
@RestController
@RequestMapping("/user-coupon/sys")
public class SysMobiUserCouponController {

    @Resource
    private CouponApplicationQueryService couponApplicationQueryService;
    @Resource
    private CouponApplicationService couponApplicationService;

    @GetMapping("/list")
    public ResponseEntity<List<MobiUserCouponFeign>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Integer status) {
        Page<UserCouponRes> page = couponApplicationQueryService.pageUserCouponsForFeign(
                userId, shopId, templateId, status, pageNum, pageSize);
        List<MobiUserCouponFeign> rows = page.getRecords().stream()
                .map(CouponAssembler::toFeign)
                .toList();
        ResponseEntity<List<MobiUserCouponFeign>> res = ResponseEntityUtils.ok(rows, "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    @PostMapping("/grant")
    public ResponseEntity<Long> grant(@RequestBody MobiUserCouponGrantFeign body) {
        return ResponseEntityUtils.ok(couponApplicationService.grantUserCoupon(body), "发放成功");
    }

    @DeleteMapping("/{userCouponId}")
    public ResponseEntity<Void> remove(@PathVariable Long userCouponId) {
        couponApplicationService.removeUserCoupon(userCouponId);
        return ResponseEntityUtils.ok(null, "删除成功");
    }
}
