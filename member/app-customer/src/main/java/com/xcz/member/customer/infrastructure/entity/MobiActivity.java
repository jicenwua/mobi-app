package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店铺积分活动主表 mobi_activity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("mobi_activity")
public class MobiActivity implements Serializable {
    private static final long serialVersionUID = 1L;

    /***活动唯一流水ID(主键)**/
    @TableId(type = IdType.AUTO)
    private Long activityId;
    /***所属店铺ID**/
    private Long shopId;
    /***活动名称**/
    private String activityName;
    /***活动描述（列表展示、公告正文）**/
    private String description;
    /***活动种类：1-满赠活动，2-公告**/
    private Integer kind;
    /***满赠子类型：1-充值满赠，2-消费满赠（公告时为空）**/
    private Integer activityType;
    /***活动开始时间**/
    private LocalDateTime startTime;
    /***活动结束时间（NULL 表示永久有效）**/
    private LocalDateTime endTime;
    /***活动状态：0-未开始，1-进行中，2-已结束，3-手动停止**/
    private Integer status;
    /***创建时间**/
    private LocalDateTime createTime;
    /***修改时间**/
    private LocalDateTime updateTime;
}
