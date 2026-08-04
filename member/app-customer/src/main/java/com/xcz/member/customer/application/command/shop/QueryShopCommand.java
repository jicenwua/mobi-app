package com.xcz.member.customer.application.command.shop;

/**
 * 店铺列表查询命令。
 */
public record QueryShopCommand(
        Integer pageNum,
        Integer pageSize
) {

    public QueryShopCommand {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
    }
}
