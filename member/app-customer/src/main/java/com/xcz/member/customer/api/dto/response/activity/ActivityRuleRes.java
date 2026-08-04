package com.xcz.member.customer.api.dto.response.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 满赠规则 API 响应对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRuleRes {

    /***规则 ID**/
    private Long ruleId;
    /***所属活动 ID**/
    private Long activityId;
    /***门槛积分**/
    private Integer thresholdAmount;
    /***赠送积分**/
    private Integer giftPoints;
}
