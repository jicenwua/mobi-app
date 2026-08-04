package com.xcz.member.user.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 角色信息表 sys_role
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /***主键ID**/
    @TableId(type = IdType.AUTO)
    private Long roleId;
    /***角色名称**/
    private String roleName;
    /***角色权限字符串**/
    private String roleKey;
    /***显示顺序**/
    private Integer roleSort;
    /***角色状态（0正常 1停用）**/
    private String status;
    /***角色版本**/
    private Long version;
    /***删除标志（0代表存在 2代表删除）**/
    private Integer delFlag;
}
