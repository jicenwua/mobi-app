package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动满赠规则明细表 mobi_activity_rule
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("mobi_activity_rule")
public class MobiActivityRule implements Serializable {
    private static final long serialVersionUID = 1L;

    /***规则唯一流水ID(主键)**/
    @TableId(type = IdType.AUTO)
    private Long ruleId;
    /***关联的活动主表ID**/
    private Long activityId;
    /***满足条件的门槛积分**/
    private Integer thresholdAmount;
    /***满足门槛后赠送的积分数量**/
    private Integer giftPoints;
    /***创建时间**/
    private LocalDateTime createTime;
}
