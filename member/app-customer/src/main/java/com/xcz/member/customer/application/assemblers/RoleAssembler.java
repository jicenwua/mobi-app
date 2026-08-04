package com.xcz.member.customer.application.assemblers;

import com.xcz.member.customer.infrastructure.entity.MobiRole;
import com.xcz.member.feign.dto.role.MobiRoleFeign;

/**
 * 小程序权限对象组装器（管理端 Feign）。
 */
public final class RoleAssembler {

    private RoleAssembler() {
    }

    public static MobiRoleFeign toFeign(MobiRole role) {
        if (role == null) {
            return null;
        }
        return MobiRoleFeign.builder()
                .id(role.getId())
                .roleKey(role.getRoleKey())
                .roleDescription(role.getRoleDescription())
                .status(role.getStatus())
                .createTime(role.getCreateTime())
                .createBy(role.getCreateBy())
                .updateTime(role.getUpdateTime())
                .updateBy(role.getUpdateBy())
                .build();
    }

    public static MobiRole toEntity(MobiRoleFeign feign) {
        if (feign == null) {
            return null;
        }
        return MobiRole.builder()
                .id(feign.getId())
                .roleKey(feign.getRoleKey())
                .roleDescription(feign.getRoleDescription())
                .status(feign.getStatus())
                .build();
    }
}
