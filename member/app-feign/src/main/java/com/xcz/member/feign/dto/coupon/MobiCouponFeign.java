package com.xcz.member.feign.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 店铺折扣券模板 Feign DTO（店长端管理）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobiCouponFeign implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 模板 ID */
    private Long templateId;
    /** 店铺 ID */
    private Long shopId;
    /** 优惠券名称 */
    private String couponName;
    /** 类型：1-折扣(打折) 2-固定减免(满减) */
    private Integer type;
    /** 满减/打折阈值 */
    private BigDecimal thresholdAmount;
    /** 打折力度(如85表示85折) 或 减免金额 */
    private BigDecimal discountValue;
    /** 领取后有效天数，为空表示领取后永不过期 */
    private Integer validDays;
    /** 生效时间（已废弃，保留兼容） */
    private LocalDateTime effectiveTime;
    /** 过期时间（已废弃，保留兼容） */
    private LocalDateTime expireTime;
    /** 发放数量 */
    private Long totalQuantity;
    /** 已发放数量 */
    private Long issuedQuantity;
    /** 开始发放时间 */
    private LocalDateTime distributionStartTime;
    /** 结束发放时间 */
    private LocalDateTime distributionEndTime;
    /** 发放控制状态：0-禁用 1-未开始 2-发放中 3-已结束 4-手动停止 */
    private Integer distributionStatus;
    /** 店铺名称 */
    private String shopName;
    /** 创建时间 */
    private LocalDateTime createTime;

    private int pageNum = 1;
    private int pageSize = 50;
}
