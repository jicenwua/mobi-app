package com.xcz.member.customer.api.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设置或修改支付密码请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordSetReq {
    /***原密码（已设置支付密码时必填）**/
    private String oldPassword;
    /***新密码（6 位数字）**/
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
