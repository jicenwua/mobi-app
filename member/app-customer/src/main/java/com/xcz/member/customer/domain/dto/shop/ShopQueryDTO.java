package com.xcz.member.customer.domain.dto.shop;

import lombok.Data;

/**
 * 管理后台店铺分页查询条件
 */
@Data
public class ShopQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String shopName;
    private String legalPerson;
    private Integer auditStatus;
    private Integer isEnabled;
    private Integer categoryId;
    private String minCreateTime;
    private String maxCreateTime;
}
