package com.xcz.member.customer.application.command.product;

/**
 * 扣减商品库存命令（内部下单场景，校验由 Assembler 执行）。
 *
 * @param productId 商品 ID
 * @param quantity  扣减数量
 */
public record DeductStockCommand(Long productId, int quantity) {
}
