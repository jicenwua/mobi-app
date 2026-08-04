package com.xcz.member.user.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用户信息表 sys_user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /***主键ID**/
    @TableId(type = IdType.AUTO)
    private Long userId;
    /***用户账号**/
    private String userName;
    /***用户昵称**/
    private String nickName;
    /***用户邮箱**/
    private String email;
    /***微信OpenID**/
    private String openId;
    /***手机号码**/
    private String phonenumber;
    /***用户性别（0男 1女 2未知）**/
    private String sex;
    /***头像地址**/
    private String avatar;
    /***密码**/
    private String password;
    /***帐号状态（0正常 1停用）**/
    private String status;
    /***最后登录IP**/
    private String loginIp;
    /***最后登录时间**/
    private LocalDateTime loginDate;
}
