package com.xcz.member.feign.dto.points;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 店铺维度积分账户 / 积分流水查询参数（Feign 传输对象）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsShopFeignQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /***店铺ID**/
    private Long shopId;
    /***用户ID**/
    private Long userId;
    /***店铺名称（模糊查询）**/
    private String shopName;
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
    /***状态：1-消费成功 2-订单取消 3-订单过期 4-积分充值**/
    private Integer status;
    /***会员昵称（模糊查询）**/
    private String nickname;
    /***会员手机号（模糊查询）**/
    private String phone;
    /***账户列表排序：1-剩余积分降序（默认）2-剩余积分正序 3-累计已用降序 4-累计已用正序**/
    private Integer sortType;
    /***页码**/
    private int pageNum = 1;
    /***每页条数**/
    private int pageSize = 10;
}
