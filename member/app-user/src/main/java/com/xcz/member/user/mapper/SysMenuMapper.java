package com.xcz.member.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcz.member.user.domain.SysMenu;
import com.xcz.member.user.domain.vo.PermissionVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单权限 Mapper 接口
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据用户ID查询菜单列表
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询菜单列表
     * @param roleId 角色ID
     * @return 菜单列表
     */
    List<SysMenu> selectMenusByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除角色菜单关联表关联
     * @param menuIds   菜单id
     * @return 是否成功
     */
    int deleteRoleMenu(List<Long> menuIds);

    List<PermissionVo> selectPermissions();

    /** 管理端：查询全部菜单（含隐藏/停用，不含逻辑删除） */
    List<SysMenu> selectAllMenus();
}
