package com.xcz.member.customer.api.dto.response.shop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加店员结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffAddRes {

    private Long userId;
    private String nickname;
}
