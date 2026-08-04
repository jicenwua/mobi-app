package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 折扣券配置表 mobi_coupon_template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("mobi_coupon_template")
public class MobiCouponTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    /***模板ID**/
    @TableId(type = IdType.AUTO)
    private Long templateId;
    /***店铺ID**/
    private Long shopId;
    /***优惠券名称**/
    private String couponName;
    /***类型：1-折扣(打折) 2-固定减免(满减)**/
    private Integer type;
    /***满减阈值(满多少可用)**/
    private BigDecimal thresholdAmount;
    /***打折力度(如85表示85折) 或 减免金额**/
    private BigDecimal discountValue;
    /***领取后有效天数；为空表示领取后永不过期**/
    private Integer validDays;
    /***发放数量；null 表示无限发放**/
    private Long totalQuantity;
    /***已发放数量**/
    private Long issuedQuantity;
    /***开始发放时间**/
    private LocalDateTime distributionStartTime;
    /***结束发放时间**/
    private LocalDateTime distributionEndTime;
    /***发放控制：0-禁用 1-启用 2-手动停止发放**/
    private Integer distributionStatus;
    /***创建时间**/
    private LocalDateTime createTime;
}
