package com.xcz.member.customer.api.dto.request.point;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 店员/店长线下充值积分请求参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsRechargeReq {

    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    @NotNull(message = "会员ID不能为空")
    private Long userId;

    /** 充值金额（元），基础积分 = 金额 × 店铺比率 */
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额须大于 0")
    private BigDecimal amountYuan;

    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
