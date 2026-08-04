package com.xcz.member.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO {
    /***用户ID**/
    private Long userId;
    /***用户名**/
    private String userName;
    /***昵称**/
    private String nickName;
    /***邮箱**/
    private String email;
    /***手机号**/
    private String phonenumber;
    /***头像**/
    private String avatar;
    /***状态**/
    private String status;
    /***角色列表**/
    private List<String> roles;
    /***拼接角色名**/
    private String roleName;
    /***角色ID拼接**/
    private String roleIds;
    /***权限列表**/
    private List<String> permissions;
    /***菜单列表**/
    private Object menus;
    /***最后登录IP**/
    private String loginIp;
    /***访问令牌**/
    private String token;
    /***最后登录时间**/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginDate;
    /***创建时间**/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
