package com.xcz.member.customer.api.dto.response.point;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 消费详情商品行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeReceiptItemRes {

    /***小票行ID**/
    private Long receiptId;
    /***商品名称**/
    private String productName;
    /***购买数量**/
    private Integer count;
    /***商品单价**/
    private BigDecimal price;
    /***该行折算积分（单价 × 比例 × 数量）**/
    private Integer linePoints;
}
