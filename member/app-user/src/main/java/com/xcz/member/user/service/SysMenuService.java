package com.xcz.member.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.user.domain.SysMenu;
import com.xcz.member.user.domain.vo.TreeVOParams;

import java.util.List;
import java.util.Set;

/**
 * 菜单服务接口
 */
public interface SysMenuService extends IService<SysMenu> {

    /**
     * 根据用户ID查询菜单列表
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<SysMenu> getMenusByUserId(Long userId);

    /**
     * 构建菜单树
     * @param menus 菜单列表
     * @return 菜单树
     */
    List<TreeVOParams> buildMenuTree(List<SysMenu> menus);

    /**
     * 获取菜单树
     * @return 菜单树
     */
    List<TreeVOParams> getMenuTree();

    /**
     * 管理端：全部菜单（含隐藏/停用）
     */
    List<SysMenu> listAllMenusForAdmin();

    /**
     * 获取用户菜单权限表示
     * @param userId 用户id
     * @return  权限标识
     */
    Set<String> getPermissions(Long userId);

    /**
     * 新增菜单
     */
    boolean saveMenu(SysMenu menu);

    /**
     * 修改菜单（权限标识等变更时同步角色权限缓存）
     */
    boolean updateMenu(SysMenu menu);

    /**
     * 删除菜单
     * @param menuIds 删除的菜单ID
     * @return  是否成功
     */
    boolean removeMenu(List<Long> menuIds);
}
