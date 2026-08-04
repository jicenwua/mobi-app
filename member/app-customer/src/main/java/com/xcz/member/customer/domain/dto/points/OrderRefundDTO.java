package com.xcz.member.customer.domain.dto.points;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 会员自助退款请求
 */
@Data
public class OrderRefundDTO {

    @NotNull(message = "订单ID不能为空")
    private Long logId;

    @NotNull(message = "店铺ID不能为空")
    private Long shopId;
}
