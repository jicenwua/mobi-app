package com.xcz.member.customer.application.command.activity;

/**
 * 活动列表查询命令（店员/店长端）。
 *
 * @param shopId       店铺 ID
 * @param kind         活动种类筛选，可为 null
 * @param pageNum      页码
 * @param pageSize     每页条数
 * @param status       活动状态筛选，可为 null
 * @param activityType 满赠子类型筛选，可为 null
 */
public record QueryActivityCommand(
        Long shopId,
        Integer kind,
        Integer pageNum,
        Integer pageSize,
        Integer status,
        Integer activityType
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
