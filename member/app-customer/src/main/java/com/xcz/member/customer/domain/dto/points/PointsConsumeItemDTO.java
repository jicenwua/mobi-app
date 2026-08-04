package com.xcz.member.customer.domain.dto.points;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 扫码消费商品行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsConsumeItemDTO {

    /** 商品 ID */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 购买数量 */
    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量须大于0")
    private Integer count;
}
