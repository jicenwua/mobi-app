package com.xcz.member.customer.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员付款码
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayQrcodeVO {

    /** 付款码 token（短时有效） */
    private String token;
    /** 过期时间戳（毫秒） */
    private Long expireAt;
    /** 二维码内容（MOBI:PAY:token，供扫码识别） */
    private String qrContent;
}
