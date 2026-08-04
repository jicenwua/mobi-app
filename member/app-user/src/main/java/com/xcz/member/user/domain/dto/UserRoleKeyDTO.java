package com.xcz.member.user.domain.dto;

import lombok.Data;

/**
 * 用户与角色键映射（批量权限刷新）。
 */
@Data
public class UserRoleKeyDTO {
    private Long userId;
    private String roleKey;
}
