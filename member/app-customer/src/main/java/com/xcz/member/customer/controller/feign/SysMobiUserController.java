package com.xcz.member.customer.controller.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.application.service.UserApplicationQueryService;
import com.xcz.member.customer.application.service.UserApplicationService;
import com.xcz.member.feign.dto.user.MobiUserFeign;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台小程序用户（Feign 入口）
 */
@RestController
@RequestMapping("/mobi/user/sys")
public class SysMobiUserController {

    @Resource
    private UserApplicationQueryService userApplicationQueryService;
    @Resource
    private UserApplicationService userApplicationService;

    @GetMapping("/list")
    public ResponseEntity<List<MobiUserFeign>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String openid,
            @RequestParam(required = false) Integer status) {
        Page<MobiUserFeign> page = userApplicationQueryService.pageForFeign(pageNum, pageSize, userId, nickname, phone, openid, status);
        return ResponseEntityUtils.okPage(page.getRecords(), page.getTotal(), "查询成功");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<MobiUserFeign> get(@PathVariable("userId") Long userId) {
        return ResponseEntityUtils.ok(userApplicationQueryService.getForFeign(userId), "查询成功");
    }

    @PutMapping
    public ResponseEntity<Void> edit(@RequestBody MobiUserFeign body) {
        userApplicationService.updateForSys(body);
        return ResponseEntityUtils.ok(null, "修改成功");
    }
}
