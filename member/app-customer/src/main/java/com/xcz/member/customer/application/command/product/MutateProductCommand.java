package com.xcz.member.customer.application.command.product;

import java.math.BigDecimal;

/**
 * 商品写操作命令载体（新增、更新共用，字段校验由 Assembler 按场景执行）。
 *
 * @param productId   商品 ID，新增时为 null
 * @param shopId      店铺 ID，更新时为 null
 * @param categoryId  分类 ID
 * @param productName 商品名称
 * @param description 商品描述
 * @param imageUrl    商品图片地址
 * @param price       商品单价
 * @param status      售卖状态：0-售完 1-出售中
 * @param stock       库存数量
 */
public record MutateProductCommand(
        Long productId,
        Long shopId,
        Long categoryId,
        String productName,
        String description,
        String imageUrl,
        BigDecimal price,
        Integer status,
        Long stock
) {
    /**
     * 解析库存值，未传时默认为 0。
     *
     * @return 库存数量
     */
    public long resolvedStock() {
        return stock == null ? 0L : stock;
    }

    /**
     * 解析售卖状态，未传时默认为出售中（1）。
     *
     * @return 售卖状态编码
     */
    public int resolvedStatus() {
        return status == null ? 1 : status;
    }
}
