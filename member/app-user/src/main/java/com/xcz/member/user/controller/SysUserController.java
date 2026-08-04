package com.xcz.member.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.security.extend.LoginUser;
import com.xcz.commons.security.service.TokenService;
import com.xcz.member.user.annotation.OperateLog;
import com.xcz.member.user.domain.dto.RegisterDTO;
import com.xcz.member.user.domain.dto.UserDto;
import com.xcz.member.user.domain.dto.UserUpdateDto;
import com.xcz.member.user.domain.vo.UserInfoVO;
import com.xcz.member.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表
     */
    @PostMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    public ResponseEntity<List<UserInfoVO>> getUserList(@RequestBody UserDto userDto) {
        Page<UserInfoVO> page = userService.getPageList(userDto);
        ResponseEntity<List<UserInfoVO>> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg("查询成功");
        response.setData(page.getRecords());
        response.setTotal(page.getTotal());
        return response;
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info/{userId}")
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    public ResponseEntity<UserInfoVO> getUserInfo(@PathVariable Long userId) {
        UserInfoVO userInfo = userService.getUserInfo(userId);
        ResponseEntity<UserInfoVO> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg("查询成功");
        response.setData(userInfo);
        return response;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserInfoVO> getCurrentUserInfo() throws IllegalAccessException {
        //从已登录的security中获取用户信息
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserInfoVO userInfo = userService.getUserInfo(loginUser.getUserId());
        ResponseEntity<UserInfoVO> response = new ResponseEntity<>();
        tokenService.refreshToken(loginUser.getToken());
        response.setCode(200);
        response.setMsg("查询成功");
        response.setData(userInfo);
        return response;
    }

    /**
     * 更新用户信息
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @OperateLog(module = "系统用户", operation = "修改用户", saveParams = false)
    public ResponseEntity<Void> updateUserInfo(@RequestBody UserUpdateDto userUpdateDto) {
        boolean result = userService.updateUserInfo(userUpdateDto);
        ResponseEntity<Void> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg(result ? "更新成功" : "更新失败");
        return response;
    }

    /**
     * 新增用户
     * @param registerDTO 用户信息
     * @return 响应结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @OperateLog(module = "系统用户", operation = "新增用户", saveParams = false)
    public ResponseEntity<Void> addUser(@RequestBody RegisterDTO registerDTO) {
        boolean result = userService.addUser(registerDTO);
        ResponseEntity<Void> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg(result ? "新增成功" : "新增失败");
        return response;
    }

    /**
     * 删除用户
     * @param ids 用户ID列表
     * @return 响应结果
     */
    @GetMapping
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @OperateLog(module = "系统用户", operation = "删除用户")
    public ResponseEntity<Void> deleteUser(@RequestParam("ids") List<Long> ids) {
        boolean result = userService.removeByIds(ids);
        ResponseEntity<Void> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg(result ? "删除成功" : "删除失败");
        return response;
    }



}
