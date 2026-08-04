package com.xcz.member.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.security.utils.SecurityUtils;
import com.xcz.member.user.annotation.OperateLog;
import com.xcz.member.user.domain.SysRole;
import com.xcz.member.user.domain.dto.RoleDTO;
import com.xcz.member.user.domain.vo.RoleMenuVO;
import com.xcz.member.user.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色控制器
 */
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    /**
     * 查询角色列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    public ResponseEntity<List<RoleMenuVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleKey,
            @RequestParam(required = false) String status) {
        RoleDTO roleDTO = RoleDTO.builder()
                .roleKey(roleKey)
                .roleName(roleName)
                .status(status)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        Page<RoleMenuVO> result = roleService.getRoleManusList(roleDTO);
        ResponseEntity<List<RoleMenuVO>> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg("查询成功");
        response.setData(result.getRecords());
        response.setTotal(result.getTotal());
        return response;
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{roleId}")
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    public ResponseEntity<RoleMenuVO> getInfo(@PathVariable Long roleId) {
        RoleMenuVO roleMenus = roleService.getRoleMenus(roleId);
        ResponseEntity<RoleMenuVO> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg("查询成功");
        response.setData(roleMenus);
        return response;
    }

    /**
     * 新增角色
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:role:add')")
    @OperateLog(module = "系统角色", operation = "新增角色")
    public ResponseEntity<Void> add(@RequestBody SysRole role) {
        boolean result = roleService.addRole(role);
        ResponseEntity<Void> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg(result ? "新增成功" : "新增失败");
        return response;
    }

    /**
     * 修改角色
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @OperateLog(module = "系统角色", operation = "修改角色")
    public ResponseEntity<Void> edit(@RequestBody SysRole role) {
        boolean result = roleService.updateRole(role);
        ResponseEntity<Void> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg(result ? "修改成功" : "修改失败");
        return response;
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{roleIds}")
    @PreAuthorize("@ss.hasPermi('system:role:remove')")
    @OperateLog(module = "系统角色", operation = "删除角色")
    public ResponseEntity<Void> remove(@PathVariable List<Long> roleIds) {
        roleService.removeRoles(roleIds);
        ResponseEntity<Void> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg( "删除成功");
        return response;
    }

    /**
     * 分配菜单权限
     */
    @PutMapping("/menus")
    @PreAuthorize("@ss.hasPermi('system:role:assign')")
    @OperateLog(module = "系统角色", operation = "分配菜单权限")
    public ResponseEntity<Void> assignMenus(
            @RequestParam Long roleId,
            @RequestBody List<Long> menuIds) {
        boolean result = roleService.assignMenus(roleId, menuIds);
        ResponseEntity<Void> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg(result ? "分配成功" : "分配失败");
        return response;
    }
}
