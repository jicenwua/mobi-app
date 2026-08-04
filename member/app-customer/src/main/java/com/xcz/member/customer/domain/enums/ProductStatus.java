package com.xcz.member.customer.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品售卖状态。
 */
@Getter
@AllArgsConstructor
public enum ProductStatus {

    /** 售完（库存耗尽） */
    SOLD_OUT(0, "售完"),

    /** 正在出售 */
    ON_SALE(1, "出售中"),

    /** 手动下架 */
    OFF_SHELF(2, "下架"),
    ;

    private final int code;
    private final String description;

    public static ProductStatus getByCode(Integer code) {
        if (code == null) {
            return ON_SALE;
        }
        for (ProductStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return ON_SALE;
    }
}
