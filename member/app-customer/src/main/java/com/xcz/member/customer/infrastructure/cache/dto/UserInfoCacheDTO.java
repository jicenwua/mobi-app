package com.xcz.member.customer.infrastructure.cache.dto;

import com.xcz.member.customer.infrastructure.entity.MobiUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户资料 Redis 缓存模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoCacheDTO implements Serializable {

    private Long userId;
    private String nickName;
    private String avatarUrl;

    public static UserInfoCacheDTO from(MobiUser user) {
        if (user == null) {
            return null;
        }
        return UserInfoCacheDTO.builder()
                .userId(user.getUserId())
                .nickName(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
