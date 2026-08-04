package com.xcz.member.customer.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用查询 DTO 基类，提供时间范围与分页默认值。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseReq {

    /** 查询起始时间 */
    private String minTime;
    /** 查询结束时间 */
    private String maxTime;
    /** 页码，默认 1 */
    private Integer pageNum = 1;
    /** 每页条数，默认 2 */
    private Integer pageSize = 2;

    /**
     * 获取有效页码（最小为 1）。
     *
     * @return 页码
     */
    public Integer getPageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    /**
     * 获取有效每页条数（最小为 2）。
     *
     * @return 每页条数
     */
    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 2 : pageSize;
    }
}
