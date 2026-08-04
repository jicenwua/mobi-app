package com.xcz.member.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xcz.commons.core.exception.user.UserPasswordNotMatchException;
import com.xcz.commons.core.utils.ServletUtils;
import com.xcz.commons.core.utils.ip.IpUtils;
import com.xcz.commons.core.utils.ip.Ipv6Utils;
import com.xcz.commons.security.extend.LoginUser;
import com.xcz.commons.security.service.TokenService;
import com.xcz.member.user.domain.SysUser;
import com.xcz.member.user.domain.dto.LoginDTO;
import com.xcz.member.user.service.SysLogService;
import com.xcz.member.user.service.SysLoginService;
import com.xcz.member.user.service.SysRoleService;
import com.xcz.member.user.service.SysUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class SysLoginServiceImpl implements SysLoginService {

    @Resource
    private AuthenticationManager authenticationManager;
    @Resource
    private TokenService tokenService;
    @Resource
    private SysUserService sysUserService;
    @Resource
    private SysLogService sysLogService;



    @Override
    public String login(LoginDTO loginDTO) throws IllegalAccessException {
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();
        String ip = Ipv6Utils.getClientIp(ServletUtils.getRequest());
        // 用户验证
        Authentication authentication = null;
        try {
            if (username == null || username.isEmpty()) {
                throw new IllegalArgumentException("请输入账号");
            }
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("请输入密码");
            }
            // 该方法会去调用 UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(username, password));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            loginUser.setIpaddr(ip);
            loginUser.setLoginLocation(IpUtils.getIpLocation(ip));
            String token = tokenService.createToken(loginUser);
            CompletableFuture.runAsync(() -> {
                try {
                    sysUserService.update(
                            new LambdaUpdateWrapper<SysUser>()
                                    .eq(SysUser::getUserId, loginUser.getUserId())
                                    .set(SysUser::getLoginIp, loginUser.getIpaddr())
                                    .set(SysUser::getLoginDate, LocalDateTime.now())
                    );
                    sysLogService.saveLoginLog(0, "登录成功", loginUser);
                } catch (Exception e) {
                    log.error("异步保存登录日志或更新用户IP失败，用户ID: {}", loginUser.getUserId(), e);
                }
            });
            return token;
        } catch (Exception e) {

            LoginUser build = LoginUser.builder()
                    .username(username)
                    .loginLocation(IpUtils.getIpLocation(ip))
                    .ipaddr(ip)
                    .build();
            if (e instanceof UserPasswordNotMatchException) {
                sysLogService.saveLoginLog(1, "账号或者密码错误", build);
                throw new UserPasswordNotMatchException();
            }
            sysLogService.saveLoginLog(1, e.getMessage(), build);
            throw e;
        }
    }

    @Override
    public boolean logout() {
        LoginUser principal = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(principal == null){
            return true;
        }
        boolean logout = tokenService.logout(principal.getToken());
        if(logout){
            SecurityContextHolder.clearContext();
        }
        return logout;
    }




}
