package com.xcz.member.customer.domain.promotion;

import com.xcz.member.customer.domain.enums.CouponType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 优惠券抵扣计算（纯领域逻辑，门槛与减免均以积分为单位）。
 */
public final class CouponSettlementCalculator {

    private static final int POINTS_SCALE = 2;
    private static final RoundingMode POINTS_ROUNDING = RoundingMode.CEILING;

    private CouponSettlementCalculator() {
    }

    /**
     * 判断订单原价是否满足优惠券门槛。
     *
     * @param thresholdAmount 门槛积分，null 或 ≤0 表示无门槛
     * @param originalPoints  商品原价折算积分合计
     * @return 是否满足门槛
     */
    public static boolean meetsThreshold(BigDecimal thresholdAmount, BigDecimal originalPoints) {
        if (originalPoints == null || originalPoints.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (thresholdAmount == null || thresholdAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        return originalPoints.compareTo(thresholdAmount) >= 0;
    }

    /**
     * 计算使用优惠券后的应付积分。
     * <p>折扣券：原价 × discountValue / 100；满减券：原价 − discountValue，最低为 0。
     * 结果保留 2 位小数并向上取整。</p>
     *
     * @param type           券类型
     * @param discountValue  折扣力度（如 85 表示 85 折）或减免积分
     * @param originalPoints 商品原价折算积分合计
     * @return 应付积分
     */
    public static BigDecimal calculatePayablePoints(CouponType type,
                                                    BigDecimal discountValue,
                                                    BigDecimal originalPoints) {
        if (originalPoints == null || originalPoints.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(POINTS_SCALE, POINTS_ROUNDING);
        }
        if (type == null || discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            return normalizePoints(originalPoints);
        }
        BigDecimal payable;
        if (type == CouponType.DISCOUNT) {
            payable = originalPoints.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), POINTS_SCALE, POINTS_ROUNDING);
        } else if (type == CouponType.REDUCTION) {
            payable = originalPoints.subtract(discountValue);
            if (payable.compareTo(BigDecimal.ZERO) < 0) {
                payable = BigDecimal.ZERO;
            }
            payable = payable.setScale(POINTS_SCALE, POINTS_ROUNDING);
        } else {
            payable = normalizePoints(originalPoints);
        }
        return payable;
    }

    private static BigDecimal normalizePoints(BigDecimal points) {
        return points.setScale(POINTS_SCALE, POINTS_ROUNDING);
    }

    /**
     * 计算优惠券抵扣积分（原价 − 应付，不足 0 时返回 0）。
     *
     * @param originalPoints 原价积分
     * @param payablePoints  应付积分
     * @return 抵扣积分
     */
    public static BigDecimal calculateDiscountPoints(BigDecimal originalPoints, BigDecimal payablePoints) {
        if (originalPoints == null || payablePoints == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = originalPoints.subtract(payablePoints);
        return discount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : discount;
    }
}
