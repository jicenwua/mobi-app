package com.xcz.member.customer.controller.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.api.dto.response.activity.ActivityRes;
import com.xcz.member.customer.application.assemblers.ActivityAssembler;
import com.xcz.member.customer.application.command.activity.MutateActivityCommand;
import com.xcz.member.customer.application.service.activity.ActivityApplicationQueryService;
import com.xcz.member.customer.application.service.activity.ActivityApplicationService;
import com.xcz.member.feign.dto.activity.MobiActivityDetailVO;
import com.xcz.member.feign.dto.activity.MobiActivityFeign;
import com.xcz.member.feign.dto.activity.MobiActivitySaveDTO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台店铺活动（Feign 入口）
 */
@RestController
@RequestMapping("/shop/sys/activity")
public class SysMobiActivityController {

    @Resource
    private ActivityApplicationQueryService activityApplicationQueryService;
    @Resource
    private ActivityApplicationService activityApplicationService;

    @GetMapping("/list")
    public ResponseEntity<List<MobiActivityFeign>> list(
            @RequestParam(required = false) Long shopId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer activityType) {
        Page<ActivityRes> page = activityApplicationQueryService.pageForFeign(
                ActivityAssembler.toFeignQueryCommand(shopId, pageNum, pageSize, status, activityType));
        List<MobiActivityFeign> rows = page.getRecords().stream()
                .map(ActivityAssembler::toFeign)
                .toList();
        ResponseEntity<List<MobiActivityFeign>> res = ResponseEntityUtils.ok(rows, "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MobiActivityDetailVO> get(@PathVariable("id") Long id) {
        ActivityRes detail = activityApplicationQueryService.getDetail(id);
        return ResponseEntityUtils.ok(ActivityAssembler.toDetailFeign(detail), "查询成功");
    }

    @PostMapping
    public ResponseEntity<Long> save(@RequestBody MobiActivitySaveDTO body) {
        MutateActivityCommand command = ActivityAssembler.toSaveCommand(body);
        if (body.getActivityId() == null) {
            return ResponseEntityUtils.ok(activityApplicationService.create(command), "创建成功");
        }
        activityApplicationService.update(command);
        return ResponseEntityUtils.ok(body.getActivityId(), "更新成功");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable("id") Long id) {
        activityApplicationService.remove(id);
        return ResponseEntityUtils.ok(null, "删除成功");
    }

    @PutMapping("/{id}/stop")
    public ResponseEntity<Void> stop(@PathVariable("id") Long id) {
        activityApplicationService.stop(id, null);
        return ResponseEntityUtils.ok(null, "活动已结束");
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<Void> enable(@PathVariable("id") Long id) {
        activityApplicationService.enable(id, null);
        return ResponseEntityUtils.ok(null, "活动已启用");
    }
}
