package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 店铺会员积分余额表 mobi_points_account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("mobi_points_account")
public class MobiPointsAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    /***用户ID**/
    private Long userId;
    /***店铺ID**/
    private Long shopId;
    /***当前基础积分**/
    private BigDecimal basePoints;
    /***当前赠送积分**/
    private BigDecimal bonusPoints;
    /***该店铺累计已使用积分**/
    private BigDecimal totalUsedPoints;
    /***更新时间**/
    private LocalDateTime updateTime;
}
