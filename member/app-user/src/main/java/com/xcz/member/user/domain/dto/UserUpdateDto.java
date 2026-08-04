package com.xcz.member.user.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDto {
    /***用户ID**/
    private Long userId;
    /***昵称**/
    private String nickname;
    /***密码**/
    private String password;
    /***手机号**/
    private String phonenumber;
    /***邮箱**/
    private String email;
    /***性别**/
    private String sex;
    /***帐号状态**/
    private String status;
    /***角色ID列表**/
    private List<Long> roleIds;
    /***备注**/
    private String remark;
}
