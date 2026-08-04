package com.xcz.member.customer.domain.dto.points;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 店铺维度积分账户 / 流水查询条件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsShopQuery {

    /***店铺ID**/
    private Long shopId;
    /***用户ID**/
    private Long userId;
    /***店铺名称（模糊查询）**/
    private String shopName;
    /***会员昵称（模糊查询）**/
    private String nickname;
    /***会员手机号（模糊查询）**/
    private String phone;
    /***统一搜索关键词**/
    private String keyword;
    /***关键词是否为纯数字（为 true 时同时匹配手机号并优先排序）**/
    private Boolean keywordSearchPhone;
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
    /***排除指定状态**/
    private Integer excludeStatus;
    /***账户列表排序：1-剩余积分降序（默认）2-剩余积分正序 3-累计已用降序 4-累计已用正序**/
    private Integer sortType = 1;
    /***页码**/
    private Integer pageNum = 1;
    /***每页条数**/
    private Integer pageSize = 10;

    /**
     * 获取有效页码（最小为 1）。
     */
    public int getPageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    /**
     * 获取有效每页条数（最小为 10）。
     */
    public int getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
