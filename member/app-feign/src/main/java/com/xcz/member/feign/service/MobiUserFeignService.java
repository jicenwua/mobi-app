package com.xcz.member.feign.service;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.user.MobiUserFeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小程序用户 mobi_user Feign 客户端（管理后台）
 */
@FeignClient(
        value = "app-miniApp",
        path = "/mobi/user/sys",
        contextId = "MobiUserFeignService"
)
public interface MobiUserFeignService {

    @GetMapping("/list")
    ResponseEntity<List<MobiUserFeign>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String openid,
            @RequestParam(required = false) Integer status);

    @GetMapping("/{userId}")
    ResponseEntity<MobiUserFeign> get(@PathVariable("userId") Long userId);

    @PutMapping
    ResponseEntity<Void> edit(@RequestBody MobiUserFeign body);
}
