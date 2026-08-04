package com.xcz.member.customer.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.customer.infrastructure.entity.MobiUserCoupons;

import java.time.LocalDateTime;

/**
 * 用户持券持久化端口。
 */
public interface MobiUserCouponsService extends IService<MobiUserCoupons> {

    /**
     * 乐观核销优惠券：仅未使用状态可更新，防止并发重复使用。
     *
     * @param userCouponId 用户持券 ID
     * @param usedTime     使用时间
     * @param unusedStatus 未使用状态码
     * @param usedStatus   已使用状态码
     * @return 影响行数，0 表示券不可用或已被使用
     */
    int markUsedIfUnused(Long userCouponId, LocalDateTime usedTime, int unusedStatus, int usedStatus);
}
