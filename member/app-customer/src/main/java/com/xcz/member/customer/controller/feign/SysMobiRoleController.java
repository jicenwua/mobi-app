package com.xcz.member.customer.controller.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.application.service.RoleApplicationService;
import com.xcz.member.feign.dto.role.MobiRoleFeign;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台小程序权限（Feign 入口）
 */
@RestController
@RequestMapping("/mobi/role")
public class SysMobiRoleController {

    @Resource
    private RoleApplicationService roleApplicationService;

    @GetMapping("/list")
    public ResponseEntity<List<MobiRoleFeign>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleKey,
            @RequestParam(required = false) Boolean status) {
        Page<MobiRoleFeign> page = roleApplicationService.pageForFeign(roleKey, status, pageNum, pageSize);
        return ResponseEntityUtils.okPage(page.getRecords(), page.getTotal(), "查询成功");
    }

    @GetMapping("/{id}")
    public ResponseEntity<MobiRoleFeign> get(@PathVariable("id") Long id) {
        return ResponseEntityUtils.ok(roleApplicationService.getForFeign(id), "查询成功");
    }

    @PostMapping
    public ResponseEntity<Void> add(@RequestBody MobiRoleFeign body) {
        roleApplicationService.add(body);
        return ResponseEntityUtils.ok(null, "新增成功");
    }

    @PutMapping
    public ResponseEntity<Void> edit(@RequestBody MobiRoleFeign body) {
        roleApplicationService.edit(body);
        return ResponseEntityUtils.ok(null, "修改成功");
    }

    @DeleteMapping("/{ids}")
    public ResponseEntity<Void> remove(@PathVariable("ids") List<Long> ids) {
        roleApplicationService.remove(ids);
        return ResponseEntityUtils.ok(null, "删除成功");
    }

    @PostMapping("/reload-cache")
    public ResponseEntity<Void> reloadCache() {
        roleApplicationService.reloadCache();
        return ResponseEntityUtils.ok(null, "缓存已刷新");
    }
}
