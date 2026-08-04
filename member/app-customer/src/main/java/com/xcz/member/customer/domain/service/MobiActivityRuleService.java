package com.xcz.member.customer.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.customer.infrastructure.entity.MobiActivityRule;
import java.util.List;

/**
 * 活动满赠规则服务层
 */
public interface MobiActivityRuleService extends IService<MobiActivityRule> {
    /**
     * 按活动 ID 查询规则列表（门槛金额升序）
     *
     * @param activityId 活动 ID
     * @return 满赠规则列表
     */
    List<MobiActivityRule> listByActivityId(Long activityId);
    /**
     * 删除活动下全部规则
     *
     * @param activityId 活动 ID
     */
    void removeByActivityId(Long activityId);
}
