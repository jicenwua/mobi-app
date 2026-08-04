package com.xcz.member.user.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户和角色关联表 sys_user_role
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user_role")
public class SysUserRole {
    private static final long serialVersionUID = 1L;

    /***用户ID**/
    private Long userId;
    /***角色ID**/
    private Long roleId;
}
