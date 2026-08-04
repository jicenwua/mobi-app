package com.xcz.member.user.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {
    /***角色名称**/
    private String roleName;
    /***角色权限字符串**/
    private String roleKey;
    /***角色状态（0正常 1停用）**/
    private String status;
    /***页码**/
    private int pageNum;
    /***每页条数**/
    private int pageSize;
}
