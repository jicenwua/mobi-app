package com.xcz.member.notify.websocket;

import com.xcz.commons.core.constant.SecurityConstants;
import com.xcz.commons.security.extend.LoginUser;
import lombok.experimental.UtilityClass;

import java.util.Set;

/**
 * 根据 {@link LoginUser} 权限推断 WebSocket 客户端类型，用于握手时校验 client 参数。
 */
@UtilityClass
public class NotifyClientTypeResolver {

    /**
     * 根据登录用户权限推断其应使用的 WebSocket 客户端类型。
     *
     * @param loginUser 已鉴权用户
     * @return STAFF / CUSTOMER；无法判定时返回 null
     */
    public static NotifyClientType resolveExpectedClientType(LoginUser loginUser) {
        if (loginUser == null || loginUser.getPermissions() == null) {
            return null;
        }
        Set<String> permissions = loginUser.getPermissionSet();
        boolean hasStaffTicketPerm = permissions.stream()
                .anyMatch(perm -> perm.startsWith("system:ticket:"));
        if (hasStaffTicketPerm) {
            return NotifyClientType.STAFF;
        }
        Set<String> roles = loginUser.getRoleSet();
        if (roles.contains(SecurityConstants.WX_APP_ROLE)) {
            return NotifyClientType.CUSTOMER;
        }
        return null;
    }
}
