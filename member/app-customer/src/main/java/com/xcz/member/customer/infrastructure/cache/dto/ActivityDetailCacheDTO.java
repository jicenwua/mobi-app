package com.xcz.member.customer.infrastructure.cache.dto;

import com.xcz.member.customer.domain.dto.activity.ActivityDetailDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动详情 Redis 缓存模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDetailCacheDTO implements Serializable {

    private Long activityId;
    private Long shopId;
    private String activityName;
    private String description;
    private Integer kind;
    private Integer activityType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<ActivityRuleCacheDTO> rules;

    public static ActivityDetailCacheDTO from(ActivityDetailDTO dto) {
        if (dto == null) {
            return null;
        }
        return ActivityDetailCacheDTO.builder()
                .activityId(dto.getActivityId())
                .shopId(dto.getShopId())
                .activityName(dto.getActivityName())
                .description(dto.getDescription())
                .kind(dto.getKind())
                .activityType(dto.getActivityType())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(dto.getStatus())
                .createTime(dto.getCreateTime())
                .updateTime(dto.getUpdateTime())
                .rules(ActivityRuleCacheDTO.fromList(dto.getRules()))
                .build();
    }

    public static List<ActivityDetailCacheDTO> fromDetailList(List<ActivityDetailDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        return dtos.stream().map(ActivityDetailCacheDTO::from).toList();
    }

    public static List<ActivityDetailDTO> toDetailList(List<ActivityDetailCacheDTO> caches) {
        if (caches == null || caches.isEmpty()) {
            return List.of();
        }
        return caches.stream().map(ActivityDetailDTO::toDto).toList();
    }


}
