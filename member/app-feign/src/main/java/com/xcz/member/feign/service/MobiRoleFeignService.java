package com.xcz.member.feign.service;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.role.MobiRoleFeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小程序权限 mobi_role Feign 客户端
 */
@FeignClient(
        value = "app-miniApp",
        path = "/mobi/role",
        contextId = "MobiRoleFeignService"
)
public interface MobiRoleFeignService {

    @GetMapping("/list")
    ResponseEntity<List<MobiRoleFeign>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleKey,
            @RequestParam(required = false) Boolean status);

    @GetMapping("/{id}")
    ResponseEntity<MobiRoleFeign> get(@PathVariable("id") Long id);

    @PostMapping
    ResponseEntity<Void> add(@RequestBody MobiRoleFeign body);

    @PutMapping
    ResponseEntity<Void> edit(@RequestBody MobiRoleFeign body);

    @DeleteMapping("/{ids}")
    ResponseEntity<Void> remove(@PathVariable("ids") List<Long> ids);

    @PostMapping("/reload-cache")
    ResponseEntity<Void> reloadCache();
}
