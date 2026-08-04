package com.xcz.member.customer.infrastructure.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import com.xcz.member.customer.domain.enums.Constants;
import com.xcz.member.customer.infrastructure.cache.dto.ShopBriefCacheDTO;
import com.xcz.member.customer.infrastructure.entity.MobiShop;
import com.xcz.member.customer.infrastructure.mapper.MobiShopMapper;
import lombok.experimental.UtilityClass;
import org.redisson.api.RMapCache;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;
import org.redisson.client.protocol.ScoredEntry;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 店铺 Redis 缓存门面：简要信息、代码索引与用户进店记录。（不包含店铺商品信息）
 * <p>
 * 写侧失效由 {@link com.xcz.member.customer.infrastructure.shop.ShopRepositoryImpl}、
 * {@link com.xcz.member.customer.infrastructure.service.MobiShopUserServiceImpl} 在业务成功后自动调用。
 */
@UtilityClass
public class ShopCache {

    private static final RedissonClient REDISSON = Constants.CACHE;
    private static final String SHOP_INFO_KEY = "shop:info";
    private static final String SHOP_CODE_KEY = "shop:code";
    private static final String SHOP_ENTER_KEY = "shop:enter:";
    private static final long BRIEF_TTL_MILLIS = 1000 * 60 * 60L;
    /** 进入记录缓存保留时长（待定时任务落库） */
    private static final Duration ENTER_CACHE_TTL = Duration.ofDays(2);

    private static final Function<Void, RMapCache<Long, ShopBriefCacheDTO>> CACHE =
            ignored -> REDISSON.getMapCache(SHOP_INFO_KEY);

    private static final Function<Void,RMapCache<String,Long>> CODE_CACHE =
            ignored -> REDISSON.getMapCache(SHOP_CODE_KEY );

    private static final Function<Long, RScoredSortedSet<Long>> ENTER_CACHE =
            userId -> REDISSON.getScoredSortedSet(SHOP_ENTER_KEY + userId);

