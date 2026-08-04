package com.xcz.member.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.security.utils.PermissionUtils;
import com.xcz.commons.security.utils.SecurityUtils;
import com.xcz.member.user.domain.SysMenu;
import com.xcz.member.user.domain.SysRole;
import com.xcz.member.user.domain.dto.RoleDTO;
import com.xcz.member.user.domain.vo.RoleMenuVO;
import com.xcz.member.user.mapper.SysMenuMapper;
import com.xcz.member.user.mapper.SysRoleMapper;
import com.xcz.member.user.service.RolePermissionCacheService;
import com.xcz.member.user.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 角色服务实现类
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final RolePermissionCacheService rolePermissionCacheService;

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        return getBaseMapper().selectRolesByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignMenus(Long roleId, List<Long> menuIds) {
        sysRoleMapper.deleteRoleMenu(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            sysRoleMapper.insertRoleMenu(roleId, menuIds);
        }
        rolePermissionCacheService.refreshRole(roleId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserRole(Long userId, List<Long> roleIds) {
        sysRoleMapper.deleteUserRole(userId);
        sysRoleMapper.insertUserRole(userId, roleIds);
    }

    @Override
    public RoleMenuVO getRoleMenus(Long roleId) {
        RoleMenuVO roleMenuVO = new RoleMenuVO();
        SysRole sysRole = sysRoleMapper.selectById(roleId);
        BeanUtils.copyProperties(sysRole, roleMenuVO);

        List<SysMenu> sysMenus = sysMenuMapper.selectMenusByRoleId(roleId);
        roleMenuVO.setMenuIds(sysMenus.stream().map(SysMenu::getMenuId).toList());
        return roleMenuVO;
    }

    @Override
    public Page<RoleMenuVO> getRoleManusList(RoleDTO roleDTO) {
        Page<SysRole> page = new Page<>(roleDTO.getPageNum(), roleDTO.getPageSize());

        return sysRoleMapper.selectRoleInfos(page, roleDTO);
    }

    @Override
    public void removeByIds(List<Long> roleIds) {
        sysRoleMapper.deleteByIds(roleIds);
        sysRoleMapper.deleteUserRoleByRoles(roleIds);
    }

    @Override
    public boolean addRole(SysRole role) {
        role.setCreateBy(String.valueOf(SecurityUtils.getUserId()));
        sysRoleMapper.insert(role);
        Map<String, Set<String>> perms = new HashMap<>();
        perms.put(role.getRoleKey(), new HashSet<>());
        PermissionUtils.setRoleCache(perms);
        return true;
    }

    @Override
    public boolean updateRole(SysRole role) {
        role.setUpdateBy(String.valueOf(SecurityUtils.getUserId()));
        role.setVersion(role.getVersion() + 1);
        SysRole sysRole = sysRoleMapper.selectById(role.getRoleId());
        if (sysRole == null) {
            throw new ServiceException("没有该角色");
        }
        sysRoleMapper.updateById(role);
        //如果修改后的权限key和之前不一致
        if (!role.getRoleKey().equals(sysRole.getRoleKey())) {
            PermissionUtils.removeRolePermission(sysRole.getRoleKey());
            rolePermissionCacheService.refreshRole(role.getRoleId(), false);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        // 逻辑删除的 UPDATE 不会返回行数据，必须在更新前先查出 roleKey 等信息
        List<SysRole> roles = listByIds(roleIds);
        if (roles.isEmpty()) {
            return;
        }
        List<String> roleKeys = roles.stream()
                .map(SysRole::getRoleKey)
                .filter(Objects::nonNull)
                .toList();
        List<Long> affectedUserIds = sysRoleMapper.selectUserIdsByRoleIds(roleIds);

        sysRoleMapper.deleteUserRoleByRoles(roleIds);
        sysRoleMapper.deleteRoles(roleIds, SecurityUtils.getUserId());

        roleKeys.forEach(PermissionUtils::removeRolePermission);
        for (Long userId : affectedUserIds) {
            List<String> remainingRoleKeys = getRolesByUserId(userId).stream()
                    .map(SysRole::getRoleKey)
                    .toList();
            PermissionUtils.addRoleChange(userId, remainingRoleKeys);
        }
    }
}
