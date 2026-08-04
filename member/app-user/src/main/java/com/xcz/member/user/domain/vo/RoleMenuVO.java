package com.xcz.member.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.xcz.member.user.domain.BaseEntity;
import lombok.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoleMenuVO extends BaseEntity {
    /***角色ID**/
    private Long roleId;
    /***角色名称**/
    private String roleName;
    /***角色权限字符串**/
    private String roleKey;
    /***角色状态（0正常 1停用）**/
    private String status;
    /***显示顺序**/
    private Integer roleSort;
    /***备注**/
    private String remark;
    /***菜单ID列表**/
    private List<Long> menuIds;

    @JsonIgnore
    private String menuIdsStr;

    public void setMenuIdsStr(String menuIdsStr){
        this.menuIdsStr = menuIdsStr;
        this.menuIds = Stream.of(menuIdsStr.split(",")).map(Long::parseLong).collect(Collectors.toList());
    }
}