    /**
     * 按店铺 ID 批量读取缓存。
     *
     * @param shopIds 店铺 ID 列表
     * @return 命中的店铺缓存，key 为店铺 ID；入参为空时返回空 Map
     */
    public Map<Long, ShopBriefDTO> getByIds(Collection<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return Collections.emptyMap();
        }
        RMapCache<Long, ShopBriefCacheDTO> mapCache = CACHE.apply(null);
        Map<Long, ShopBriefCacheDTO> cached = mapCache.getAll(new HashSet<>(shopIds));
        Map<Long, ShopBriefDTO> result = new HashMap<>(cached.size());
        cached.forEach((id, dto) -> result.put(id, ShopBriefDTO.toDto(dto)));
        return result;
    }

    /**
     * 按店铺 ID 获取简要信息（优先 Redis，未命中则查库并回填）。
     *
     * @param shopId 店铺 ID
     * @param mapper 店铺 Mapper
     * @return 店铺简要读模型，不存在时返回 null
     */
    public ShopBriefDTO getById(Long shopId, MobiShopMapper mapper) {
        if (shopId == null) {
            return null;
        }
        ShopBriefDTO cached = getByIds(List.of(shopId)).get(shopId);
        if (cached != null) {
            return cached;
        }
        // 如果缓存中没有找到则从数据库中获取并添加到缓存中
        MobiShop shop = mapper.selectById(shopId);
        if (shop == null) {
            return null;
        }
        ShopBriefDTO brief = ShopBriefDTO.of(shop);
        putAll(List.of(brief));
        return brief;
    }

    /**
     * 按店铺代码获取简要信息（优先 Redis，未命中则查库并回填）。
     *
     * @param shopCode 店铺唯一代码
     * @param mapper   店铺 Mapper
     * @return 店铺简要读模型，不存在时返回 null
     */
    public ShopBriefDTO getByShopCode(String shopCode, MobiShopMapper mapper) {
        if (shopCode == null || shopCode.isBlank()) {
            return null;
        }

        RMapCache<String, Long> cache = CODE_CACHE.apply(null);
        Long shopId = cache.get(shopCode);
        if (shopId != null) {
            return getById(shopId, mapper);
        }
        //如果缓存中没有改code，则从数据库中获取
        MobiShop shop = mapper.selectOne(
                new LambdaQueryWrapper<MobiShop>().eq(MobiShop::getShopCode, shopCode)
        );
        if (shop == null) {
            return null;
        }
        cache.fastPut(shop.getShopCode(),shop.getId(), BRIEF_TTL_MILLIS, TimeUnit.MILLISECONDS);
        ShopBriefDTO brief = ShopBriefDTO.of(shop);
        putAll(List.of(brief));
        return brief;
    }

    /**
     * 批量写入店铺缓存。
     *
     * @param shops 待缓存的店铺简要信息
     */
    public void putAll(Collection<ShopBriefDTO> shops) {
        if (shops == null || shops.isEmpty()) {
            return;
        }
        Map<Long, ShopBriefCacheDTO> entries = shops.stream()
                .filter(dto -> dto != null && dto.id() != null)
                .collect(Collectors.toMap(
                        ShopBriefDTO::id,
                        ShopBriefCacheDTO::from,
                        (a, b) -> b,
                        HashMap::new
                ));
        if (entries.isEmpty()) {
            return;
        }
        CACHE.apply(null).putAll(entries, BRIEF_TTL_MILLIS, TimeUnit.MILLISECONDS);
        //根据店铺的code保存对应的店铺id
        Map<String, Long> collect = shops.stream()
                .collect(Collectors.toMap(ShopBriefDTO::shopCode, ShopBriefDTO::id));
        CODE_CACHE.apply(null).putAll(collect, BRIEF_TTL_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * 删除单个店铺缓存。
     *
     * @param shopId 店铺 ID
     */
    public void evict(Long shopId) {
        if (shopId == null) {
            return;
        }
        CACHE.apply(null).remove(shopId);
    }

    /**
     * 批量删除店铺缓存。
     *
     * @param shopIds 店铺 ID 列表
     */
    public void evictByIds(Collection<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return;
        }
        RMapCache<Long, ShopBriefCacheDTO> mapCache = CACHE.apply(null);
        shopIds.stream()
                .filter(Objects::nonNull)
                .forEach(mapCache::remove);
    }

    /**
     * 清除店铺相关的全部缓存（简要信息、代码索引）。
     *
     * @param shopId   店铺 ID
     * @param shopCode 店铺唯一代码
     */
    public void evictShop(Long shopId, String shopCode) {
        evict(shopId);
        if (shopCode != null && !shopCode.isBlank()) {
            CODE_CACHE.apply(null).remove(shopCode);
        }
    }

    /**
     * 记录用户进入店铺（score 为进入时间毫秒戳，同日多次进入会覆盖为最新时间）。
     *
     * @param userId 用户 ID
     * @param shopId 店铺 ID
     */
    public void recordEnter(Long userId, Long shopId) {
        if (userId == null || shopId == null) {
            return;
        }
        RScoredSortedSet<Long> enterSet = ENTER_CACHE.apply(userId);
        enterSet.add(System.currentTimeMillis(), shopId);
        enterSet.expire(ENTER_CACHE_TTL);
    }

    /**
     * 读取用户待同步的进入记录。
     *
     * @param userId 用户 ID
     * @return 店铺 ID → 进入时间毫秒戳
     */
    public Map<Long, Long> getEnterRecords(Long userId) {
        if (userId == null) {
            return Collections.emptyMap();
        }
        Collection<ScoredEntry<Long>> entries = ENTER_CACHE.apply(userId).entryRange(0, -1);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> result = new HashMap<>(entries.size());
        for (ScoredEntry<Long> entry : entries) {
            result.put(entry.getValue(), entry.getScore().longValue());
        }
        return result;
    }

    /**
     * 删除已落库的进入记录。
     *
     * @param userId  用户 ID
     * @param shopIds 店铺 ID 列表
     */
    public void removeEnterRecords(Long userId, Collection<Long> shopIds) {
        if (userId == null || shopIds == null || shopIds.isEmpty()) {
            return;
        }
        ENTER_CACHE.apply(userId).removeAll(new HashSet<>(shopIds));
    }

    /**
     * 扫描存在待同步进入记录的用户 ID。
     *
     * @return 用户 ID 列表
     */
    public List<Long> listUserIdsWithEnterCache() {
        String pattern = SHOP_ENTER_KEY + "*";
        Iterable<String> keys = REDISSON.getKeys().getKeys(KeysScanOptions.defaults().pattern(pattern));
        String prefix = SHOP_ENTER_KEY;
        List<Long> userIds = new ArrayList<>();
        for (String key : keys) {
            if (!key.startsWith(prefix)) {
                continue;
            }
            String userIdStr = key.substring(prefix.length());
            try {
                userIds.add(Long.parseLong(userIdStr));
            } catch (NumberFormatException ignored) {
                // skip malformed key
            }
        }
        return userIds;
    }
}
