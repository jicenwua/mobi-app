package com.xcz.member.user.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色和菜单关联表 sys_role_menu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role_menu")
public class SysRoleMenu {
    private static final long serialVersionUID = 1L;

    /***角色ID**/
    private Long roleId;
    /***菜单ID**/
    private Long menuId;
}
