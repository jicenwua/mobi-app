package com.xcz.member.customer.infrastructure.task;

import com.xcz.member.customer.domain.service.MobiCouponTemplateService;
import com.xcz.member.customer.infrastructure.cache.CouponStockCache;
import com.xcz.member.customer.infrastructure.entity.MobiCouponTemplate;
import jakarta.annotation.Resource;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 定时将 Redis 优惠券库存同步到数据库。
 */
@Component
public class CouponStockSyncTask {

    @Resource
    private MobiCouponTemplateService mobiCouponTemplateService;

    /**
     * 定时将 Redis 剩余库存同步到数据库，并在售罄或过期后清理缓存。
     */
    @Scheduled(fixedDelay = 5000)
    public void syncRemainingToDb() {
        Map<Long, RBucket<MobiCouponTemplate>> buckets = CouponStockCache.activeBuckets();
        List<Long> templateIds = new ArrayList<>(buckets.keySet());
        LocalDateTime now = LocalDateTime.now();

        for (Long templateId : templateIds) {
            RBucket<MobiCouponTemplate> bucket = buckets.get(templateId);
            if (bucket == null) {
                continue;
            }
            MobiCouponTemplate template = bucket.get();
            if (template == null) {
                buckets.remove(templateId);
                continue;
            }
            RAtomicLong atomicLong = CouponStockCache.getStockAtomicLong(templateId);
            long remaining = atomicLong.get();
            if (!CouponStockCache.isUnlimitedTotal(template.getTotalQuantity())
                    && !CouponStockCache.isUnlimitedRemaining(remaining)) {
                mobiCouponTemplateService.updateRemaining(templateId, remaining);
            }

            boolean soldOut = !CouponStockCache.isUnlimitedRemaining(remaining) && remaining <= 0;
            boolean expired = template.getDistributionEndTime() != null
                    && now.isAfter(template.getDistributionEndTime());
            if (soldOut || expired) {
                CouponStockCache.evict(templateId);
            }
        }
    }
}
