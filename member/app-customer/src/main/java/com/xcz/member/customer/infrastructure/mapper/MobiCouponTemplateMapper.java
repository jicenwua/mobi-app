package com.xcz.member.customer.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcz.member.customer.infrastructure.entity.MobiCouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 优惠券模板 Mapper。
 */
@Mapper
public interface MobiCouponTemplateMapper extends BaseMapper<MobiCouponTemplate> {

    /**
     * 更新优惠券模板剩余可发放数量。
     *
     * @param templateId 优惠券模板 ID
     * @param remaining  剩余数量
     */
    void updateRemaining(@Param("templateId") Long templateId, @Param("remaining") long remaining);
}
