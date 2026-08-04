package com.xcz.member.feign.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小程序用户 mobi_user Feign DTO（不含支付密码）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobiUserFeign implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String openid;
    private String unionid;
    private String phone;
    private String nickname;
    private String avatarUrl;
    /** 状态：1-正常 0-禁用 */
    private Integer status;
    /** 是否已设置支付密码 */
    private Boolean hasPassword;
    private String loginIp;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
