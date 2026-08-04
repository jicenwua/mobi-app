package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.member.customer.domain.service.MobiActivityRuleService;
import com.xcz.member.customer.infrastructure.entity.MobiActivityRule;
import com.xcz.member.customer.infrastructure.mapper.MobiActivityRuleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 活动满赠规则服务实现类
 */
@Service
public class MobiActivityRuleServiceImpl extends ServiceImpl<MobiActivityRuleMapper, MobiActivityRule>
        implements MobiActivityRuleService {
    /**
     * 按活动 ID 查询满赠规则列表
     *
     * @param activityId 活动 ID
     * @return 规则列表（门槛金额升序）
     */
    @Override
    public List<MobiActivityRule> listByActivityId(Long activityId) {
        LambdaQueryWrapper<MobiActivityRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MobiActivityRule::getActivityId, activityId);
        wrapper.orderByAsc(MobiActivityRule::getThresholdAmount);
        return list(wrapper);
    }

    /**
     * 删除活动下全部满赠规则
     *
     * @param activityId 活动 ID
     */
    @Override
    public void removeByActivityId(Long activityId) {
        LambdaQueryWrapper<MobiActivityRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MobiActivityRule::getActivityId, activityId);
        remove(wrapper);
    }
}
