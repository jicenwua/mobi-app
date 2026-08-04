package com.xcz.member.customer.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcz.member.customer.infrastructure.entity.MobiUserCoupons;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 用户优惠券 Mapper。
 */
@Mapper
public interface MobiUserCouponsMapper extends BaseMapper<MobiUserCoupons> {

    /**
     * 乐观核销：仅当券状态为未使用时更新为已使用。
     *
     * @param userCouponId 用户持券 ID
     * @param usedTime     使用时间
     * @param unusedStatus 未使用状态码
     * @param usedStatus   已使用状态码
     * @return 影响行数
     */
    int markUsedIfUnused(@Param("userCouponId") Long userCouponId,
                         @Param("usedTime") LocalDateTime usedTime,
                         @Param("unusedStatus") int unusedStatus,
                         @Param("usedStatus") int usedStatus);
}
