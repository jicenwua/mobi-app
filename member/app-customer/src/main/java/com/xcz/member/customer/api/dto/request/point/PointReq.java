package com.xcz.member.customer.api.dto.request.point;

import com.xcz.member.customer.api.dto.request.BaseReq;
import lombok.*;

/**
 * 积分账户 / 消费流水查询参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PointReq extends BaseReq {
    /***店铺ID**/
    private Long shopId;
    /***状态：1-消费成功 2-订单取消 3-订单过期 4-积分充值 6-后台添加 7-后台扣除**/
    private Integer status;
    /***排除指定状态（与 status 互斥）**/
    private Integer excludeStatus;
    /***动作类型：1-增加 2-消耗**/
    private Integer actionType;
    /***排序：1-剩余积分降序（默认）2-剩余积分正序 3-累计已用降序 4-累计已用正序**/
    private Integer sortType;
    /***会员昵称（模糊查询）**/
    private String nickname;
    /***会员手机号（模糊查询）**/
    private String phone;
    /***统一搜索关键词（名称模糊；纯数字时同时匹配手机号且手机号命中优先）**/
    private String keyword;
}
