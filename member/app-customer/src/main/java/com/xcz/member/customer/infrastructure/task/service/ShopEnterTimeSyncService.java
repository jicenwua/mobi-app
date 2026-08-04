package com.xcz.member.customer.infrastructure.task.service;

import com.xcz.member.customer.infrastructure.cache.ShopCache;
import com.xcz.member.customer.infrastructure.entity.MobiShopUser;
import com.xcz.member.customer.infrastructure.mapper.MobiShopUserMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 将 Redis 中的用户进入店铺时间同步至数据库。
 */
@Slf4j
@Service
public class ShopEnterTimeSyncService {

    @Resource
    private MobiShopUserMapper mobiShopUserMapper;


    /**
     * 同步所有用户的进入时间缓存到数据库。
     */
    public void syncAll(ShopEnterTimeSyncService self) {
        for (Long userId : ShopCache.listUserIdsWithEnterCache()) {
            try {
                // 通过代理调用，确保 @Transactional 生效
                self.syncUserEnterTime(userId);
            } catch (Exception e) {
                log.warn("同步用户进入店铺时间失败, userId={}", userId, e);
            }
        }
    }

    /**
     * 同步指定用户的进入时间缓存到数据库。
     *
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncUserEnterTime(Long userId) {
        Map<Long, Long> records = ShopCache.getEnterRecords(userId);
        if (records.isEmpty()) {
            return;
        }

        ZoneId zoneId = ZoneId.systemDefault();
        List<MobiShopUser> updates = new ArrayList<>(records.size());
        records.forEach((shopId, enterMillis) -> updates.add(
                MobiShopUser.builder()
                        .shopId(shopId)
                        .lastEnterTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(enterMillis), zoneId))
                        .build()
        ));

        mobiShopUserMapper.batchUpdateLastEnterTime(userId, updates);
        ShopCache.removeEnterRecords(userId, records.keySet());
    }
}
