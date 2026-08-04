package com.xcz.member.customer.utils;

import com.xcz.member.customer.domain.enums.Constants;
import lombok.experimental.UtilityClass;
import org.redisson.api.RBucket;

import java.time.Duration;
import java.util.UUID;

/**
 * 会员付款码短时 token（Redis，防截图重放）
 */
@UtilityClass
public class PayQrcodeUtils {

    private static final String TOKEN_KEY = "pay:token:";

    /***付款码有效时长（毫秒）***/
    public static final long TOKEN_TTL_MS = 120_000L;

    /**
     * 生成并缓存付款码 token
     *
     * @param userId 用户 ID
     * @return 付款码 token
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
     * @param token 付款码 token
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
     * 原子读取并删除 token，防止同一付款码并发扣款。
     *
     * @param token 付款码 token
     * @return 用户 ID，无效或已被使用时返回 null
     */
    public static Long tryConsumeUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        RBucket<Long> bucket = Constants.CACHE.getBucket(TOKEN_KEY + token.trim());
        return bucket.getAndDelete();
    }

    /**
     * 消费成功后删除 token（单次使用）
     *
     * @param token 付款码 token
     */
    public static void invalidate(String token) {
        if (token != null && !token.isBlank()) {
            Constants.CACHE.getBucket(TOKEN_KEY + token.trim()).delete();
        }
    }
}
