package com.xcz.member.customer.application.assemblers;

import com.xcz.member.customer.infrastructure.entity.MobiUser;
import com.xcz.member.feign.dto.user.MobiUserFeign;

import java.util.function.Function;

/**
 * 小程序用户对象组装器（管理端 Feign）。
 */
public final class UserAssembler {

    private UserAssembler() {
    }

    public static MobiUserFeign toFeign(MobiUser user, Function<String, String> avatarUrlResolver) {
        if (user == null) {
            return null;
        }
        String avatar = user.getAvatarUrl();
        if (avatarUrlResolver != null && avatar != null && !avatar.isBlank()) {
            avatar = avatarUrlResolver.apply(avatar.trim());
        }
        return MobiUserFeign.builder()
                .userId(user.getUserId())
                .openid(user.getOpenid())
                .unionid(user.getUnionid())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatarUrl(avatar)
                .status(user.getStatus())
                .hasPassword(user.getPassword() != null && !user.getPassword().isBlank())
                .loginIp(user.getLoginIp())
                .lastLoginTime(user.getLastLoginTime())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }
}
