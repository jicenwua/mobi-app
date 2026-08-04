package com.xcz.member.customer.domain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.customer.infrastructure.entity.MobiRole;

import java.util.List;
import java.util.Set;

/**
 * 小程序权限角色服务（mobi_role）
 */
public interface MobiRoleService extends IService<MobiRole> {

    /**
     * 获取权限版本号（登录 token 失效判断）
     *
     * @return 版本号时间戳
     */
    Long getVersion();

    /**
     * 获取已启用的小程序权限 roleKey 集合
     *
     * @return 权限标识集合
     */
    Set<String> getWxRole();

    /**
     * 重新加载权限缓存并 bump 版本号
     */
    void reloadCache();

    /**
     * 分页查询权限角色
     *
     * @param roleKey  权限标识关键字（可选）
     * @param status   启用状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 角色分页结果
     */
    Page<MobiRole> pageRoles(String roleKey, Boolean status, int pageNum, int pageSize);

    /**
     * 新增权限角色
     *
     * @param role 角色实体
     * @return 是否新增成功
     */
    boolean addRole(MobiRole role);

    /**
     * 修改权限角色
     *
     * @param role 角色实体（须含 id）
     * @return 是否修改成功
     */
    boolean updateRole(MobiRole role);

    /**
     * 批量删除权限角色
     *
     * @param ids 角色 ID 列表
     */
    void removeRoles(List<Long> ids);
}
