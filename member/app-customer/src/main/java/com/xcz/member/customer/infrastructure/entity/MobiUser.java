package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息表 mobi_user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("mobi_user")
public class MobiUser implements Serializable {
    private static final long serialVersionUID = 1L;

    /***内部全局唯一ID**/
    @TableId(type = IdType.AUTO)
    private Long userId;
    /***小程序OpenID**/
    private String openid;
    /***微信开放平台ID**/
    private String unionid;
    /***绑定手机号**/
    private String phone;
    /***支付密码**/
    private String password;
    /***昵称**/
    private String nickname;
    /***OSS头像地址**/
    private String avatarUrl;
    /***状态：1-正常 0-禁用**/
    private Integer status;
    /***登录IP**/
    private String loginIp;
    /***最后登录时间**/
    private LocalDateTime lastLoginTime;
    /***创建时间**/
    private LocalDateTime createTime;
    /***更新时间**/
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
