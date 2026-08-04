package com.xcz.member.customer.application.command.product;

/**
 * 商品查询命令。
 *
 * @param shopId      店铺 ID
 * @param categoryId  分类 ID 筛选，可为 null
 * @param productName 商品名称模糊筛选，可为 null
 * @param status      售卖状态筛选，可为 null
 * @param pageNum     页码
 * @param pageSize    每页条数
 */
public record QueryProductCommand(
        Long shopId,
        Long categoryId,
        String productName,
        Integer status,
        int pageNum,
        int pageSize
) {
}
