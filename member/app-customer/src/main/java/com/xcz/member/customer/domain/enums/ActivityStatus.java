package com.xcz.member.customer.domain.enums;

import com.xcz.member.customer.infrastructure.entity.MobiActivity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动状态（对应 mobi_activity.status 持久化字段）。
 * <p>
 * 0-未开始，1-进行中，2-已结束，3-手动停止。
 * 除手动停止外，状态随当前时间与活动周期自动流转。
 */
@Getter
@RequiredArgsConstructor
public enum ActivityStatus {

    /** 未到开始时间 */
    NOT_STARTED(0, "未开始"),
    /** 当前时间在活动周期内 */
    ONGOING(1, "进行中"),
    /** 已超过结束时间 */
    ENDED(2, "已结束"),
    /** 店长手动停止（不再随时间自动变更） */
    MANUALLY_STOPPED(3, "手动停止");

    /** 持久化编码 */
    private final Integer code;
    /** 展示名称 */
    private final String label;

    /**
     * 按编码解析活动状态。
     *
     * @param code 状态编码
     * @return 对应枚举；无法识别时返回 null
     */
    public static ActivityStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ActivityStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据活动时间与当前时刻计算应处于的状态（手动停止保持不变）。
     *
     * @param activity 活动实体
     * @param now      当前时间
     * @return 计算后的状态；活动为空时返回 null
     */
    public static ActivityStatus resolve(MobiActivity activity, LocalDateTime now) {
        if (activity == null || now == null) {
            return null;
        }
        if (MANUALLY_STOPPED.getCode().equals(activity.getStatus())) {
            return MANUALLY_STOPPED;
        }
        return resolveFromTime(activity.getStartTime(), activity.getEndTime(), now);
    }

    /**
     * 仅根据活动时间窗口计算状态（不考虑手动停止等持久化标记）。
     * 用于修改时间后重新判定并写回数据库。
     *
     * @param start 开始时间
     * @param end   结束时间（可为 null 表示永久有效）
     * @param now   当前时间
     * @return 计算后的状态
     */
    public static ActivityStatus resolveFromTime(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (now == null) {
            return null;
        }
        if (start != null && now.isBefore(start)) {
            return NOT_STARTED;
        }
        if (end != null && now.isAfter(end)) {
            return ENDED;
        }
        return ONGOING;
    }

    /**
     * 规范化状态编码。
     *
     * @param code 原始状态编码
     * @return 合法编码；无法识别时返回 null
     */
    public static Integer normalizeCode(Integer code) {
        ActivityStatus status = getByCode(code);
        return status == null ? null : status.getCode();
    }

    /**
     * 判断活动是否处于进行中。
     */
    public static boolean isOngoing(MobiActivity activity, LocalDateTime now) {
        return resolve(activity, now) == ONGOING;
    }

    /**
     * 判断活动是否已开始（含进行中、已结束、手动停止）。
     */
    public static boolean hasStarted(MobiActivity activity, LocalDateTime now) {
        ActivityStatus status = resolve(activity, now);
        return status == ONGOING || status == ENDED || status == MANUALLY_STOPPED;
    }
}
