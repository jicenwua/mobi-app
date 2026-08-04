package com.xcz.member.user.controller;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.role.MobiRoleFeign;
import com.xcz.member.feign.service.MobiRoleFeignService;
import com.xcz.member.user.service.admin.MobiRoleAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台：小程序权限配置（mobi_role），代理 app-customer。
 */
@RestController
@RequestMapping("/system/mobi-role")
@RequiredArgsConstructor
public class SysMobiRoleController {

    private final MobiRoleFeignService mobiRoleFeignService;
    private final MobiRoleAdminService mobiRoleAdminService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:mobi:role:list')")
    public ResponseEntity<List<MobiRoleFeign>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleKey,
            @RequestParam(required = false) Boolean status) {
        return mobiRoleFeignService.list(pageNum, pageSize, roleKey, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:mobi:role:query')")
    public ResponseEntity<MobiRoleFeign> get(@PathVariable Long id) {
        return mobiRoleFeignService.get(id);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:mobi:role:add')")
    public ResponseEntity<Void> add(@RequestBody MobiRoleFeign body) {
        return mobiRoleAdminService.add(body);
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:mobi:role:edit')")
    public ResponseEntity<Void> edit(@RequestBody MobiRoleFeign body) {
        return mobiRoleAdminService.edit(body);
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPermi('system:mobi:role:remove')")
    public ResponseEntity<Void> remove(@PathVariable List<Long> ids) {
        return mobiRoleAdminService.remove(ids);
    }

    @PostMapping("/reload-cache")
    @PreAuthorize("@ss.hasPermi('system:mobi:role:edit')")
    public ResponseEntity<Void> reloadCache() {
        return mobiRoleAdminService.reloadCache();
    }
}
