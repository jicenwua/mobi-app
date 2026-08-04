package com.xcz.member.customer.infrastructure.task;

import com.xcz.member.customer.infrastructure.task.service.ShopEnterTimeSyncService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 定时将 Redis 中的用户进入店铺时间刷新到数据库 last_enter_time 字段。
 */
@Component
public class ShopEnterTimeSyncTask {

    @Resource
    private ShopEnterTimeSyncService shopEnterTimeSyncService;

    /**
     * 每 5 分钟同步一次进入时间。
     */
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void syncLastEnterTime() {
        shopEnterTimeSyncService.syncAll(shopEnterTimeSyncService);
    }
}
