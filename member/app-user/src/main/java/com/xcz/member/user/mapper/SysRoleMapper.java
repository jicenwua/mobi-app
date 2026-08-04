package com.xcz.member.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.member.user.domain.SysRole;
import com.xcz.member.user.domain.dto.RoleDTO;
import com.xcz.member.user.domain.dto.UserRoleKeyDTO;
import com.xcz.member.user.domain.vo.RoleMenuVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色信息 Mapper 接口
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户ID查询角色列表
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    void deleteUserRole(Long userId);

    void deleteUserRoleByRoles(@Param("roleIds") List<Long> roleIds);

    void insertUserRole(Long userId,@Param("roleIds") List<Long> roleIds);

    void deleteRoleMenu(Long roleId);

    void insertRoleMenu(Long roleId, List<Long> menuIds);

    Page<RoleMenuVO> selectRoleInfos(Page<SysRole> page, @Param("roleDto")RoleDTO roleDTO);

    void deleteRoles(@Param("roleIds") List<Long> roleIds, @Param("userId") Long userId);

    /**
     * 查询拥有指定角色的用户 ID（删除前调用，用于刷新在线会话）
     */
    List<Long> selectUserIdsByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 查询绑定了指定菜单的角色（菜单变更/删除前用于刷新缓存）
     */
    List<SysRole> selectRolesByMenuIds(@Param("menuIds") List<Long> menuIds);

    /**
     * 批量查询用户角色键，避免权限刷新 N+1。
     */
    List<UserRoleKeyDTO> selectRoleKeysByUserIds(@Param("userIds") List<Long> userIds);
}
