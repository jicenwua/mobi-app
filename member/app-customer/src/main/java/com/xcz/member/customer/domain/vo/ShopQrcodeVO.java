package com.xcz.member.customer.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 店铺邀请小程序码
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopQrcodeVO {

    /** 店铺唯一编码 */
    private String shopCode;
    /** 小程序码图片（Base64 Data URL） */
    private String imageBase64;
    /** 邀请码过期时间戳（毫秒） */
    private Long expireAt;
    /** 备用二维码内容（MOBI:SHOP:INV:token） */
    private String inviteContent;
}
