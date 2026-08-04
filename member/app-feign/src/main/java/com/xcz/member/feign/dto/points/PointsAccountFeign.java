package com.xcz.member.feign.dto.points;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店铺下用户积分账户列表项（Feign 传输对象）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsAccountFeign implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
