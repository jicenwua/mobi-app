package com.xcz.member.feign.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理后台发放用户折扣券
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobiUserCouponGrantFeign implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long shopId;
    private Long templateId;
}
