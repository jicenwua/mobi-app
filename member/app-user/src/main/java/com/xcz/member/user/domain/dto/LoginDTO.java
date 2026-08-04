package com.xcz.member.user.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {
    /***用户名**/
    private String username;
    /***密码**/
    private String password;
    /***验证码**/
    private String code;
    /***验证码唯一标识**/
    private String uuid;
}
