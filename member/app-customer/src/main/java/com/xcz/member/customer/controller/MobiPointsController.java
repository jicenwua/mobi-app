package com.xcz.member.customer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.api.dto.request.point.*;
import com.xcz.member.customer.api.dto.response.point.*;
import com.xcz.member.customer.application.assemblers.PointAssembler;
import com.xcz.member.customer.application.assemblers.PurchaseAssembler;
import com.xcz.member.customer.application.service.point.PointApplicationQueryService;
import com.xcz.member.customer.application.service.point.PointsConsumeApplicationService;
import com.xcz.member.customer.application.service.point.PointsRechargeApplicationService;
import com.xcz.member.customer.application.service.point.PurchaseApplicationService;
import com.xcz.member.customer.domain.vo.PointsPurchaseResultVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 小程序积分与消费记录接口。
 */
@RestController
@RequestMapping("/points")
@Validated
public class MobiPointsController {

    @Resource
    private PointApplicationQueryService pointApplicationQueryService;
    @Resource
    private PointsConsumeApplicationService pointsConsumeApplicationService;
    @Resource
    private PurchaseApplicationService purchaseApplicationService;
    @Resource
    private PointsRechargeApplicationService pointsRechargeApplicationService;

    /**
     * 店员：店铺会员积分账户（小程序专用 VO）。
     */
    @GetMapping("/shop/account/list")
    public ResponseEntity<List<PointsAccountListRes>> shopAccountList(PointReq query) {
        Page<PointsAccountListRes> page = pointApplicationQueryService.pageShopAccounts(query);
        ResponseEntity<List<PointsAccountListRes>> res = ResponseEntityUtils.ok(page.getRecords(), "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    /**
     * 店员：店铺全部消费记录（小程序专用 VO）。
     */
    @GetMapping("/shop/log/list")
    public ResponseEntity<List<ConsumeLogRes>> shopLogList(PointReq query) {
        Page<ConsumeLogRes> page = pointApplicationQueryService.pageShopConsumeLogs(query);
        ResponseEntity<List<ConsumeLogRes>> res = ResponseEntityUtils.ok(page.getRecords(), "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    /**
     * 顾客：本人在店铺的消费记录。
     */
    @GetMapping("/my/log/list")
    public ResponseEntity<List<ConsumeLogRes>> myLogList(PointReq query) {
        Page<ConsumeLogRes> page = pointApplicationQueryService.pageMyConsumeLogs(query);
        ResponseEntity<List<ConsumeLogRes>> res = ResponseEntityUtils.ok(page.getRecords(), "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    /**
     * 消费记录详情（顾客仅可查本人，店员可查店铺内任意记录）。
     */
    @GetMapping("/log/{logId}")
    public ResponseEntity<ConsumeLogDetailRes> logDetail(@PathVariable @NotNull Long logId,
                                                        @RequestParam @NotNull Long shopId) {
        ConsumeLogDetailRes detail = pointApplicationQueryService.getConsumeLogDetail(logId, shopId);
        return ResponseEntityUtils.ok(detail, "查询成功");
    }

    /**
     * 店员扫码扣减会员积分。
     *
     * @param dto 扣款请求（shopId、付款码 token、购物清单）
     * @return 本次消耗积分
     */
    @PostMapping("/consume")
    public ResponseEntity<BigDecimal> consume(@Valid @RequestBody PointsConsumeReq req) {
        BigDecimal points = pointsConsumeApplicationService.staffConsume(PointAssembler.toConsumeDto(req));
        return ResponseEntityUtils.ok(points, "扣款成功");
    }

    /**
     * 店员/店长线下充值积分（基础积分 + 充值满赠，单条流水）。
     */
    @PostMapping("/recharge")
    public ResponseEntity<PointsRechargeRes> recharge(@Valid @RequestBody PointsRechargeReq req) {
        return ResponseEntityUtils.ok(
                PointAssembler.toRechargeRes(pointsRechargeApplicationService.staffRecharge(PointAssembler.toRechargeDto(req))),
                "充值成功");
    }

    /**
     * 会员自助购买商品，扣减积分并生成待使用订单。
     */
    @PostMapping("/purchase")
    public ResponseEntity<PointsPurchaseRes> purchase(@Valid @RequestBody PointsPurchaseReq req) {
        PointsPurchaseResultVO result = purchaseApplicationService.purchase(PointAssembler.toPurchaseDto(req));
        return ResponseEntityUtils.ok(PurchaseAssembler.toPurchaseRes(result), "购买成功");
    }

    /**
     * 顾客：本人在全部店铺的订单列表（分页，可按状态筛选）。
     */
    @GetMapping("/my/order/list")
    public ResponseEntity<List<ConsumeLogRes>> myOrderList(PointReq query) {
        Page<ConsumeLogRes> page = pointApplicationQueryService.pageMyOrders(query);
        ResponseEntity<List<ConsumeLogRes>> res = ResponseEntityUtils.ok(page.getRecords(), "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    /**
     * 店员扫码核销待使用订单。
     */
    @PostMapping("/order/verify")
    public ResponseEntity<Long> verifyOrder(@Valid @RequestBody OrderVerifyReq req) {
        Long logId = pointsConsumeApplicationService.staffVerifyOrder(req.getShopId(), req.getToken());
        return ResponseEntityUtils.ok(logId, "核销成功");
    }

    /**
     * 会员自助退款待使用订单，返还积分。
     */
    @PostMapping("/order/refund")
    public ResponseEntity<BigDecimal> refundOrder(@Valid @RequestBody OrderRefundReq req) {
        BigDecimal remaining = pointsConsumeApplicationService.memberRefundOrder(req.getLogId(), req.getShopId());
        return ResponseEntityUtils.ok(remaining, "退款成功");
    }

}
