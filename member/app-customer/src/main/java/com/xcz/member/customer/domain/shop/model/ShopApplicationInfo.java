package com.xcz.member.customer.domain.shop.model;

import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.customer.api.dto.request.shop.ShopCreatePayloadReq;
import com.xcz.member.customer.utils.IdCardUtils;

/**
 * 提交入驻申请时的店铺基础信息
 */
public record ShopApplicationInfo(
        Long parentId,
        String shopName,
        String phone,
        Integer categoryId,
        String province,
        String city,
        String district,
        String address,
        String legalPerson,
        String idCardNo,
        String businessLicenseNo,
        Integer ratio
) {

    public static ShopApplicationInfo of(ShopCreatePayloadReq payload,Long parentId){
        return new ShopApplicationInfo(
                parentId != null ? parentId : 0L,
                StringUtils.trim(payload.getShopName()),
                StringUtils.trim(payload.getPhone()),
                payload.getCategoryId(),
                StringUtils.trimToNull(payload.getProvince()),
                StringUtils.trimToNull(payload.getCity()),
                StringUtils.trimToNull(payload.getDistrict()),
                StringUtils.trim(payload.getAddress()),
                StringUtils.trim(payload.getLegalPerson()),
                IdCardUtils.normalizeIdNumber(payload.getIdCardNo()),
                StringUtils.trim(payload.getBusinessLicenseNo()),
                payload.getRatio()
        );

    }
}
