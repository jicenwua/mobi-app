package com.xcz.member.customer.infrastructure.cache;

import com.xcz.member.customer.domain.enums.Constants;
import lombok.experimental.UtilityClass;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Optional;

/**
 * 购买/扣款幂等缓存：基于 requestId 防止网络重试导致重复下单或重复扣款。
 */
@UtilityClass
public class PurchaseIdempotentCache {

    private static final RedissonClient REDISSON = Constants.CACHE;
    private static final String KEY_PREFIX = "points:idempotent:";
    private static final Duration TTL = Duration.ofHours(24);
    /** 占位值，表示请求正在处理中 */
    private static final String PROCESSING = "PROCESSING";

    /**
     * 尝试占用幂等键。
     *
     * @param requestId 客户端生成的唯一请求 ID
     * @return true 表示获得执行权；false 表示已有相同请求在处理或已完成
     */
    public boolean tryAcquire(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return false;
        }
        RBucket<String> bucket = bucket(requestId);
        return bucket.setIfAbsent(PROCESSING, TTL);
    }

    /**
     * 读取已完成的幂等结果（订单流水 ID）。
     *
     * @param requestId 请求 ID
     * @return 已存在的 logId；处理中或不存在时返回 empty
     */
    public Optional<Long> getCompletedLogId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return Optional.empty();
        }
        String value = bucket(requestId).get();
        if (value == null || PROCESSING.equals(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /**
     * 判断相同 requestId 是否正在处理中。
     */
    public boolean isProcessing(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return false;
        }
        return PROCESSING.equals(bucket(requestId).get());
    }

    /**
     * 记录幂等完成结果。
     *
     * @param requestId 请求 ID
     * @param logId     生成的积分流水 ID
     */
    public void complete(String requestId, Long logId) {
        if (requestId == null || requestId.isBlank() || logId == null) {
            return;
        }
        bucket(requestId).set(String.valueOf(logId), TTL);
    }

    /**
     * 业务失败时释放幂等占位，允许客户端重试。
     */
    public void release(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return;
        }
        RBucket<String> bucket = bucket(requestId);
        if (PROCESSING.equals(bucket.get())) {
            bucket.delete();
        }
    }

    private static RBucket<String> bucket(String requestId) {
        return REDISSON.getBucket(KEY_PREFIX + requestId.trim());
    }
}
