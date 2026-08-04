package com.xcz.member.customer.controller.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.application.assemblers.PointAssembler;
import com.xcz.member.customer.application.service.point.PointApplicationQueryService;
import com.xcz.member.customer.application.service.point.PointApplicationService;
import com.xcz.member.customer.domain.dto.points.PointsShopQuery;
import com.xcz.member.customer.domain.vo.PointsAccountVO;
import com.xcz.member.customer.domain.vo.PointsLogBriefVO;
import com.xcz.member.feign.dto.points.PointsAccountFeign;
import com.xcz.member.feign.dto.points.PointsAdjustFeign;
import com.xcz.member.feign.dto.points.PointsLogBriefFeign;
import com.xcz.member.feign.dto.points.PointsShopFeignQuery;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台积分查询（Feign 入口）
 */
@RestController
@RequestMapping("/points/sys")
public class SysMobiPointsController {

    @Resource
    private PointApplicationQueryService pointApplicationQueryService;
    @Resource
    private PointApplicationService pointApplicationService;

    /**
     * 店铺会员积分账户列表。
     */
    @GetMapping("/account/list")
    public ResponseEntity<List<PointsAccountFeign>> accountList(PointsShopFeignQuery feignQuery) {
        PointsShopQuery query = PointAssembler.toShopQuery(feignQuery);
        Page<PointsAccountVO> page = pointApplicationQueryService.pageAccountsForFeign(query);
        List<PointsAccountFeign> rows = page.getRecords().stream()
                .map(PointAssembler::toAccountFeign)
                .toList();
        ResponseEntity<List<PointsAccountFeign>> res = ResponseEntityUtils.ok(rows, "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    /**
     * 手动调整用户积分（基础/赠送，增加/减少）。
     */
    @PostMapping("/account/adjust")
    public ResponseEntity<Void> adjustAccount(@RequestBody PointsAdjustFeign body) {
        pointApplicationService.adjustPoints(body);
        return ResponseEntityUtils.ok(null, "调整成功");
    }

    /**
     * 店铺全部消费记录（按消费时间倒序，列表不含商品明细）。
     */
    @GetMapping("/log/list")
    public ResponseEntity<List<PointsLogBriefFeign>> logList(PointsShopFeignQuery feignQuery) {
        PointsShopQuery query = PointAssembler.toShopQuery(feignQuery);
        Page<PointsLogBriefVO> page = pointApplicationQueryService.pageLogsForFeign(query);
        List<PointsLogBriefFeign> rows = page.getRecords().stream()
                .map(PointAssembler::toLogFeign)
                .toList();
        ResponseEntity<List<PointsLogBriefFeign>> res = ResponseEntityUtils.ok(rows, "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    /**
     * 消费流水详情（含商品明细、优惠券与积分变动）。
     */
    @GetMapping("/log/{logId}")
    public ResponseEntity<PointsLogBriefFeign> logDetail(
            @PathVariable Long logId,
            @RequestParam(required = false) Long shopId) {
        return ResponseEntityUtils.ok(
                PointAssembler.toLogDetailFeign(pointApplicationQueryService.getLogDetailForFeign(logId, shopId)),
                "查询成功");
    }
}
