package com.xcz.member.feign.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户持有折扣券 Feign DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobiUserCouponFeign implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userCouponId;
    private Long userId;
    private String nickname;
    private Long shopId;
    private String shopName;
    private Long templateId;
    private String couponName;
    /** 0-未使用 1-已使用 2-已过期 */
    private Integer status;
    private LocalDateTime receiveTime;
    private LocalDateTime usedTime;
    private LocalDateTime expireTime;
}
