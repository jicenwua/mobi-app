package com.xcz.member.customer.domain.promotion;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.domain.enums.ActivityKind;
import com.xcz.member.customer.domain.enums.ActivityStatus;
import com.xcz.member.customer.domain.enums.ActivityType;
import com.xcz.member.customer.infrastructure.entity.MobiActivity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 活动时间冲突校验：同店铺、同种类、同满赠子类型下，有效活动的时间窗口不可重合。
 */
public final class ActivityScheduleConflictChecker {

    private ActivityScheduleConflictChecker() {
    }

    /**
     * 判断两个时间窗口是否重合（结束时间为 null 表示永久有效；首尾相接不算重合）。
     */
    public static boolean overlaps(LocalDateTime start1, LocalDateTime end1,
                                   LocalDateTime start2, LocalDateTime end2) {
        if (start1 == null || start2 == null) {
            return false;
        }
        boolean firstEndsAfterSecondStarts = end1 == null || end1.isAfter(start2);
        boolean secondEndsAfterFirstStarts = end2 == null || end2.isAfter(start1);
        return firstEndsAfterSecondStarts && secondEndsAfterFirstStarts;
    }

    /**
     * 校验新活动时间与同店铺已存在的有效同类型活动是否重合。
     *
     * @param shopId            店铺 ID
     * @param kind              活动种类
     * @param activityType      满赠子类型（公告时为 null，跳过校验）
     * @param startTime         新活动开始时间
     * @param endTime           新活动结束时间（可为 null）
     * @param excludeActivityId 排除的活动 ID（更新/启用时排除自身），创建时传 null
     * @param existingActivities 同店铺已有活动列表
     */
    public static void assertNoConflict(Long shopId, Integer kind, Integer activityType,
                                        LocalDateTime startTime, LocalDateTime endTime,
                                        Long excludeActivityId, List<MobiActivity> existingActivities) {
        if (!Objects.equals(ActivityKind.PROMOTION.getCode(), kind) || activityType == null) {
            return;
        }
        ActivityType type = ActivityType.getByCode(activityType);
        if (type == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (MobiActivity existing : existingActivities) {
            if (existing == null || !shopId.equals(existing.getShopId())) {
                continue;
            }
            if (excludeActivityId != null && excludeActivityId.equals(existing.getActivityId())) {
                continue;
            }
            if (!Objects.equals(kind, existing.getKind())
                    || !Objects.equals(activityType, existing.getActivityType())) {
                continue;
            }
            ActivityStatus status = ActivityStatus.resolve(existing, now);
            if (status == ActivityStatus.ENDED || status == ActivityStatus.MANUALLY_STOPPED) {
                continue;
            }
            if (overlaps(startTime, endTime, existing.getStartTime(), existing.getEndTime())) {
                throw new ServiceException(
                        "与同类型活动「" + existing.getActivityName() + "」的进行中时间重合，无法保存",
                        400);
            }
        }
    }
}
