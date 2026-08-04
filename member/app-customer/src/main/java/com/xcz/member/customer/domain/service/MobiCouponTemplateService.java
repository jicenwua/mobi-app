package com.xcz.member.customer.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.customer.infrastructure.entity.MobiCouponTemplate;

/**
 * 优惠券模板持久化端口。
 */
public interface MobiCouponTemplateService extends IService<MobiCouponTemplate> {

    /**
     * 根据 Redis 剩余库存反算并更新数据库已发放数量。
     *
     * @param templateId 模板 ID
     * @param remaining  Redis 剩余可领数量
     */
    void updateRemaining(Long templateId, long remaining);

    /**
     * 用户领取优惠券：预热 Redis、扣减库存、持久化持券并更新已发放数量。
     *
     * @param template 券模板
     * @param userId   用户 ID
     * @return 用户持券 ID；领取失败时返回 null
     */
    Long claimForUser(MobiCouponTemplate template, Long userId);
}
