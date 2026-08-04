package com.xcz.member.customer.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 付款码校验结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayQrcodeVerifyVO {

    /** 会员用户 ID */
    private Long userId;
    /** 会员昵称 */
    private String nickname;
    /** 会员手机号 */
    private String phone;
    /** 在当前店铺的剩余积分 */
    private Integer remainingPoints;
}
