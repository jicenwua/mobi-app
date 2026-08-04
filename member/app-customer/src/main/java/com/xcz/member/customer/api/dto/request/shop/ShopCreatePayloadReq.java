package com.xcz.member.customer.api.dto.request.shop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增店铺表单元数据（与 multipart 中的图片分开发送）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopCreatePayloadReq {
    /***父店铺编码**/
    private String parentShopCode;
    /***店铺名称**/
    private String shopName;
    /***联系电话**/
    private String phone;
    /***行业类目ID**/
    private Integer categoryId;
    /***省**/
    private String province;
    /***市**/
    private String city;
    /***区县**/
    private String district;
    /***详细地址**/
    private String address;
    /***法人姓名**/
    private String legalPerson;
    /***法人身份证号**/
    private String idCardNo;
    /***统一社会信用代码**/
    private String businessLicenseNo;
    /***积分兑换比率（1元×比率=充值1元所得积分）**/
    private Integer ratio;
    /***店长小程序用户 ID**/
    private Long managerUserId;
}
