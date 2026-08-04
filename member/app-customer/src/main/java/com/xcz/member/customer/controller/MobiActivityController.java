package com.xcz.member.customer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.api.dto.request.activity.ActivityReq;
import com.xcz.member.customer.api.dto.response.activity.ActivityRes;
import com.xcz.member.customer.application.assemblers.ActivityAssembler;
import com.xcz.member.customer.application.service.activity.ActivityApplicationQueryService;
import com.xcz.member.customer.application.service.activity.ActivityApplicationService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小程序端活动接口。
 */
@RestController
@RequestMapping("/activity")
public class MobiActivityController {

    @Resource
    private ActivityApplicationService activityApplicationService;
    @Resource
    private ActivityApplicationQueryService activityApplicationQueryService;

    /**
     * 店铺成员：当前进行中的满赠活动（含规则，走 Redis）。
     */
    @GetMapping("/ongoing")
    public ResponseEntity<List<ActivityRes>> listOngoing(@RequestParam Long shopId) {
        List<ActivityRes> list = activityApplicationQueryService.listOngoingForCustomer(shopId);
        return ResponseEntityUtils.ok(list, "查询成功");
    }

    /**
     * 店铺成员：最新公告（含详情，走 Redis）。
     */
    @GetMapping("/announcement/latest")
    public ResponseEntity<ActivityRes> latestAnnouncement(@RequestParam Long shopId) {
        ActivityRes vo = activityApplicationQueryService.getLatestAnnouncement(shopId);
        return ResponseEntityUtils.ok(vo, "查询成功");
    }

    /**
     * 店员/店长：全部活动列表（含规则，走 Redis）。
     */
    @GetMapping("/list")
    public ResponseEntity<List<ActivityRes>> list(ActivityReq query) {
        Page<ActivityRes> page = activityApplicationQueryService.pageForStaff(ActivityAssembler.toQueryCommand(query));
        ResponseEntity<List<ActivityRes>> res = ResponseEntityUtils.ok(page.getRecords(), "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    /**
     * 活动详情（已加入店铺的成员均可查看）。
     */
    @GetMapping("/{activityId}")
    public ResponseEntity<ActivityRes> detail(@PathVariable Long activityId) {
        return ResponseEntityUtils.ok(activityApplicationQueryService.getDetail(activityId), "查询成功");
    }

    /**
     * 店长：创建活动。
     */
    @PostMapping
    public ResponseEntity<Long> create(@RequestBody ActivityReq dto) {
        Long id = activityApplicationService.create(ActivityAssembler.toCreateCommand(dto));
        return ResponseEntityUtils.ok(id, "创建成功");
    }

    /**
     * 店长：更新活动（手动停止时仅改时间；其余按阶段限制可改字段）。
     */
    @PutMapping("/{activityId}")
    public ResponseEntity<Void> update(@PathVariable Long activityId, @RequestBody ActivityReq dto) {
        activityApplicationService.update(ActivityAssembler.toUpdateCommand(activityId, dto));
        return ResponseEntityUtils.ok(null, "更新成功");
    }

    /**
     * 店长：手动结束活动。
     */
    @PutMapping("/{activityId}/stop")
    public ResponseEntity<Void> stop(@PathVariable Long activityId, @RequestParam Long shopId) {
        activityApplicationService.stop(activityId, shopId);
        return ResponseEntityUtils.ok(null, "活动已结束");
    }

    /**
     * 店长：重新启用手动停止的活动。
     */
    @PutMapping("/{activityId}/enable")
    public ResponseEntity<Void> enable(@PathVariable Long activityId, @RequestParam Long shopId) {
        activityApplicationService.enable(activityId, shopId);
        return ResponseEntityUtils.ok(null, "活动已启用");
    }

    /**
     * 店长：删除活动。
     */
    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> remove(@PathVariable Long activityId) {
        activityApplicationService.remove(activityId);
        return ResponseEntityUtils.ok(null, "删除成功");
    }
}
