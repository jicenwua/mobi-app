package com.xcz.member.user.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户注册DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO {
    /***用户名**/
    private String userName;
    /***昵称**/
    private String nickName;
    /***密码**/
    private String password;
    /***性别**/
    private String sex;
    /***邮箱**/
    private String email;
    /***手机号**/
    private String phonenumber;
    /***角色ID列表**/
    private List<Long> roleIds;
}
