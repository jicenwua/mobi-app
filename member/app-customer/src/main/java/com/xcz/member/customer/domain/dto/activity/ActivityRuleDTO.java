package com.xcz.member.customer.domain.dto.activity;

import com.xcz.member.customer.infrastructure.cache.dto.ActivityRuleCacheDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 满赠规则领域读模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRuleDTO {

    /***规则 ID**/
    private Long ruleId;
    /***所属活动 ID**/
    private Long activityId;
    /***门槛积分**/
    private Integer thresholdAmount;
    /***赠送积分**/
    private Integer giftPoints;



    public static ActivityRuleDTO toDto(ActivityRuleCacheDTO cacheDTO) {
        if (cacheDTO == null) {
            return null;
        }
        return ActivityRuleDTO.builder()
                .ruleId(cacheDTO.getRuleId())
                .activityId(cacheDTO.getActivityId())
                .thresholdAmount(cacheDTO.getThresholdAmount())
                .giftPoints(cacheDTO.getGiftPoints())
                .build();
    }
}
