package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.customer.domain.enums.Constants;
import com.xcz.member.customer.domain.service.MobiRoleService;
import com.xcz.member.customer.infrastructure.entity.MobiRole;
import com.xcz.member.customer.infrastructure.mapper.MobiRoleMapper;
import com.xcz.commons.security.utils.PermissionUtils;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 小程序端权限角色服务实现
 */
@Service
public class MobiRoleServiceImpl extends ServiceImpl<MobiRoleMapper, MobiRole> implements MobiRoleService {

    @Resource
    private MobiRoleMapper mobiRoleMapper;

    /**
     * 应用启动后预热小程序权限缓存与版本号。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (PermissionUtils.getVersion(Constants.USER_ROLE) == null) {
            long version = System.currentTimeMillis();
            PermissionUtils.publishRoleVersion(Constants.USER_ROLE, version);
        }
        getWxRole();
    }

    /**
     * 获取权限版本号
     *
     * @return 版本号时间戳
     */
    @Override
    public Long getVersion() {
        Long version = PermissionUtils.getVersion(Constants.USER_ROLE);
        //如果获取的权限是空，则这是权限为当前时间戳，并保存到内存中
        if (version == null) {
            version = System.currentTimeMillis();
            PermissionUtils.publishRoleVersion(Constants.USER_ROLE, version);
        }
        return version;
    }

    /**
     * 获取已启用的小程序权限 roleKey 集合
     *
     * @return 权限标识集合
     */
    @Override
    public Set<String> getWxRole() {

        Set<String> roleKeys =  PermissionUtils.getPermission(Constants.USER_ROLE);
        //如果没有获取到权限则从数据库中获取并保存到缓存中
        if (roleKeys == null || roleKeys.isEmpty()) {
            List<MobiRole> mobiRoles = mobiRoleMapper.selectList(
                    new LambdaQueryWrapper<MobiRole>()
                            .eq(MobiRole::getStatus, true)
            );
            roleKeys = mobiRoles.stream()
                    .map(MobiRole::getRoleKey)
                    .collect(Collectors.toSet());
            PermissionUtils.setRoleCache(Map.of(Constants.USER_ROLE,roleKeys));
        }
        return roleKeys;
    }

    /**
     * 清除并重新加载权限角色缓存
     */
    @Override
    public void reloadCache() {
        PermissionUtils.removeRolePermission(Constants.USER_ROLE);
        long version = System.currentTimeMillis();
        PermissionUtils.publishRoleVersion(Constants.USER_ROLE, version);
        getWxRole();
    }

    /**
     * 分页查询权限角色
     *
     * @param roleKey  权限标识关键字（可选）
     * @param status   启用状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 角色分页结果
     */
    @Override
    public Page<MobiRole> pageRoles(String roleKey, Boolean status, int pageNum, int pageSize) {
        Page<MobiRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MobiRole> q = new LambdaQueryWrapper<>();
        q.like(StringUtils.isNotEmpty(roleKey), MobiRole::getRoleKey, roleKey);
        q.eq(status != null, MobiRole::getStatus, status);
        q.orderByDesc(MobiRole::getCreateTime);
        return mobiRoleMapper.selectPage(page, q);
    }

    /**
     * 新增权限角色并刷新缓存
     *
     * @param role 角色实体
     * @return 是否新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addRole(MobiRole role) {
        validateRole(role, true);
        role.setId(null);
        role.setCreateTime(LocalDateTime.now());
        role.setCreateBy(SecurityUtils.getUserId());
        role.setUpdateTime(LocalDateTime.now());
        role.setUpdateBy(SecurityUtils.getUserId());
        if (role.getStatus() == null) {
            role.setStatus(true);
        }
        boolean ok = save(role);
        if (ok) {
            reloadCache();
        }
        return ok;
    }

    /**
     * 修改权限角色并刷新缓存
     *
     * @param role 角色实体（须含 id）
     * @return 是否修改成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(MobiRole role) {
        if (role == null || role.getId() == null) {
            throw new ServiceException("权限 ID 不能为空", 400);
        }
        validateRole(role, false);
        role.setUpdateTime(LocalDateTime.now());
        role.setUpdateBy(SecurityUtils.getUserId());
        boolean ok = updateById(role);
        if (ok) {
            reloadCache();
        }
        return ok;
    }

    /**
     * 批量删除权限角色并刷新缓存
     *
     * @param ids 角色 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRoles(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        removeByIds(ids);
        reloadCache();
    }

    private void validateRole(MobiRole role, boolean isNew) {
        if (role == null) {
            throw new ServiceException("权限数据不能为空", 400);
        }
        String key = StringUtils.trim(role.getRoleKey());
        if (StringUtils.isEmpty(key)) {
            throw new ServiceException("权限标识不能为空", 400);
        }
        if (key.length() > 30) {
            throw new ServiceException("权限标识长度不能超过 30", 400);
        }
        if (!key.startsWith("wx:")) {
            throw new ServiceException("小程序权限标识须以 wx: 开头", 400);
        }
        role.setRoleKey(key);
        LambdaQueryWrapper<MobiRole> dup = new LambdaQueryWrapper<MobiRole>()
                .eq(MobiRole::getRoleKey, key);
        if (!isNew && role.getId() != null) {
            dup.ne(MobiRole::getId, role.getId());
        }
        if (mobiRoleMapper.selectCount(dup) > 0) {
            throw new ServiceException("权限标识已存在", 400);
        }
    }
}
