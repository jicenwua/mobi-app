package com.xcz.member.customer.domain.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户持券简要读模型（应用层内部使用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponBriefDTO {

    /***用户持券 ID**/
    private Long userCouponId;
    /***用户 ID**/
    private Long userId;
    /***用户昵称（读侧填充）**/
    private String nickname;
    /***店铺 ID**/
    private Long shopId;
    /***券模板 ID**/
    private Long templateId;
    /***优惠券名称（来自模板）**/
    private String couponName;
    /***类型：1-折扣 2-满减**/
    private Integer type;
    /***使用门槛金额**/
    private BigDecimal thresholdAmount;
    /***折扣力度或减免金额**/
    private BigDecimal discountValue;
    /***状态：0-未使用 1-已使用 2-已过期**/
    private Integer status;
    /***领取时间**/
    private LocalDateTime receiveTime;
    /***使用时间**/
    private LocalDateTime usedTime;
    /***过期时间**/
    private LocalDateTime expireTime;
    /***店铺名称（读侧填充）**/
    private String shopName;
}
