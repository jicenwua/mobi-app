package com.xcz.member.user.service.impl;

import com.xcz.commons.core.utils.StringUtils;
import com.xcz.commons.security.utils.PermissionUtils;
import com.xcz.member.user.domain.SysMenu;
import com.xcz.member.user.domain.dto.UserRoleKeyDTO;
import com.xcz.member.user.domain.SysRole;
import com.xcz.member.user.mapper.SysMenuMapper;
import com.xcz.member.user.mapper.SysRoleMapper;
import com.xcz.member.user.service.RolePermissionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionCacheServiceImpl implements RolePermissionCacheService {

    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public void refreshRole(Long roleId) {
        refreshRole(roleId, true);
    }

    @Override
    public void refreshRole(Long roleId, boolean incrementVersion) {
        if (roleId == null) {
            return;
        }
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null || StringUtils.isEmpty(role.getRoleKey())) {
            return;
        }

        Set<String> perms = sysMenuMapper.selectMenusByRoleId(roleId).stream()
                .map(SysMenu::getPerms)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
        PermissionUtils.getRoles().put(role.getRoleKey(), perms);

        long publishVersion;
        if (incrementVersion) {
            publishVersion = (role.getVersion() == null ? 0 : role.getVersion()) + 1;
            SysRole versionUpdate = new SysRole();
            versionUpdate.setRoleId(roleId);
            versionUpdate.setVersion(publishVersion);
            sysRoleMapper.updateById(versionUpdate);
        } else {
            publishVersion = role.getVersion() == null ? 0 : role.getVersion();
        }
        PermissionUtils.publishRoleVersion(role.getRoleKey(), publishVersion);
        notifyUsersForRole(roleId);
    }

    @Override
    public void refreshRolesByMenuIds(List<Long> menuIds) {
        if (CollectionUtils.isEmpty(menuIds)) {
            return;
        }
        List<SysRole> roles = sysRoleMapper.selectRolesByMenuIds(menuIds);
        roles.stream()
                .map(SysRole::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(this::refreshRole);
    }

    private void notifyUsersForRole(Long roleId) {
        List<Long> userIds = sysRoleMapper.selectUserIdsByRoleIds(Collections.singletonList(roleId));
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        Map<Long, List<String>> roleKeysByUser = sysRoleMapper.selectRoleKeysByUserIds(userIds).stream()
                .filter(row -> row.getUserId() != null && StringUtils.isNotEmpty(row.getRoleKey()))
                .collect(Collectors.groupingBy(
                        UserRoleKeyDTO::getUserId,
                        Collectors.mapping(UserRoleKeyDTO::getRoleKey, Collectors.toList())));
        for (Long userId : userIds) {
            PermissionUtils.addRoleChange(userId, roleKeysByUser.getOrDefault(userId, List.of()));
        }
    }
}
