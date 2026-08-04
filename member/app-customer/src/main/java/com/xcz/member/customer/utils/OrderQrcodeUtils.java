package com.xcz.member.customer.utils;

import com.xcz.member.customer.domain.enums.Constants;
import com.xcz.member.customer.infrastructure.cache.dto.OrderTokenCacheDTO;
import lombok.experimental.UtilityClass;
import org.redisson.api.RBucket;

import java.time.Duration;
import java.util.UUID;

/**
 * 订单核销码短时 token（Redis，绑定 logId / shopId / userId）
 */
@UtilityClass
public class OrderQrcodeUtils {

    private static final String TOKEN_KEY = "order:token:";

    /** 订单码有效时长（毫秒），待核销期间可刷新展示 */
    public static final long TOKEN_TTL_MS = 24 * 60 * 60 * 1000L;

    /**
     * 生成并缓存订单核销 token
     */
    public static String issueToken(Long logId, Long shopId, Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        RBucket<OrderTokenCacheDTO> bucket = Constants.CACHE.getBucket(TOKEN_KEY + token);
        bucket.set(OrderTokenCacheDTO.builder()
                .logId(logId)
                .shopId(shopId)
                .userId(userId)
                .build(), Duration.ofMillis(TOKEN_TTL_MS));
        return token;
    }

    /**
     * 读取 token 对应 payload，不存在或已过期返回 null
     */
    public static OrderTokenCacheDTO getPayload(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        RBucket<OrderTokenCacheDTO> bucket = Constants.CACHE.getBucket(TOKEN_KEY + token.trim());
        return bucket.get();
    }

    /**
     * 原子读取并删除 token，防止同一订单码重复核销。
     */
    public static OrderTokenCacheDTO tryConsumePayload(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        RBucket<OrderTokenCacheDTO> bucket = Constants.CACHE.getBucket(TOKEN_KEY + token.trim());
        return bucket.getAndDelete();
    }

    /**
     * 核销成功后删除 token（单次使用）
     */
    public static void invalidate(String token) {
        if (token != null && !token.isBlank()) {
            Constants.CACHE.getBucket(TOKEN_KEY + token.trim()).delete();
        }
    }
}
