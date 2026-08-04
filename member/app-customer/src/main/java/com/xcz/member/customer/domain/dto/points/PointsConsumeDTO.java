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
 * 店员扫码扣减积分请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsConsumeDTO {

    /** 店铺 ID */
    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    /** 顾客付款码 token */
    @NotBlank(message = "付款码无效")
    private String token;

    /** 优惠券 ID（可选） */
    private Long couponId;

    /** 消费商品明细 */
    @NotEmpty(message = "请选择商品")
    @Valid
    private List<PointsConsumeItemDTO> items;

    /**
     * 幂等请求 ID（客户端 UUID，防重复扣款）。
     */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
