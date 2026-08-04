package com.xcz.member.customer.api.dto.response.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动 API 响应对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRes {

    /***活动 ID**/
    private Long activityId;
    /***店铺 ID**/
    private Long shopId;
    /***店铺名称**/
    private String shopName;
    /***活动名称**/
    private String activityName;
    /***活动描述**/
    private String description;
    /***活动种类：1-满赠活动 2-公告**/
    private Integer kind;
    /***满赠子类型：1-充值满赠 2-消费满赠**/
    private Integer activityType;
    /***开始时间**/
    private LocalDateTime startTime;
    /***结束时间**/
    private LocalDateTime endTime;
    /***活动状态：0-未开始 1-进行中 2-已结束 3-手动停止**/
    private Integer status;
    /***创建时间**/
    private LocalDateTime createTime;
    /***满赠规则列表**/
    private List<ActivityRuleRes> rules;
}
