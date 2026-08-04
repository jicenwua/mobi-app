package com.xcz.member.user.controller;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.user.annotation.OperateLog;
import com.xcz.member.user.domain.dto.LoginDTO;
import com.xcz.member.user.service.SysLoginService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/system/auth")
public class SysLoginController {

    @Resource
    private SysLoginService sysLoginService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginDTO loginDTO) throws IllegalAccessException {
        String login = sysLoginService.login(loginDTO);
        return ResponseEntityUtils.ok(login);
    }

    @PostMapping("/logout")
    @OperateLog(module = "系统认证", operation = "退出登录", saveParams = false)
    public ResponseEntity<Boolean> logout() throws IllegalAccessException {
        return ResponseEntityUtils.ok(sysLoginService.logout());
    }

}
