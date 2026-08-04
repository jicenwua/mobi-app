package com.xcz.member.feign.dto.shop;

import lombok.Data;

/**
 * 新增店铺表单元数据（Feign 传输对象，与 multipart 中的图片分开发送）
 */
@Data
public class ShopCreateFeign {
    private String parentShopCode;
    private String shopName;
    private String phone;
    private Integer categoryId;
    private String province;
    private String city;
    private String district;
    private String address;
    private String legalPerson;
    private String idCardNo;
    private String businessLicenseNo;
    private Integer ratio;
    /** 店长小程序用户 ID（须已注册 mobi_user） */
    private Long managerUserId;
}
