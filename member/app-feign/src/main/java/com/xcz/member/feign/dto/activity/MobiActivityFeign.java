package com.xcz.member.feign.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店铺积分活动查询/列表 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobiActivityFeign implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long activityId;
    private Long shopId;
    private String shopName;
    private String activityName;
    /***活动类型：1-充值满赠，2-消费满赠**/
    private Integer activityType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /***活动状态：0-未开始，1-进行中，2-已结束，3-手动停止**/
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private int pageNum = 1;
    private int pageSize = 10;
}
