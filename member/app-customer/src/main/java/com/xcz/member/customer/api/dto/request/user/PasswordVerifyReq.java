package com.xcz.member.customer.api.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 校验支付密码请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordVerifyReq {
    /** 6 位数字支付密码 */
    @NotBlank(message = "请输入支付密码")
    @Pattern(regexp = "^\\d{6}$", message = "支付密码须为 6 位数字")
    private String password;
}
