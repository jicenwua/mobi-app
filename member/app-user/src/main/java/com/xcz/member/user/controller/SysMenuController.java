package com.xcz.member.user.controller;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.security.utils.SecurityUtils;
import com.xcz.member.user.annotation.OperateLog;
import com.xcz.member.user.domain.SysMenu;
import com.xcz.member.user.domain.vo.TreeVOParams;
import com.xcz.member.user.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 菜单控制器
 */
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    /**
     * 获取菜单树
     */
    @GetMapping("/tree")
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    public ResponseEntity<List<TreeVOParams>> getMenuTree() {
        List<TreeVOParams> menuTree = menuService.getMenuTree();
        ResponseEntity<List<TreeVOParams>> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg("查询成功");
        response.setData(menuTree);
        return response;
    }

    /**
     * 管理端菜单列表（扁平，含隐藏/停用；前端按 parentId 组树）
     */
    @GetMapping("/admin/list")
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    public ResponseEntity<List<SysMenu>> getAdminMenuList() {
        List<SysMenu> menus = menuService.listAllMenusForAdmin();
        ResponseEntity<List<SysMenu>> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg("查询成功");
        response.setData(menus);
        return response;
    }

    /**
     * 根据用户ID获取菜单树
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    public ResponseEntity<List<TreeVOParams>> getUserMenus(@PathVariable Long userId) {
        List<SysMenu> menus = menuService.getMenusByUserId(userId);
        List<TreeVOParams> menuTree = menuService.buildMenuTree(menus);
        ResponseEntity<List<TreeVOParams>> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg("查询成功");
        response.setData(menuTree);
        return response;
    }


    /**
     * 获取菜单详情
     */
    @GetMapping("/{menuId}")
    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    public ResponseEntity<SysMenu> getInfo(@PathVariable Long menuId) {
        SysMenu menu = menuService.getById(menuId);
        ResponseEntity<SysMenu> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg("查询成功");
        response.setData(menu);
        return response;
    }

    /**
     * 新增菜单
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @OperateLog(module = "系统菜单", operation = "新增菜单")
    public ResponseEntity<Void> add(@RequestBody SysMenu menu) {
        menu.setCreateBy(SecurityUtils.getUsername());
        boolean result = menuService.saveMenu(menu);
        ResponseEntity<Void> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg(result ? "新增成功" : "新增失败");
        return response;
    }

    /**
     * 修改菜单
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @OperateLog(module = "系统菜单", operation = "修改菜单")
    public ResponseEntity<Void> edit(@RequestBody SysMenu menu) {
        if(Objects.equals(menu.getMenuId(), menu.getParentId())){
            throw new IllegalArgumentException("父级目录不能为自己");
        }
        menu.setUpdateBy(SecurityUtils.getUsername());
        boolean result = menuService.updateMenu(menu);
        ResponseEntity<Void> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg(result ? "修改成功" : "修改失败");
        return response;
    }

    /**
     * 删除菜单（支持单个或逗号分隔批量，如 1 或 1,2,3）
     */
    @DeleteMapping("/{menuIds}")
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @OperateLog(module = "系统菜单", operation = "删除菜单")
    public ResponseEntity<Void> remove(@PathVariable List<Long> menuIds) {
        boolean result = menuService.removeMenu(menuIds);
        ResponseEntity<Void> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg(result ? "删除成功" : "删除失败");
        return response;
    }
}
