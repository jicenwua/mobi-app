package com.xcz.member.customer.controller.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.api.dto.response.coupon.CouponTemplateRes;
import com.xcz.member.customer.application.assemblers.CouponAssembler;
import com.xcz.member.customer.application.service.coupon.CouponApplicationQueryService;
import com.xcz.member.customer.application.service.coupon.CouponApplicationService;
import com.xcz.member.feign.dto.coupon.MobiCouponFeign;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台店铺优惠券模板（Feign 入口）
 */
@RestController
@RequestMapping("/shop/sys/coupon")
public class SysMobiShopCouponController {

    @Resource
    private CouponApplicationQueryService couponApplicationQueryService;
    @Resource
    private CouponApplicationService couponApplicationService;

    @GetMapping("/list")
    public ResponseEntity<List<MobiCouponFeign>> list(
            @RequestParam(required = false) Long shopId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<CouponTemplateRes> page = couponApplicationQueryService.pageTemplatesForFeign(shopId, pageNum, pageSize);
        List<MobiCouponFeign> rows = page.getRecords().stream()
                .map(CouponAssembler::toFeign)
                .toList();
        ResponseEntity<List<MobiCouponFeign>> res = ResponseEntityUtils.ok(rows, "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<MobiCouponFeign> get(@PathVariable Long templateId) {
        return ResponseEntityUtils.ok(
                CouponAssembler.toFeign(couponApplicationQueryService.getTemplateForFeign(templateId)),
                "查询成功");
    }

    @PostMapping
    public ResponseEntity<Boolean> add(@RequestBody MobiCouponFeign body) {
        couponApplicationService.createTemplate(CouponAssembler.toCreateCommand(body));
        return ResponseEntityUtils.ok(true, "新增成功");
    }

    @PutMapping
    public ResponseEntity<Boolean> edit(@RequestBody MobiCouponFeign body) {
        couponApplicationService.updateTemplate(CouponAssembler.toUpdateCommand(body));
        return ResponseEntityUtils.ok(true, "修改成功");
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> remove(@PathVariable Long templateId) {
        couponApplicationService.removeTemplate(templateId);
        return ResponseEntityUtils.ok(null, "删除成功");
    }

    @PutMapping("/{templateId}/stop")
    public ResponseEntity<Void> stop(@PathVariable Long templateId) {
        couponApplicationService.stopDistribution(templateId, null);
        return ResponseEntityUtils.ok(null, "已停止发放");
    }

    @PutMapping("/{templateId}/resume")
    public ResponseEntity<Void> resume(@PathVariable Long templateId) {
        couponApplicationService.resumeDistribution(templateId, null);
        return ResponseEntityUtils.ok(null, "已恢复发放");
    }
}
