package com.xcz.member.feign.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动详情（含规则列表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobiActivityDetailVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long activityId;
    private Long shopId;
    private String shopName;
    private String activityName;
    private Integer activityType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<MobiActivityRuleFeign> rules;
}
