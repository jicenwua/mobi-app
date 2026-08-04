package com.xcz.member.customer.api.dto.request.activity;

import com.xcz.member.customer.api.dto.request.BaseReq;
import com.xcz.member.customer.domain.dto.activity.ActivityRuleDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动 HTTP 请求参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityReq extends BaseReq {

    /***活动 ID**/
    private Long activityId;
    /***店铺 ID**/
    private Long shopId;
    /***活动名称**/
    private String activityName;
    /***活动描述/公告正文**/
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
    /***满赠规则列表**/
    private List<ActivityRuleDTO> rules;
}
