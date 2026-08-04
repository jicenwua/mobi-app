package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分变动流水表 mobi_points_log
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("mobi_points_log")
public class MobiPointsLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /***日志ID**/
    @TableId(type = IdType.AUTO)
    private Long logId;
    /***用户ID**/
    private Long userId;
    /***店铺ID**/
    private Long shopId;
    /***关联折扣券ID**/
    private Long couponId;
    /***动作类型：1-增加 2-消耗**/
    private Integer actionType;
    /***变更前基础积分**/
    private BigDecimal preBasePoints;
    /***变更前赠送积分**/
    private BigDecimal preBonusPoints;
    /***基础积分变动值**/
    private BigDecimal changeBase;
    /***赠送积分变动值**/
    private BigDecimal changeBonus;
    /***变更后基础积分**/
    private BigDecimal afterBasePoints;
    /***变更后赠送积分**/
    private BigDecimal afterBonusPoints;
    /***状态：1-消费成功 2-订单取消 3-订单过期 4-积分充值 6-后台添加 7-后台扣除**/
    private Integer status;
    /***备注（后台调整等）**/
    private String remark;
    /***创建时间**/
    private LocalDateTime createTime;
    /***消费时间**/
    private LocalDateTime consumeTime;
    /***幂等请求 ID（客户端 UUID，防重复提交）**/
    private String requestId;
}
