package com.xcz.member.feign.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动满赠规则 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobiActivityRuleFeign implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long ruleId;
    private Long activityId;
    /***门槛积分**/
    private Integer thresholdAmount;
    /***赠送积分**/
    private Integer giftPoints;
    private LocalDateTime createTime;
}
