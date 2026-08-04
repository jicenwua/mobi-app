package com.xcz.member.customer.api.dto.request.point;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 会员自助退款请求参数。
 */
@Data
public class OrderRefundReq {

    @NotNull(message = "订单ID不能为空")
    private Long logId;

    @NotNull(message = "店铺ID不能为空")
    private Long shopId;
}
