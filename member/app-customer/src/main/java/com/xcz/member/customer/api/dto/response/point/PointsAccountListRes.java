package com.xcz.member.customer.api.dto.response.point;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 店铺会员积分账户列表项。
 * 店员端展示基础/赠送拆分；小程序 C 端主要使用 remainingPoints / totalUsedPoints。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsAccountListRes {

    /***店铺ID**/
    private Long shopId;
    /***用户ID**/
    private Long userId;
    /***店铺名称**/
    private String shopName;
    /***会员昵称**/
    private String nickname;
    /***会员手机号**/
    private String phone;
    /***基础积分**/
    private Integer basePoints;
    /***赠送积分**/
    private Integer bonusPoints;
    /***剩余积分（基础 + 赠送）**/
    private Integer remainingPoints;
    /***累计已用积分**/
    private Integer totalUsedPoints;
    /***账户更新时间**/
    private LocalDateTime updateTime;
}
