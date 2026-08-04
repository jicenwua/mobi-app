package com.xcz.member.customer.utils;

import com.xcz.member.customer.domain.enums.Constants;
import lombok.experimental.UtilityClass;
import org.redisson.api.RBucket;

import java.time.Duration;
import java.util.UUID;

/**
 * 店员邀请码短时 token（Redis，店长扫码添加店员）
 */
@UtilityClass
public class StaffInviteUtils {

    private static final String TOKEN_KEY = "staff:invite:token:";

    /** 邀请码有效时长（毫秒） */
    public static final long TOKEN_TTL_MS = 300_000L;

    /**
     * 生成并缓存店员邀请 token
     *
     * @param userId 用户 ID
     * @return 邀请 token
     */
    public static String issueToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        RBucket<Long> bucket = Constants.CACHE.getBucket(TOKEN_KEY + token);
        bucket.set(userId, Duration.ofMillis(TOKEN_TTL_MS));
        return token;
    }

    /**
     * 读取 token 对应用户 ID，不存在或已过期返回 null
     *
     * @param token 邀请 token
     * @return 用户 ID，无效时返回 null
     */
    public static Long getUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        RBucket<Long> bucket = Constants.CACHE.getBucket(TOKEN_KEY + token.trim());
        return bucket.get();
    }

    /**
     * 添加成功后删除 token（单次使用）
     *
     * @param token 邀请 token
     */
    public static void invalidate(String token) {
        if (token != null && !token.isBlank()) {
            Constants.CACHE.getBucket(TOKEN_KEY + token.trim()).delete();
        }
    }
}
