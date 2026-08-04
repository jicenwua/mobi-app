package com.xcz.member.customer.infrastructure.cache;

import com.xcz.member.customer.domain.dto.activity.ActivityDetailDTO;
import com.xcz.member.customer.domain.enums.Constants;
import com.xcz.member.customer.infrastructure.cache.dto.ActivityDetailCacheDTO;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 活动读缓存：店铺侧保存活动 ID 列表，详情按 activityId 全局存储，所有读路径共用。
 * <p>
 * 写侧失效由 {@link com.xcz.member.customer.infrastructure.service.MobiActivityServiceImpl} 自动维护。
 */
@Slf4j
@UtilityClass
public class ActivityCache {

    private static final RedissonClient REDISSON = Constants.CACHE;
    /** 店铺活动 ID 集合：activity:shop-ids:{shopId} → activityId */
    private static final String SHOP_KEY = "activity:shop-ids:";
    /** 活动详情：activity:detail → activityId → detail */
    private static final String DETAIL_KEY = "activity:detail";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final long CACHE_TTL_MILLIS = CACHE_TTL.toMillis();

    private static RSet<Long> shopSet(Long shopId) {
        return REDISSON.getSet(SHOP_KEY + shopId);
    }

    private static RMapCache<Long, ActivityDetailCacheDTO> detailMap() {
        return REDISSON.getMapCache(DETAIL_KEY);
    }

    /**
     * 读取店铺全部活动详情，未命中时回源并回填。
     */
    public static List<ActivityDetailDTO> getShopActivities(Long shopId, Supplier<List<ActivityDetailDTO>> loader) {
        if (shopId == null) {
            return List.of();
        }
        RSet<Long> ids = shopSet(shopId);
        if (!ids.isExists()) {
            List<ActivityDetailDTO> loaded = loader.get();
            putAll(shopId, loaded == null ? List.of() : loaded);
            return loaded == null ? List.of() : loaded;
        }
        try {
            Set<Long> activityIds = ids.readAll();
            if (activityIds.isEmpty()) {
                return List.of();
            }
            Map<Long, ActivityDetailCacheDTO> cached = detailMap().getAll(activityIds);
            if (cached.size() != activityIds.size()) {
                log.warn("店铺活动缓存不完整，将删除并回源加载。shopId={}, expected={}, actual={}",
                        shopId, activityIds.size(), cached.size());
                return reloadShop(shopId, loader);
            }
            return cached.values().stream().map(ActivityDetailDTO::toDto).toList();
        } catch (Exception ex) {
            log.warn("店铺活动缓存反序列化失败，将删除并回源加载。shopId={}, key={}{}", shopId, SHOP_KEY, shopId, ex);
            return reloadShop(shopId, loader);
        }
    }

    /**
     * 按活动 ID 读取详情。
     */
    public static ActivityDetailDTO getByActivityId(Long activityId) {
        if (activityId == null) {
            return null;
        }
        try {
            ActivityDetailCacheDTO cached = detailMap().get(activityId);
            return cached == null ? null : ActivityDetailDTO.toDto(cached);
        } catch (Exception ex) {
            log.warn("活动详情缓存反序列化失败，将删除该条。activityId={}", activityId, ex);
            evictRef(activityId);
            return null;
        }
    }

    /**
     * 写入或更新单条活动详情。
     */
    public static void put(Long shopId, ActivityDetailDTO detail) {
        if (shopId == null || detail == null || detail.getActivityId() == null) {
            return;
        }
        detailMap().fastPut(
                detail.getActivityId(),
                ActivityDetailCacheDTO.from(detail),
                CACHE_TTL_MILLIS,
                TimeUnit.MILLISECONDS
        );
        RSet<Long> ids = shopSet(shopId);
        ids.add(detail.getActivityId());
        ids.expire(CACHE_TTL);
    }

    /**
     * 批量写入店铺活动缓存（覆盖该店铺索引）。
     */
    public static void putAll(Long shopId, List<ActivityDetailDTO> details) {
        evictShop(shopId);
        if (details == null || details.isEmpty()) {
            return;
        }
        Map<Long, ActivityDetailCacheDTO> entries = new HashMap<>();
        Set<Long> activityIds = new HashSet<>();
        for (ActivityDetailDTO detail : details) {
            if (detail == null || detail.getActivityId() == null) {
                continue;
            }
            entries.put(detail.getActivityId(), ActivityDetailCacheDTO.from(detail));
            activityIds.add(detail.getActivityId());
        }
        if (!entries.isEmpty()) {
            detailMap().putAll(entries, CACHE_TTL_MILLIS, TimeUnit.MILLISECONDS);
            RSet<Long> ids = shopSet(shopId);
            ids.addAll(activityIds);
            ids.expire(CACHE_TTL);
        }
    }

    /**
     * 清除单条活动详情及店铺索引中的引用。
     */
    public static void evictRef(Long activityId) {
        if (activityId == null) {
            return;
        }
        ActivityDetailCacheDTO cached = detailMap().get(activityId);
        if (cached != null && cached.getShopId() != null) {
            shopSet(cached.getShopId()).remove(activityId);
        }
        detailMap().remove(activityId);
    }

    /**
     * 清除店铺活动 ID 集合及关联详情。
     */
    public static void evictShop(Long shopId) {
        if (shopId == null) {
            return;
        }
        RSet<Long> ids = shopSet(shopId);
        Set<Long> activityIds = ids.readAll();
        if (!activityIds.isEmpty()) {
            detailMap().fastRemoveAsync(activityIds.toArray(new Long[0]));
        }
        ids.delete();
    }

    private static List<ActivityDetailDTO> reloadShop(Long shopId, Supplier<List<ActivityDetailDTO>> loader) {
        evictShop(shopId);
        List<ActivityDetailDTO> loaded = loader.get();
        putAll(shopId, loaded == null ? List.of() : loaded);
        return loaded == null ? List.of() : loaded;
    }
}
