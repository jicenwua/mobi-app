package com.xcz.member.user.service;

import java.util.List;

/**
 * 角色权限 Redis 缓存与在线用户会话同步
 */
public interface RolePermissionCacheService {

    /**
     * 按角色 ID 从库中重建权限集、写入 Redis、递增版本并通知在线用户
     */
    void refreshRole(Long roleId);

    /**
     * 同步 Redis 权限集并通知在线用户；{@code incrementVersion=false} 时沿用库中已有版本（如 updateRole 已自增）
     */
    void refreshRole(Long roleId, boolean incrementVersion);

    /**
     * 刷新绑定了指定菜单的所有角色
     */
    void refreshRolesByMenuIds(List<Long> menuIds);
}
