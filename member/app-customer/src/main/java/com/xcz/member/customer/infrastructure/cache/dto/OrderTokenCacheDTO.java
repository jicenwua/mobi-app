package com.xcz.member.customer.infrastructure.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单核销码 token Redis 缓存 payload。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTokenCacheDTO implements Serializable {

    private Long logId;
    private Long shopId;
    private Long userId;
}
