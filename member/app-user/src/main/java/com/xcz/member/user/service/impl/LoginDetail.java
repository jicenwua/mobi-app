package com.xcz.member.user.service.impl;

import com.xcz.commons.security.extend.LoginUser;
import com.xcz.commons.security.utils.PermissionUtils;
import com.xcz.member.user.domain.SysRole;
import com.xcz.member.user.domain.SysUser;
import com.xcz.member.user.service.SysMenuService;
import com.xcz.member.user.service.SysRoleService;
import com.xcz.member.user.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LoginDetail implements UserDetailsService {
    @Resource
    private SysUserService sysUserService;
    @Resource
    private SysRoleService roleService;
    @Resource
    private SysMenuService menuService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //查询用户信息
        SysUser user = sysUserService.getUserByUserName(username);
        if(user == null){
            throw new UsernameNotFoundException("用户不存在");
        }

        //检查账号状态
        if("1".equals(user.getStatus())){
            throw new UsernameNotFoundException("账号已停用");
        }

        //查询用户角色（转换为角色标识集合）
        List<SysRole> roles = roleService.getRolesByUserId(user.getUserId());
        Set<String> roleKeys = roles.stream()
                .map(SysRole::getRoleKey)
                .collect(Collectors.toSet());

        Map<String, Long> version = roles.stream()
                .collect(Collectors.toMap(SysRole::getRoleKey, SysRole::getVersion));

        //从缓存中获取角色权限
        Map<String, Set<String>> map = PermissionUtils.getRoles();
        Map<String, Set<String>> permissions = new HashMap<>();
        roleKeys.forEach(it -> permissions.put(it, map.get(it)));


        //创建 LoginUser 对象（包含密码，由 Spring Security 自动验证）
        return new LoginUser(
                user.getUserId(),           // userId
                null,                   // token (登录后生成)
                null,                   // ipaddr (登录时记录)
                null,                   // loginLocation (登录时记录)
                permissions,            // permissions
                user.getUserName(),     // username
                user.getNickName(),     // name
                user.getAvatar(),       // avatar
                version,
                user.getPassword(),     // password (由 DaoAuthenticationProvider 验证)
                "0".equals(user.getStatus()),  // enabled: 0=正常, 1=停用
                true,                   // accountNonLocked
                true,                   // credentialsNonExpired
                true                    // accountNonExpired
        );
    }
}
