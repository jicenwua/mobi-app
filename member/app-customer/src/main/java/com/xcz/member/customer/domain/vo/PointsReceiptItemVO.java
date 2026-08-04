package com.xcz.member.customer.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 消费小票商品行读模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsReceiptItemVO {

    /***小票行ID**/
    private Long receiptId;
    /***关联流水ID**/
    private Long logId;
    /***商品ID**/
    private Long productId;
    /***商品名称**/
    private String productName;
    /***购买数量**/
    private Integer count;
    /***商品单价**/
    private BigDecimal price;
}
