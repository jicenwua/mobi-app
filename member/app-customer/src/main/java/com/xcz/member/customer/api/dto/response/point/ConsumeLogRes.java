package com.xcz.member.customer.api.dto.response.point;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小程序消费记录列表项（仅展示汇总积分，不拆分基础/赠送）。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeLogRes {

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
    /***状态：1-消费成功 2-订单取消 3-订单过期 4-积分充值 6-后台添加 7-后台扣除**/
    private Integer status;
    /***备注**/
    private String remark;
    /***创建时间**/
    private LocalDateTime createTime;
    /***消费时间**/
    private LocalDateTime consumeTime;
    /***本次消费积分**/
    private Integer consumePoints;
    /***交易后剩余积分（基础 + 赠送）**/
    private Integer remainingPoints;
    /***关联优惠券ID**/
    private Long couponId;
    /***消费商品明细（列表接口按需返回）**/
    private List<ConsumeReceiptItemRes> items;
}
