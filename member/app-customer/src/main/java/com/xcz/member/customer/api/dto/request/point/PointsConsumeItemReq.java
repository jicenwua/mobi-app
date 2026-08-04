package com.xcz.member.customer.api.dto.request.point;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 扫码消费商品行请求参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsConsumeItemReq {

    /** 商品 ID */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 购买数量 */
    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量须大于0")
    private Integer count;
}
