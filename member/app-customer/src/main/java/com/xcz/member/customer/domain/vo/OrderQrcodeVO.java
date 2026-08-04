package com.xcz.member.customer.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单核销二维码
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderQrcodeVO {

    /** 订单码 token */
    private String token;
    /** 过期时间戳（毫秒） */
    private Long expireAt;
    /** 二维码内容（MOBI:ORDER:token） */
    private String qrContent;
}
