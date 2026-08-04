package com.xcz.member.customer.api.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信小程序登录请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WxLoginReq {
    /***微信登录code**/
    @NotBlank(message = "微信code不能为空")
    private String code;
    /***手机号授权code**/
    private String phoneCode;
}
