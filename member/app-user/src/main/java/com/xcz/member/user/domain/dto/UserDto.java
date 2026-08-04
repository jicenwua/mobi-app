package com.xcz.member.user.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    /***用户名**/
    private String username;
    /***昵称**/
    private String nickname;
    /***手机号**/
    private String phonenumber;
    /***微信OpenID**/
    private String openId;
    /***帐号状态**/
    private String status;
    /***最小登录时间（查询条件）**/
    private String minLoginTime;
    /***最大登录时间（查询条件）**/
    private String maxLoginTime;
    /***最小创建时间（查询条件）**/
    private String minCreateTime;
    /***最大创建时间（查询条件）**/
    private String maxCreateTime;
    /***按登录时间排序**/
    private Boolean orderByLoginTime;
    /***按创建时间排序**/
    private Boolean orderByCreateTime;
    /***页码**/
    @Builder.Default
    private int pageNum = 1;
    /***每页条数**/
    @Builder.Default
    private int pageSize = 10;
}
