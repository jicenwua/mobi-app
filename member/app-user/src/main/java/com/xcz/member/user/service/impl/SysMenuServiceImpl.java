package com.xcz.member.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.commons.security.utils.PermissionUtils;
import com.xcz.member.user.domain.SysMenu;
import com.xcz.member.user.domain.vo.PermissionVo;
import com.xcz.member.user.domain.vo.TreeVOParams;
import com.xcz.member.user.mapper.SysMenuMapper;
import com.xcz.member.user.service.RolePermissionCacheService;
import com.xcz.member.user.service.SysMenuService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Resource
    private SysMenuMapper sysMenuMapper;
    @Resource
    private RolePermissionCacheService rolePermissionCacheService;

    @PostConstruct
    public void initPermissionCache(){
        List<PermissionVo> permissionVos = sysMenuMapper.selectPermissions();
        Map<String, Set<String>> collect = permissionVos.stream()
                .collect(Collectors.groupingBy(
                        PermissionVo::getRoleKey,
                        Collectors.mapping(PermissionVo::getPermission, Collectors.toSet())
                ));
        PermissionUtils.setRoleCache(collect);
    }


    @Override
    public List<SysMenu> getMenusByUserId(Long userId) {
        return getBaseMapper().selectMenusByUserId(userId);
    }

    @Override
    public List<TreeVOParams> buildMenuTree(List<SysMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, List<SysMenu>> menuGroupByParent = menus.stream()
                .collect(Collectors.groupingBy(menu -> normalizeParentId(menu.getParentId())));

        menuGroupByParent.values().forEach(list ->
                list.sort(Comparator.comparingInt(m -> m.getOrderNum() == null ? 0 : m.getOrderNum())));

        return buildTreeRecursive(menuGroupByParent.getOrDefault(0L, Collections.emptyList()), menuGroupByParent);
    }

    private static Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private List<TreeVOParams> buildTreeRecursive(List<SysMenu> currentLevelMenus,
                                                   Map<Long, List<SysMenu>> menuGroupByParent) {
        List<TreeVOParams> tree = new ArrayList<>();

        for (SysMenu menu : currentLevelMenus) {
            // 获取当前菜单的子菜单（按 orderNum 排序）
            List<SysMenu> children = menuGroupByParent.getOrDefault(menu.getMenuId(), Collections.emptyList())
                    .stream()
                    .sorted(Comparator.comparingInt(SysMenu::getOrderNum))
                    .collect(Collectors.toList());

            // 构建当前节点
            TreeVOParams treeVOParams = TreeVOParams.builder()
                    .menuId(menu.getMenuId())
                    .parentId(menu.getParentId())
                    .orderNum(menu.getOrderNum())
                    .menuName(menu.getMenuName())
                    .path(menu.getPath())
                    .component(menu.getComponent())
                    .query(menu.getQuery())
                    .isFrame(menu.getIsFrame())
                    .isCache(menu.getIsCache())
                    .menuType(menu.getMenuType())
                    .perms(menu.getPerms())
                    .icon(menu.getIcon())
                    .visible(menu.getVisible())
                    .status(menu.getStatus())
                    .createBy(menu.getCreateBy())
                    .createTime(menu.getCreateTime())
                    .updateBy(menu.getUpdateBy())
                    .updateTime(menu.getUpdateTime())
                    .children(buildTreeRecursive(children, menuGroupByParent))
                    .build();

            tree.add(treeVOParams);
        }

        return tree;
    }

    @Override
    public List<TreeVOParams> getMenuTree() {
        return buildMenuTree(listAllMenusForAdmin());
    }

    @Override
    public List<SysMenu> listAllMenusForAdmin() {
        return sysMenuMapper.selectAllMenus();
    }



    public Set<String> getPermissions(Long userId) {
        List<SysMenu> sysMenus = sysMenuMapper.selectMenusByUserId(userId);
        return sysMenus.stream()
                .map(SysMenu::getPerms)
                .filter(it -> it != null && !it.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveMenu(SysMenu menu) {
        return save(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMenu(SysMenu menu) {
        SysMenu old = getById(menu.getMenuId());
        boolean updated = updateById(menu);
        if (updated && old != null && menuPermAffectsAuth(old, menu)) {
            rolePermissionCacheService.refreshRolesByMenuIds(List.of(menu.getMenuId()));
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeMenu(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return true;
        }
        List<Long> allIds = collectMenuIdsWithDescendants(menuIds);
        rolePermissionCacheService.refreshRolesByMenuIds(allIds);
        sysMenuMapper.deleteRoleMenu(allIds);
        return removeByIds(allIds);
    }

    /** 删除父菜单时一并收集全部子孙菜单 ID */
    private List<Long> collectMenuIdsWithDescendants(List<Long> menuIds) {
        List<SysMenu> allMenus = listAllMenusForAdmin();
        Map<Long, List<SysMenu>> menuGroupByParent = allMenus.stream()
                .collect(Collectors.groupingBy(menu -> normalizeParentId(menu.getParentId())));

        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (Long menuId : menuIds) {
            collectDescendantMenuIds(menuId, menuGroupByParent, result);
        }
        return new ArrayList<>(result);
    }

    private void collectDescendantMenuIds(Long menuId,
                                          Map<Long, List<SysMenu>> menuGroupByParent,
                                          Set<Long> out) {
        if (menuId == null || !out.add(menuId)) {
            return;
        }
        for (SysMenu child : menuGroupByParent.getOrDefault(menuId, Collections.emptyList())) {
            collectDescendantMenuIds(child.getMenuId(), menuGroupByParent, out);
        }
    }

    /** 权限标识、状态变化会影响鉴权 */
    private static boolean menuPermAffectsAuth(SysMenu old, SysMenu current) {
        return !Objects.equals(old.getPerms(), current.getPerms())
                || !Objects.equals(old.getStatus(), current.getStatus());
    }
}
