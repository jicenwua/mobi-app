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
 * 新增/修改活动（含规则）请求体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobiActivitySaveDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /***为空表示新增**/
    private Long activityId;
    private Long shopId;
    private String activityName;
    private Integer activityType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /***0-未启用，1-已启用；新增可不传，默认已启用**/
    private Integer status;
    private List<MobiActivityRuleFeign> rules;
}
