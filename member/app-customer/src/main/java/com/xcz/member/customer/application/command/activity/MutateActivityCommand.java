package com.xcz.member.customer.application.command.activity;

import com.xcz.member.customer.domain.dto.activity.ActivityRuleDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动写操作命令载体（创建、全量更新、调整时间共用，字段校验由 Assembler 按场景执行）。
 *
 * @param activityId   活动 ID，创建时为 null
 * @param shopId       店铺 ID
 * @param activityName 活动名称
 * @param description  活动描述/公告正文
 * @param kind         活动种类：1-满赠 2-公告
 * @param activityType 满赠子类型，公告时为 null
 * @param startTime    开始时间
 * @param endTime      结束时间
 * @param rules        满赠规则，公告或调整时间时为 null
 */
public record MutateActivityCommand(
        Long activityId,
        Long shopId,
        String activityName,
        String description,
        Integer kind,
        Integer activityType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<ActivityRuleDTO> rules
) {
}
