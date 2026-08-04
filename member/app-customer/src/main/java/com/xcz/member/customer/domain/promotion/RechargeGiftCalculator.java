package com.xcz.member.customer.domain.promotion;

import com.xcz.member.customer.domain.dto.activity.ActivityRuleDTO;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 充值满赠：按门槛阶梯取最高档赠送积分。
 */
@UtilityClass
public class RechargeGiftCalculator {

    /**
     * @param basePoints 本次充值所得基础积分
     * @param rules        满赠规则（门槛升序或乱序均可）
     * @return 赠送积分，无匹配规则时为 0
     */
    public static BigDecimal calcGiftPoints(BigDecimal basePoints, List<ActivityRuleDTO> rules) {
        if (basePoints == null || basePoints.compareTo(BigDecimal.ZERO) <= 0 || rules == null || rules.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int base = basePoints.intValue();
        return rules.stream()
                .filter(rule -> rule.getThresholdAmount() != null && rule.getGiftPoints() != null)
                .filter(rule -> base >= rule.getThresholdAmount())
                .max(Comparator.comparing(ActivityRuleDTO::getThresholdAmount))
                .map(rule -> BigDecimal.valueOf(rule.getGiftPoints()))
                .orElse(BigDecimal.ZERO);
    }
}
