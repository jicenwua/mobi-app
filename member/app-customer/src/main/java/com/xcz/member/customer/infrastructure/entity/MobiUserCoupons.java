package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户持有折扣券表 mobi_user_coupons
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("mobi_user_coupons")
public class MobiUserCoupons implements Serializable {
    private static final long serialVersionUID = 1L;

    /***用户优惠券ID**/
    @TableId(type = IdType.AUTO)
    private Long userCouponId;
    /***用户ID**/
    private Long userId;
    /***店铺ID**/
    private Long shopId;
    /***关联模板ID**/
    private Long templateId;
    /***状态：0-未使用 1-已使用 2-已过期**/
    private Integer status;
    /***使用时间**/
    private LocalDateTime usedTime;
    /***领取时间**/
    private LocalDateTime receiveTime;
    /***过期时间（领取时按模板 valid_days 计算，为空表示永不过期）**/
    private LocalDateTime expireTime;
}
