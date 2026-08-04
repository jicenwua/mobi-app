package com.xcz.member.customer.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 店员邀请码（用户出示，店长扫码添加）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffInviteQrcodeVO {

    /** 邀请 token（短时有效） */
    private String token;
    /** 过期时间戳（毫秒） */
    private Long expireAt;
    /** 二维码内容（MOBI:STAFF:INV:token） */
    private String qrContent;
}
