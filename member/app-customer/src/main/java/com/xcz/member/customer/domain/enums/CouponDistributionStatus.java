package com.xcz.member.customer.domain.enums;

import com.xcz.member.customer.infrastructure.entity.MobiCouponTemplate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 优惠券发放状态（对应 mobi_coupon_template.distribution_status）。
 * <p>
 * 0-禁用，1-未开始，2-发放中，3-已结束，4-手动停止。
 * 除禁用与手动停止外，状态随当前时间、发放窗口与库存自动流转。
 */
@Getter
@RequiredArgsConstructor
public enum CouponDistributionStatus {

    /** 模板已禁用 */
    DISABLED(0, "禁用"),
    /** 未到发放开始时间 */
    NOT_STARTED(1, "未开始发放"),
    /** 正在发放且仍有库存 */
    DISTRIBUTING(2, "发放中"),
    /** 时间结束或库存耗尽 */
    ENDED(3, "发放结束"),
    /** 店长手动停止发放 */
    MANUALLY_STOPPED(4, "手动停止发放");

    /** 持久化编码 */
    private final int code;
    /** 展示名称 */
    private final String label;

    /**
     * 按编码解析发放状态。
     *
     * @param code 状态编码
     * @return 对应枚举；无法识别时返回 null
     */
    public static CouponDistributionStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponDistributionStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据模板与当前时刻计算应处于的发放状态（禁用、手动停止保持不变）。
     *
     * @param template 券模板实体
     * @param now      当前时间
     * @return 计算后的状态；模板为空时返回 null
     */
    public static CouponDistributionStatus resolve(MobiCouponTemplate template, LocalDateTime now) {
        if (template == null || now == null) {
            return null;
        }
        Integer rawCode = template.getDistributionStatus();
        if (rawCode != null && DISABLED.code == rawCode) {
            return DISABLED;
        }
        if (isManuallyStoppedCode(rawCode)) {
            return MANUALLY_STOPPED;
        }
        return resolveFromTimeAndStock(
                template.getDistributionStartTime(),
                template.getDistributionEndTime(),
                template.getIssuedQuantity(),
                template.getTotalQuantity(),
                now);
    }

    /**
     * 仅根据发放时间窗口与库存计算状态（不考虑禁用、手动停止等持久化标记）。
     */
    public static CouponDistributionStatus resolveFromTimeAndStock(
            LocalDateTime start,
            LocalDateTime end,
            Long issuedQuantity,
            Long totalQuantity,
            LocalDateTime now) {
        if (now == null) {
            return null;
        }
        if (start != null && now.isBefore(start)) {
            return NOT_STARTED;
        }
        if (end != null && now.isAfter(end)) {
            return ENDED;
        }
        long issued = issuedQuantity == null ? 0L : issuedQuantity;
        if (totalQuantity != null && totalQuantity > 0 && issued >= totalQuantity) {
            return ENDED;
        }
        return DISTRIBUTING;
    }

    /**
     * 判断编码是否表示手动停止。
     */
    public static boolean isManuallyStoppedCode(Integer code) {
        return code != null && MANUALLY_STOPPED.code == code;
    }

    /**
     * 判断券模板是否处于发放中。
     */
    public static boolean isDistributing(MobiCouponTemplate template, LocalDateTime now) {
        return resolve(template, now) == DISTRIBUTING;
    }

    /**
     * 计算剩余可发放数量（基于数据库已发放数）。
     *
     * @param template 券模板实体
     * @return 剩余库存，无限量时返回 null
     */
    public static Long remainingStock(MobiCouponTemplate template) {
        if (template == null || template.getTotalQuantity() == null) {
            return null;
        }
        long issued = template.getIssuedQuantity() == null ? 0L : template.getIssuedQuantity();
        long total = template.getTotalQuantity();
        return Math.max(0L, total - issued);
    }
}
