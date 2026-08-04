package com.xcz.member.customer.infrastructure.cache;

import com.xcz.member.customer.domain.enums.Constants;
import com.xcz.member.customer.infrastructure.cache.dto.UserInfoCacheDTO;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import lombok.experimental.UtilityClass;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户会话与资料 Redis 缓存工具。
 * <p>
 * 资料写侧刷新由 {@link com.xcz.member.customer.infrastructure.service.MobiUserServiceImpl} 自动维护。
 */
@UtilityClass
public class UserCache {

    private final RedissonClient redisson = Constants.CACHE;
    private final String USER_SESSION = "user:session:";
    private final String USER_INFO = "user:info";
    /** 用户资料缓存 TTL（分钟） */
    private final Long USER_INFO_EXPIRE = 30L;

    /**
     * 缓存微信登录 session_key。
     *
     * @param openId  微信 openId
     * @param session 微信 session_key
     */
    public void addSession(String openId, String session) {
        redisson.getBucket(USER_SESSION + openId).set(session, Duration.ofMinutes(30));
    }

    /**
     * 写入用户资料缓存。
     *
     * @param userInfo 用户资料摘要
     */
    public void saveUserInfo(UserInfoCacheDTO userInfo) {
        redisson.getMapCache(USER_INFO).fastPut(userInfo.getUserId(), userInfo, USER_INFO_EXPIRE, TimeUnit.MINUTES);
    }

    /**
     * 从用户实体写入资料缓存。
     *
     * @param mobiUser 用户实体
     */
    public void saveUserInfo(MobiUser mobiUser) {
        saveUserInfo(UserInfoCacheDTO.from(mobiUser));
    }

    /**
     * 按用户 ID 批量读取资料缓存（保持入参顺序）。
     *
     * @param userIds 用户 ID 列表
     * @return 用户资料列表；未命中项为 null
     */
    public List<UserInfoCacheDTO> getUserInfo(List<Long> userIds) {
        RMapCache<Long, UserInfoCacheDTO> mapCache = redisson.getMapCache(USER_INFO);
        Map<Long, UserInfoCacheDTO> all = mapCache.getAll(new HashSet<>(userIds));
        List<UserInfoCacheDTO> result = new ArrayList<>();
        for (Long userId : userIds) {
            result.add(all.get(userId));
        }
        return result;
    }
}
