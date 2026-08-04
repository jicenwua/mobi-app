package com.xcz.member.feign.dto.shop;

import lombok.Data;

/**
 * 店铺审核提交（Feign 传输对象）
 */
@Data
public class ShopAuditFeign {
    private Long id;
    private Integer auditStatus;
    private String auditReason;
}
