package com.xcz.member.customer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.api.dto.request.coupon.CouponTemplateReq;
import com.xcz.member.customer.api.dto.request.coupon.UserCouponReq;
import com.xcz.member.customer.api.dto.response.coupon.CouponTemplateRes;
import com.xcz.member.customer.api.dto.response.coupon.UserCouponRes;
import com.xcz.member.customer.application.assemblers.CouponAssembler;
import com.xcz.member.customer.application.service.coupon.CouponApplicationQueryService;
import com.xcz.member.customer.application.service.coupon.CouponApplicationService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小程序端优惠券接口。
 */
@RestController
@RequestMapping("/coupon")
public class MobiCouponController {

    @Resource
    private CouponApplicationService couponApplicationService;
    @Resource
    private CouponApplicationQueryService couponApplicationQueryService;

    /**
     * 顾客：当前可领取的优惠券。
     */
    @GetMapping("/distributing")
    public ResponseEntity<List<CouponTemplateRes>> listDistributing(@RequestParam Long shopId) {
        List<CouponTemplateRes> list = couponApplicationQueryService.listDistributingForCustomer(shopId);
        return ResponseEntityUtils.ok(list, "查询成功");
    }

    /**
     * 店员/店长：全部券模板。
     */
    @GetMapping("/template/list")
    public ResponseEntity<List<CouponTemplateRes>> listTemplates(CouponTemplateReq query) {
        Page<CouponTemplateRes> page = couponApplicationQueryService.pageTemplatesForStaff(
                CouponAssembler.toQueryTemplateCommand(query));
        ResponseEntity<List<CouponTemplateRes>> res = ResponseEntityUtils.ok(page.getRecords(), "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    /**
     * 顾客：我的优惠券。
     */
    @GetMapping("/my")
    public ResponseEntity<List<UserCouponRes>> myCoupons(UserCouponReq query) {
        Page<UserCouponRes> page = couponApplicationQueryService.pageMyCoupons(
                CouponAssembler.toQueryUserCouponCommand(query));
        ResponseEntity<List<UserCouponRes>> res = ResponseEntityUtils.ok(page.getRecords(), "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    /**
     * 顾客：领取优惠券。
     */
    @PostMapping("/claim")
    public ResponseEntity<Long> claim(@RequestBody UserCouponReq dto) {
        Long id = couponApplicationService.claim(dto.getShopId(), dto.getTemplateId());
        return ResponseEntityUtils.ok(id, "领取成功");
    }

    /**
     * 店长：创建优惠券模板。
     */
    @PostMapping("/template")
    public ResponseEntity<Long> createTemplate(@RequestBody CouponTemplateReq dto) {
        Long id = couponApplicationService.createTemplate(CouponAssembler.toCreateCommand(dto));
        return ResponseEntityUtils.ok(id, "创建成功");
    }

    /**
     * 店长：全量更新券模板（未开始可改全部；手动停止仅改发放时间；其余阶段可改发放时间）。
     */
    @PutMapping("/template/{templateId}")
    public ResponseEntity<Void> updateTemplate(@PathVariable Long templateId, @RequestBody CouponTemplateReq dto) {
        couponApplicationService.updateTemplate(CouponAssembler.toUpdateCommand(templateId, dto));
        return ResponseEntityUtils.ok(null, "更新成功");
    }

    /**
     * 店长：调整发放时间与总量（发放中或未开始）。
     */
    @PutMapping("/template/{templateId}/distribution")
    public ResponseEntity<Void> adjustDistribution(@PathVariable Long templateId, @RequestBody CouponTemplateReq dto) {
        couponApplicationService.adjustDistribution(CouponAssembler.toAdjustCommand(templateId, dto));
        return ResponseEntityUtils.ok(null, "调整成功");
    }

    /**
     * 店长：追加优惠券库存。
     */
    @PutMapping("/template/{templateId}/stock")
    public ResponseEntity<Void> addStock(@PathVariable Long templateId, @RequestBody CouponTemplateReq dto) {
        couponApplicationService.addStock(CouponAssembler.toAddStockCommand(templateId, dto));
        return ResponseEntityUtils.ok(null, "库存追加成功");
    }

    /**
     * 店长：手动停止优惠券发放。
     */
    @PutMapping("/template/{templateId}/stop")
    public ResponseEntity<Void> stopDistribution(@PathVariable Long templateId, @RequestParam Long shopId) {
        couponApplicationService.stopDistribution(templateId, shopId);
        return ResponseEntityUtils.ok(null, "已停止发放");
    }

    /**
     * 店长：恢复手动停止的优惠券发放。
     */
    @PutMapping("/template/{templateId}/resume")
    public ResponseEntity<Void> resumeDistribution(@PathVariable Long templateId, @RequestParam Long shopId) {
        couponApplicationService.resumeDistribution(templateId, shopId);
        return ResponseEntityUtils.ok(null, "已恢复发放");
    }

    /**
     * 店长：删除优惠券模板。
     */
    @DeleteMapping("/template/{templateId}")
    public ResponseEntity<Void> removeTemplate(@PathVariable Long templateId) {
        couponApplicationService.removeTemplate(templateId);
        return ResponseEntityUtils.ok(null, "删除成功");
    }
}
