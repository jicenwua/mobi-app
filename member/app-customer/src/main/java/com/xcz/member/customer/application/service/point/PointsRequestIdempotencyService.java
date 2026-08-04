package com.xcz.member.customer.application.service.point;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.domain.service.MobiPointsLogService;
import com.xcz.member.customer.infrastructure.cache.PurchaseIdempotentCache;
import com.xcz.member.customer.infrastructure.entity.MobiPointsLog;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 积分购买/扣款幂等编排：Redis 快速路径 + DB request_id 持久化。
 */
@Service
public class PointsRequestIdempotencyService {

    @Resource
    private MobiPointsLogService mobiPointsLogService;

    /**
     * 查询已完成的幂等结果（先 Redis，再 DB）。
     */
    public Optional<Long> findCompletedLogId(String requestId) {
        Optional<Long> cached = PurchaseIdempotentCache.getCompletedLogId(requestId);
        if (cached.isPresent()) {
            return cached;
        }
        MobiPointsLog log = mobiPointsLogService.findByRequestId(requestId);
        if (log == null || log.getLogId() == null) {
            return Optional.empty();
        }
        PurchaseIdempotentCache.complete(requestId, log.getLogId());
        return Optional.of(log.getLogId());
    }

    public void assertNotProcessing(String requestId) {
        if (PurchaseIdempotentCache.isProcessing(requestId)) {
            throw new ServiceException("请求处理中，请勿重复提交");
        }
    }

    public void acquireOrThrow(String requestId) {
        assertNotProcessing(requestId);
        if (!PurchaseIdempotentCache.tryAcquire(requestId)) {
            throw new ServiceException("请求处理中，请勿重复提交");
        }
    }

    public void complete(String requestId, Long logId) {
        PurchaseIdempotentCache.complete(requestId, logId);
    }

    public void release(String requestId) {
        PurchaseIdempotentCache.release(requestId);
    }
}
