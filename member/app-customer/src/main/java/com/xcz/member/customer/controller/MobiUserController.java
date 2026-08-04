package com.xcz.member.customer.controller;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.api.dto.request.user.PasswordSetReq;
import com.xcz.member.customer.api.dto.request.user.PasswordVerifyReq;
import com.xcz.member.customer.api.dto.request.user.WxLoginReq;
import com.xcz.member.customer.application.service.UserApplicationService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 小程序端用户：登录、资料、密码
 */
@RestController
@RequestMapping("/app")
public class MobiUserController {
    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 微信 code 登录或注册
     *
     * @param wxLogin 登录参数
     * @return token 与用户信息
     */
    @PostMapping(value = "/login")
    public ResponseEntity<Map<String,Object>> loginWithProfile(@Validated @RequestBody WxLoginReq wxLogin) {
        return ResponseEntityUtils.ok(userApplicationService.doLogin(wxLogin), "登录成功");
    }

    /**
     * 更新昵称、头像
     *
     * @param nickName   用户信息
     * @param avatarFile 头像文件，可选
     * @return 操作结果
     */
    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> update(
            @RequestParam (required = false) String nickName,
            @RequestPart(value = "file", required = false) MultipartFile avatarFile) {
        userApplicationService.updateUserInfo(nickName, avatarFile);
        return ResponseEntityUtils.ok(null, "更新成功");
    }

    /**
     * 设置或修改支付密码
     *
     * @param password 密码数据
     * @return 操作结果
     */
    @PostMapping("/password")
    public ResponseEntity<Void> updatePassword(@Validated@RequestBody PasswordSetReq password) {
        userApplicationService.updatePassword(password);
        return ResponseEntityUtils.ok(null, "设置成功");
    }

    /**
     * 校验支付密码（出示付款码前）
     *
     * @param password 6 位支付密码
     * @return 操作结果
     */
	@PostMapping("/password/verify")
	public ResponseEntity<Void> verifyPassword(@Validated @RequestBody PasswordVerifyReq password) {
        userApplicationService.verifyPassword(password);
        return ResponseEntityUtils.ok(null, "校验成功");
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息（含 token 续签）
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntityUtils.ok(userApplicationService.getUserInfo());
    }
}
