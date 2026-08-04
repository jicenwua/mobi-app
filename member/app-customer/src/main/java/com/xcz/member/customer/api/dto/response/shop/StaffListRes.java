package com.xcz.member.customer.api.dto.response.shop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 店铺店员列表项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffListRes {

    private Long userId;
    private String nickname;
    private String phone;
    private LocalDateTime createTime;
}
