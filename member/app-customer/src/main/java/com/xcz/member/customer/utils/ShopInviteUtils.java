package com.xcz.member.customer.utils;

import com.xcz.member.customer.domain.enums.Constants;
import lombok.experimental.UtilityClass;
import org.redisson.api.RBucket;

import java.time.Duration;
import java.util.UUID;

/**
 * 店铺邀请码短时 token（Redis，防截图传播）
 */
@UtilityClass
public class ShopInviteUtils {

    private static final String TOKEN_KEY = "shop:invite:token:";

    /***邀请码有效时长（毫秒）***/
    public static final long TOKEN_TTL_MS = 120_000L;

    /** scene 前缀，后接 31 位 token（微信 scene 最长 32 字符） */
    private static final String SCENE_PREFIX = "t";

    /**
     * 生成并缓存店铺邀请 token
     *
     * @param shopId 店铺 ID
     * @return 微信 scene 字符串（含前缀）
     */
    public static String issueScene(Long shopId) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 31);
        RBucket<Long> bucket = Constants.CACHE.getBucket(TOKEN_KEY + token);
        bucket.set(shopId, Duration.ofMillis(TOKEN_TTL_MS));
        return SCENE_PREFIX + token;
    }

    /**
     * 从 scene 或裸 token 解析店铺 ID，不存在或已过期返回 null
     *
     * @param sceneOrToken scene（t 开头）或 31 位 token
     * @return 店铺 ID
     */
    public static Long getShopId(String sceneOrToken) {
        if (sceneOrToken == null || sceneOrToken.isBlank()) {
            return null;
        }
        String token = sceneOrToken.trim();
        if (token.startsWith(SCENE_PREFIX)) {
            token = token.substring(SCENE_PREFIX.length());
        }
        if (token.isBlank()) {
            return null;
        }
        RBucket<Long> bucket = Constants.CACHE.getBucket(TOKEN_KEY + token);
        return bucket.get();
    }

}
