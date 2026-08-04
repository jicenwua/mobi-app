package com.xcz.member.customer.infrastructure.cache.dto;

import com.xcz.member.customer.domain.dto.activity.ActivityRuleDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 满赠规则 Redis 缓存模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRuleCacheDTO implements Serializable {

    private Long ruleId;
    private Long activityId;
    private Integer thresholdAmount;
    private Integer giftPoints;

    public static ActivityRuleCacheDTO from(ActivityRuleDTO dto) {
        if (dto == null) {
            return null;
        }
        return ActivityRuleCacheDTO.builder()
                .ruleId(dto.getRuleId())
                .activityId(dto.getActivityId())
                .thresholdAmount(dto.getThresholdAmount())
                .giftPoints(dto.getGiftPoints())
                .build();
    }



    public static List<ActivityRuleCacheDTO> fromList(Collection<ActivityRuleDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        return dtos.stream().map(ActivityRuleCacheDTO::from).toList();
    }

    public static List<ActivityRuleDTO> toDtoList(Collection<ActivityRuleCacheDTO> caches) {
        if (caches == null || caches.isEmpty()) {
            return List.of();
        }
        return caches.stream().map(ActivityRuleDTO::toDto).toList();
    }
}
