package com.xcz.member.customer.domain.dto.points;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会员自助购买商品请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsPurchaseDTO {

    /** 店铺 ID */
    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    /** 用户持券 ID（可选） */
    private Long userCouponId;

    /** 消费商品明细 */
    @NotEmpty(message = "请选择商品")
    @Valid
    private List<PointsConsumeItemDTO> items;

    /**
     * 幂等请求 ID（客户端生成的 UUID，防重复提交）。
     */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
