package com.xcz.member.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.user.domain.SysRole;
import com.xcz.member.user.domain.dto.RoleDTO;
import com.xcz.member.user.domain.vo.RoleMenuVO;

import java.util.List;
import java.util.Map;

/**
 * 角色服务接口
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 根据用户ID查询角色列表
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> getRolesByUserId(Long userId);

    /**
     * 分配菜单权限
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 是否成功
     */
    boolean assignMenus(Long roleId, List<Long> menuIds);

    /**
     * 更新用户分配角色
     * @param userId    用户id
     * @param roleIds   分配角色id
     */
    void updateUserRole(Long userId, List<Long> roleIds);


    /**
     * 获取角色信息包括关联菜单信息
     * @param roleId    角色id
     * @return  角色信息
     */
    RoleMenuVO getRoleMenus(Long roleId);

    /**
     * 获取角色列表
     * @param roleDTo   角色参数
     * @return  角色列表
     */
   Page<RoleMenuVO> getRoleManusList(RoleDTO roleDTo);

   void removeByIds(List<Long> roleIds);

    boolean addRole(SysRole role);

    boolean updateRole(SysRole role);

    void removeRoles(List<Long> roleIds);
}
