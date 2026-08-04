package com.xcz.member.customer.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 积分流水列表/详情读模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsLogBriefVO {

    /***流水ID**/
    private Long logId;
    /***店铺ID**/
    private Long shopId;
    /***店铺名称**/
    private String shopName;
    /***用户ID**/
    private Long userId;
    /***会员昵称**/
    private String nickname;
    /***会员手机号**/
    private String phone;
    /***动作类型：1-增加 2-消耗**/
    private Integer actionType;
    /***变更前基础积分**/
    private Integer preBasePoints;
    /***变更前赠送积分**/
    private Integer preBonusPoints;
    /***基础积分变动值**/
    private Integer changeBase;
    /***赠送积分变动值**/
    private Integer changeBonus;
    /***变更后基础积分**/
    private Integer afterBasePoints;
    /***变更后赠送积分**/
    private Integer afterBonusPoints;
    /***状态：1-消费成功 2-订单取消 3-订单过期 4-积分充值 6-后台添加 7-后台扣除**/
    private Integer status;
    /***备注**/
    private String remark;
    /***关联优惠券ID**/
    private Long couponId;
    /***优惠券名称**/
    private String couponName;
    /***优惠券类型：1-折扣 2-满减**/
    private Integer couponType;
    /***创建时间**/
    private LocalDateTime createTime;
    /***消费时间**/
    private LocalDateTime consumeTime;
    /***本次消费积分**/
    private Integer consumePoints;
    /***消费商品明细**/
    private List<PointsReceiptItemVO> items;
}
