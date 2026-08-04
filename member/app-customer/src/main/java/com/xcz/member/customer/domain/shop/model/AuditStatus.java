package com.xcz.member.customer.domain.shop.model;

import com.xcz.member.customer.domain.shop.exception.ShopDomainException;
import lombok.Getter;

/**
 * 店铺审核状态
 */
@Getter
public enum AuditStatus {
    PENDING(0),
    APPROVED(1),
    REJECTED(2);

    private final int code;

    AuditStatus(int code) {
        this.code = code;
    }

    public static AuditStatus fromCode(int code) {
        for (AuditStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new ShopDomainException("无效的审核状态: " + code);
    }
}
