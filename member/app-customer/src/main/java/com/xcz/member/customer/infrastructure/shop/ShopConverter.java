package com.xcz.member.customer.infrastructure.shop;

import com.xcz.member.customer.domain.shop.model.AuditStatus;
import com.xcz.member.customer.domain.shop.model.Shop;
import com.xcz.member.customer.domain.shop.model.ShopCertificates;
import com.xcz.member.customer.infrastructure.entity.MobiShop;

/**
 * 店铺聚合根与持久化实体转换
 */
public final class ShopConverter {

    private ShopConverter() {
    }

    /**
     * 将店铺聚合根转换为持久化实体。
     *
     * @param shop 店铺聚合根
     * @return 持久化实体
     */
    public static MobiShop toPo(Shop shop) {
        MobiShop po = new MobiShop();
        po.setId(shop.getId());
        po.setParentId(shop.getParentId());
        po.setShopName(shop.getShopName());
        po.setShopCode(shop.getShopCode());
        po.setRatio(shop.getRatio());
        po.setPhone(shop.getPhone());
        po.setCategoryId(shop.getCategoryId());
        po.setProvince(shop.getProvince());
        po.setCity(shop.getCity());
        po.setDistrict(shop.getDistrict());
        po.setAddress(shop.getAddress());
        po.setLegalPerson(shop.getLegalPerson());
        po.setIdCardNo(shop.getIdCardNo());
        po.setBusinessLicenseNo(shop.getBusinessLicenseNo());

        ShopCertificates certs = shop.getCertificates();
        if (certs != null) {
            po.setIdCardFront(certs.idCardFront());
            po.setIdCardBack(certs.idCardBack());
            po.setBusinessLicensePic(certs.businessLicensePic());
            po.setShopExterior(certs.shopExterior());
            po.setShopInterior(certs.shopInterior());
            po.setPicture(certs.carouselPictures());
        }

        po.setAuditStatus(shop.getAuditStatus().getCode());
        po.setAuditReason(shop.getAuditReason());
        po.setAuditTime(shop.getAuditTime());
        po.setAuditId(shop.getAuditId());
        po.setIsEnabled(shop.isEnabled() ? 1 : 0);
        po.setCreateTime(shop.getCreateTime());
        po.setUpdateTime(shop.getUpdateTime());
        po.setDelFlag(0);
        return po;
    }

    /**
     * 将持久化实体还原为店铺聚合根。
     *
     * @param po 持久化实体
     * @return 店铺聚合根；实体为 null 时返回 null
     */
    public static Shop toDomain(MobiShop po) {
        if (po == null) {
            return null;
        }
        ShopCertificates certificates = new ShopCertificates(
                po.getIdCardFront(),
                po.getIdCardBack(),
                po.getBusinessLicensePic(),
                po.getShopExterior(),
                po.getShopInterior(),
                po.getPicture()
        );
        AuditStatus auditStatus = po.getAuditStatus() == null
                ? AuditStatus.PENDING
                : AuditStatus.fromCode(po.getAuditStatus());
        boolean enabled = po.getIsEnabled() == null || po.getIsEnabled() == 1;
        return Shop.restore(
                po.getId(),
                po.getParentId(),
                po.getShopName(),
                po.getShopCode(),
                po.getRatio(),
                po.getPhone(),
                po.getCategoryId(),
                po.getProvince(),
                po.getCity(),
                po.getDistrict(),
                po.getAddress(),
                po.getLegalPerson(),
                po.getIdCardNo(),
                po.getBusinessLicenseNo(),
                certificates,
                auditStatus,
                po.getAuditReason(),
                po.getAuditTime(),
                po.getAuditId(),
                enabled,
                po.getCreateTime(),
                po.getUpdateTime()
        );
    }
}
