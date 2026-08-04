package com.xcz.member.customer.domain.promotion;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.infrastructure.entity.MobiActivity;

import java.time.LocalDateTime;

/**
 * 活动时间规范化：开始时间不填则取创建时刻；结束时间不填表示永久有效。
 */
public final class ActivityTimeNormalizer {

    private ActivityTimeNormalizer() {
    }

    /**
     * 创建活动时规范化开始时间。
     */
    public static LocalDateTime normalizeStartForCreate(LocalDateTime startTime, LocalDateTime createTime) {
        LocalDateTime base = createTime != null ? createTime : LocalDateTime.now();
        return startTime != null ? startTime : base;
    }

    /**
     * 更新活动时规范化开始时间。
     */
    public static LocalDateTime normalizeStartForUpdate(LocalDateTime startTime, MobiActivity existing) {
        if (startTime != null) {
            return startTime;
        }
        if (existing.getCreateTime() != null) {
            return existing.getCreateTime();
        }
        return existing.getStartTime() != null ? existing.getStartTime() : LocalDateTime.now();
    }

    /**
     * 调整时间时规范化开始时间。
     */
    public static LocalDateTime normalizeStartForAdjust(LocalDateTime startTime, MobiActivity existing) {
        if (startTime != null) {
            return startTime;
        }
        return existing.getStartTime();
    }

    /**
     * 结束时间为 null 表示永久有效。
     */
    public static LocalDateTime normalizeEnd(LocalDateTime endTime) {
        return endTime;
    }

    /**
     * 校验时间区间（结束时间可为空）。
     */
    public static void assertValidRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) {
            throw new ServiceException("开始时间不能为空", 400);
        }
        if (endTime != null && !endTime.isAfter(startTime)) {
            throw new ServiceException("结束时间必须晚于开始时间", 400);
        }
    }
}
