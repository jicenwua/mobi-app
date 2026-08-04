package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.member.customer.domain.service.MobiUserCouponsService;
import com.xcz.member.customer.infrastructure.entity.MobiUserCoupons;
import com.xcz.member.customer.infrastructure.mapper.MobiUserCouponsMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户持券持久化实现。
 */
@Service
public class MobiUserCouponsServiceImpl extends ServiceImpl<MobiUserCouponsMapper, MobiUserCoupons>
        implements MobiUserCouponsService {

    /**
     * 乐观更新用户券为已使用（仅 unused 状态可成功）。
     *
     * @param userCouponId 用户持券 ID
     * @param usedTime     使用时间
     * @param unusedStatus 未使用状态码
     * @param usedStatus   已使用状态码
     * @return 受影响行数
     */
    @Override
    public int markUsedIfUnused(Long userCouponId, LocalDateTime usedTime, int unusedStatus, int usedStatus) {
        return baseMapper.markUsedIfUnused(userCouponId, usedTime, unusedStatus, usedStatus);
    }
}
