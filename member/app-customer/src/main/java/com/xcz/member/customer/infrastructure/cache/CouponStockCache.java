package com.xcz.member.customer.infrastructure.cache;

import com.xcz.member.customer.domain.enums.Constants;
import com.xcz.member.customer.infrastructure.entity.MobiCouponTemplate;
import com.xcz.member.customer.infrastructure.entity.MobiUserCoupons;
import com.xcz.commons.security.utils.SecurityUtils;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 优惠券发放库存 Redis 层（高并发领取场景）。
 * <p>
 * 写侧同步由 {@link com.xcz.member.customer.infrastructure.service.MobiCouponTemplateServiceImpl} 自动维护。
 */
public final class CouponStockCache {

    /** Redis 无限库存标记（与 {@link ProductStockCache#UNLIMITED_STOCK} 一致） */
    public static final long UNLIMITED_STOCK = -1L;

    private static final RedissonClient REDISSON = Constants.CACHE;
    //缓存优惠券库存
    private static final String STOCK_KEY = "coupon:stock:";
    //缓存优惠券信息
    private static final String INFO_KEY = "coupon:info:";
    //缓存优惠券领取id
    private static final String CLAIMED_KEY = "coupon:claimed:";

    /** 正在发放中的模板 bucket，供定时任务同步库存 */
    private static final Map<Long, RBucket<MobiCouponTemplate>> ACTIVE_BUCKETS = new ConcurrentHashMap<>();

    private CouponStockCache() {
    }

    /**
     * 预热券模板到 Redis（仅在首次发放前初始化库存）。
     *
     * @param template 券模板实体
     */
    public static void warmTemplate(MobiCouponTemplate template) {
        if (template == null || template.getTemplateId() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        //如果优惠券还没有开始或者以及结束，则直接返回
        if (template.getDistributionStartTime() != null && now.isBefore(template.getDistributionStartTime())) {
            return;
        }
        if (template.getDistributionEndTime() != null && now.isAfter(template.getDistributionEndTime())) {
            return;
        }
        //优惠券已经被领取数量
        long issued = template.getIssuedQuantity() == null ? 0L : template.getIssuedQuantity();
        Long totalQuantity = template.getTotalQuantity();
        long remaining;
        if (isUnlimitedTotal(totalQuantity)) {
            remaining = UNLIMITED_STOCK;
        } else {
            remaining = totalQuantity - issued;
            if (remaining <= 0) {
                return;
            }
        }

        Long templateId = template.getTemplateId();
        RAtomicLong stock = stockKey(templateId);
        if (stock.compareAndSet(0, remaining)) {
            RBucket<MobiCouponTemplate> bucket = infoKey(templateId);
            bucket.set(template);
            ACTIVE_BUCKETS.putIfAbsent(templateId, bucket);
            //如果有结束时间，则设置缓存时间为到过期的时间
            if (template.getDistributionEndTime() != null) {
                Duration ttl = Duration.between(now, template.getDistributionEndTime()).plusMinutes(1);
                if (!ttl.isNegative() && !ttl.isZero()) {
                    stock.expire(ttl);
                    bucket.expire(ttl);
                }
            }
        }
    }

    /**
     * 尝试从 Redis 扣减库存并生成待入库的用户持券实体。
     *
     * @param templateId 券模板 ID
     * @return 领取成功时返回待保存实体；失败返回 null
     */
    public static MobiUserCoupons tryClaim(Long templateId) {
        Long userId = SecurityUtils.getUserId();
        RBucket<MobiCouponTemplate> bucket = infoKey(templateId);
        MobiCouponTemplate template = bucket.get();
        if (template == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (template.getDistributionEndTime() != null && now.isAfter(template.getDistributionEndTime())) {
            return null;
        }
        //判断是否有库存
        RAtomicLong stock = stockKey(templateId);
        boolean unlimited = isUnlimitedTotal(template.getTotalQuantity());
        if (!unlimited && stock.get() <= 0) {
            return null;
        }
        //判断是否已经领取过
        RSetCache<Long> claimed = claimedKey(templateId);
        if (!claimed.add(userId, 365, TimeUnit.DAYS)) {
            return null;
        }
        if (!unlimited) {
            long remaining = stock.decrementAndGet();
            if (remaining < 0) {
                rollbackClaim(templateId, userId);
                return null;
            }
        }
        //构建领取的优惠券的过期时间并且返回
        LocalDateTime expireTime = template.getValidDays() == null ? null : now.plusDays(template.getValidDays());
        return MobiUserCoupons.builder()
                .shopId(template.getShopId())
                .templateId(templateId)
                .userId(userId)
                .status(0)
                .receiveTime(now)
                .expireTime(expireTime)
                .build();
    }

    /**
     * 入库失败时回滚 Redis 领取状态。
     *
     * @param templateId 券模板 ID
     * @param userId     用户 ID
     */
    public static void rollbackClaim(Long templateId, Long userId) {
        RAtomicLong stock = stockKey(templateId);
        if (stock.get() != UNLIMITED_STOCK) {
            stock.incrementAndGet();
        }
        claimedKey(templateId).remove(userId);
    }

    /**
     * 读取 Redis 中剩余可领数量。
     *
     * @param templateId 券模板 ID
     * @return 剩余库存
     */
    public static long getRemaining(Long templateId) {
        return stockKey(templateId).get();
    }

    /**
     * 读取券模板库存计数器（供定时同步任务使用）。
     *
     * @param templateId 券模板 ID
     * @return Redis 原子计数器
     */
    public static RAtomicLong getStockAtomicLong(Long templateId) {
        return stockKey(templateId);
    }

    /**
     * 重置 Redis 库存（发放调整时使用）。
     *
     * @param templateId 券模板 ID
     * @param remaining  剩余数量
     */
    public static void resetStock(Long templateId, long remaining) {
        stockKey(templateId).set(remaining);
    }

    /**
     * 追加 Redis 库存。
     *
     * @param templateId  券模板 ID
     * @param addQuantity 追加数量
     */
    public static void addStock(Long templateId, long addQuantity) {
        stockKey(templateId).addAndGet(addQuantity);
    }

    /**
     * 数据库总量为 null 表示无限发放。
     */
    public static boolean isUnlimitedTotal(Long totalQuantity) {
        return totalQuantity == null;
    }

    /**
     * Redis 剩余量为 {@link #UNLIMITED_STOCK} 表示无限发放。
     */
    public static boolean isUnlimitedRemaining(long remaining) {
        return remaining == UNLIMITED_STOCK;
    }

    /**
     * 获取活跃模板 bucket 映射（供定时同步任务使用）。
     *
     * @return 模板 ID → bucket
     */
    public static Map<Long, RBucket<MobiCouponTemplate>> activeBuckets() {
        return ACTIVE_BUCKETS;
    }

    /**
     * 清除券模板相关的全部 Redis 键。
     *
     * @param templateId 券模板 ID
     */
    public static void evict(Long templateId) {
        ACTIVE_BUCKETS.remove(templateId);
        stockKey(templateId).delete();
        infoKey(templateId).delete();
        claimedKey(templateId).delete();
    }

    private static RAtomicLong stockKey(Long templateId) {
        return REDISSON.getAtomicLong(STOCK_KEY + templateId);
    }

    private static RBucket<MobiCouponTemplate> infoKey(Long templateId) {
        return REDISSON.getBucket(INFO_KEY + templateId);
    }

    private static RSetCache<Long> claimedKey(Long templateId) {
        return REDISSON.getSetCache(CLAIMED_KEY + templateId);
    }
}
