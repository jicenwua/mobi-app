package com.xcz.member.customer.api.dto.request.point;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 店员扫码核销订单请求参数。
 */
@Data
public class OrderVerifyReq {

    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    @NotBlank(message = "订单码无效")
    private String token;
}
