package com.xcz.member.feign.dto.points;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 消费小票商品行（Feign 传输对象）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsReceiptItemFeign implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /***小票行ID**/
    private Long receiptId;
    /***关联流水ID**/
    private Long logId;
    /***商品名称**/
    private String productName;
    /***购买数量**/
    private Integer count;
    /***商品单价**/
    private BigDecimal price;
}
