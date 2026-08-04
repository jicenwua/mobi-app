package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户消费小票明细表 mobi_receipt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("mobi_receipt")
public class MobiReceipt implements Serializable {
    private static final long serialVersionUID = 1L;

    /***小票流水ID(主键)**/
    @TableId(type = IdType.AUTO)
    private Long receiptId;
    /***关联积分流水ID**/
    private Long logId;
    /***商品ID（下单时快照，退款回滚库存用）**/
    private Long productId;
    /***购买的商品名（下单时快照）**/
    private String productName;
    /***购买数量**/
    private Integer count;
    /***商品单价**/
    private BigDecimal price;
    /***购买的用户ID**/
    private Long userId;
    /***小票生成时间/购买时间**/
    private LocalDateTime createTime;
}
