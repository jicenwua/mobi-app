package com.xcz.member.customer.application.command.coupon;

/**
 * 用户持券列表查询命令。
 *
 * @param shopId   店铺 ID，可为 null 表示全部店铺
 * @param status   持券状态筛选，可为 null
 * @param pageNum  页码
 * @param pageSize 每页条数
 */
public record QueryUserCouponCommand(
        Long shopId,
        Integer status,
        Integer pageNum,
        Integer pageSize
) {
    /**
     * 解析页码，未传或非法时默认 1。
     *
     * @return 页码
     */
    public int resolvedPageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    /**
     * 解析每页条数，未传或非法时默认 10。
     *
     * @return 每页条数
     */
    public int resolvedPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
