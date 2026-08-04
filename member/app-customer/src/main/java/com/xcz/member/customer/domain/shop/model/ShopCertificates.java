package com.xcz.member.customer.domain.shop.model;

/**
 * 店铺证照与门店图片
 */
public record ShopCertificates(
        String idCardFront,
        String idCardBack,
        String businessLicensePic,
        String shopExterior,
        String shopInterior,
        String carouselPictures
) {
}
