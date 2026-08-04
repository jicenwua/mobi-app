package com.xcz.member.user.controller;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.user.MobiUserFeign;
import com.xcz.member.feign.service.MobiUserFeignService;
import com.xcz.member.user.annotation.OperateLog;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台：小程序用户（mobi_user），代理 app-customer。
 */
@RestController
@RequestMapping("/user/mobi-user")
@RequiredArgsConstructor
public class SysMobiUserController {

    private final MobiUserFeignService mobiUserFeignService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:mobi:user:list')")
    public ResponseEntity<List<MobiUserFeign>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String openid,
            @RequestParam(required = false) Integer status) {
        return mobiUserFeignService.list(pageNum, pageSize, userId, nickname, phone, openid, status);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@ss.hasPermi('system:mobi:user:query')")
    public ResponseEntity<MobiUserFeign> get(@PathVariable Long userId) {
        return mobiUserFeignService.get(userId);
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:mobi:user:edit')")
    @OperateLog(module = "小程序用户", operation = "修改用户")
    public ResponseEntity<Void> edit(@RequestBody MobiUserFeign body) {
        return mobiUserFeignService.edit(body);
    }
}
