package com.xcz.member.customer.infrastructure.task;

import com.xcz.member.customer.infrastructure.task.service.ProductSoldSyncService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 定时将 Redis 中的商品已售增量刷新到数据库。
 */
@Component
public class ProductSoldSyncTask {

    @Resource
    private ProductSoldSyncService productSoldSyncService;

    /**
     * 每 5 分钟同步一次已售数量。
     */
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void syncSoldCount() {
        productSoldSyncService.syncAll(productSoldSyncService);
    }
}
