package com.xcz.member.customer.application.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.member.customer.application.assemblers.RoleAssembler;
import com.xcz.member.customer.domain.service.MobiRoleService;
import com.xcz.member.customer.infrastructure.entity.MobiRole;
import com.xcz.member.feign.dto.role.MobiRoleFeign;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理后台小程序权限应用服务（Feign 入口编排）。
 */
@Service
public class RoleApplicationService {

    @Resource
    private MobiRoleService mobiRoleService;

    public Page<MobiRoleFeign> pageForFeign(String roleKey, Boolean status, Integer pageNum, Integer pageSize) {
        Page<MobiRole> page = mobiRoleService.pageRoles(roleKey, status, pageNum, pageSize);
        Page<MobiRoleFeign> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(RoleAssembler::toFeign).toList());
        return result;
    }

    public MobiRoleFeign getForFeign(Long id) {
        return RoleAssembler.toFeign(mobiRoleService.getById(id));
    }

    public void add(MobiRoleFeign body) {
        mobiRoleService.addRole(RoleAssembler.toEntity(body));
    }

    public void edit(MobiRoleFeign body) {
        mobiRoleService.updateRole(RoleAssembler.toEntity(body));
    }

    public void remove(List<Long> ids) {
        mobiRoleService.removeRoles(ids);
    }

    public void reloadCache() {
        mobiRoleService.reloadCache();
    }
}
