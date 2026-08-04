package com.xcz.member.customer.infrastructure.adapter.auth;

import com.xcz.commons.core.utils.ip.IpUtils;
import com.xcz.member.customer.domain.enums.Constants;
import com.xcz.member.customer.domain.service.MobiRoleService;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import com.xcz.commons.security.extend.LoginUser;
import com.xcz.commons.security.service.TokenService;
import jakarta.annotation.Resource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 登录 token 签发与刷新适配器。
 */
@Component
public class AuthTokenIssuer {

    @Resource
    private TokenService tokenService;
    @Resource
    private MobiRoleService mobiRoleService;

    /**
     * 根据用户实体签发 token 并写入安全上下文。
     *
     * @param user 小程序用户实体
     * @return 已填充 token 的登录用户
     */
    public LoginUser issue(MobiUser user) {
        Long version = mobiRoleService.getVersion();
        Set<String> wxRole = mobiRoleService.getWxRole();

        Map<String, Set<String>> permissions = new HashMap<>();
        permissions.put(Constants.USER_ROLE, new HashSet<>(wxRole));
        Map<String, Long> roleVersion = new HashMap<>();
        roleVersion.put(Constants.USER_ROLE, version);

        LoginUser loginUser = LoginUser.builder()
                .userId(user.getUserId())
                .username(user.getOpenid())
                .name(user.getNickname())
                .avatar(user.getAvatarUrl())
                .ipaddr(user.getLoginIp())
                .loginLocation(IpUtils.getIpLocation(user.getLoginIp()))
                .permissions(permissions)
                .version(roleVersion)
                .password(user.getPassword())
                .enabled(user.getStatus() == 1)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        tokenService.createToken(loginUser);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        return loginUser;
    }

    /**
     * 刷新已登录用户的 token 与缓存信息（昵称、头像、密码等变更后调用）。
     *
     * @param loginUser 当前登录用户
     */
    public void refreshLoginInfo(LoginUser loginUser) {
        tokenService.updateLoginUser(loginUser);
    }
}
