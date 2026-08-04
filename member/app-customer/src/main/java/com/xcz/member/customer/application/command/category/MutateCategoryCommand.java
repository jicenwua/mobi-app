package com.xcz.member.customer.application.command.category;

/**
 * 商品分类写操作命令载体（新增、更新共用，字段校验由 Assembler 按场景执行）。
 *
 * @param categoryId   分类 ID，新增时为 null
 * @param shopId       店铺 ID，更新时为 null
 * @param categoryName 分类名称
 * @param sortOrder    排序序号
 */
public record MutateCategoryCommand(
        Long categoryId,
        Long shopId,
        String categoryName,
        Integer sortOrder
) {
}
