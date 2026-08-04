package com.xcz.member.customer.domain.shop.model;

import com.xcz.member.customer.domain.shop.exception.ShopDomainException;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 店铺聚合根：负责入驻申请与审核状态流转
 */
@Getter
public class Shop {

    private Long id;
    private Long parentId;
    private String shopName;
    private String shopCode;
    private Integer ratio;
    private String phone;
    private Integer categoryId;
    private String province;
    private String city;
    private String district;
    private String address;
    private String legalPerson;
    private String idCardNo;
    private String businessLicenseNo;
    private ShopCertificates certificates;
    private AuditStatus auditStatus;
    private String auditReason;
    private LocalDateTime auditTime;
    private Long auditId;
    private boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Shop() {
    }

    /**
     * 提交入驻申请（初始状态：待审核）
     */
    public static Shop submitApplication(ShopApplicationInfo info,
                                         ShopCertificates certificates,
                                         String shopCode) {
        Shop shop = new Shop();
        shop.parentId = info.parentId() != null ? info.parentId() : 0L;
        shop.shopName = info.shopName();
        shop.phone = info.phone();
        shop.categoryId = info.categoryId();
        shop.province = info.province();
        shop.city = info.city();
        shop.district = info.district();
        shop.address = info.address();
        shop.legalPerson = info.legalPerson();
        shop.idCardNo = info.idCardNo();
        shop.businessLicenseNo = info.businessLicenseNo();
        shop.ratio = info.ratio();
        shop.certificates = certificates;
        shop.shopCode = shopCode;
        shop.auditStatus = AuditStatus.PENDING;
        shop.enabled = true;
        shop.auditReason = null;
        shop.auditTime = null;
        shop.auditId = null;
        shop.createTime = LocalDateTime.now();
        return shop;
    }

    /**
     * 审核通过：仅待审店铺可操作；通过后重新生成店铺编码
     */
    public void approve(Long auditorId) {
        assertPending("审核通过");
        this.auditStatus = AuditStatus.APPROVED;
        this.auditReason = null;
        this.auditTime = LocalDateTime.now();
        this.auditId = auditorId;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 审核驳回：仅待审店铺可操作；驳回原因必填
     */
    public void reject(Long auditorId, String reason) {
        assertPending("审核驳回");
        if (reason == null || reason.isBlank()) {
            throw new ShopDomainException("驳回时必须填写原因");
        }
        this.auditStatus = AuditStatus.REJECTED;
        this.auditReason = reason;
        this.auditTime = LocalDateTime.now();
        this.auditId = auditorId;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 校验是否可作为上级总店（已审核通过且无更上级）
     */
    public void assertApprovedHeadShop() {
        if (parentId != null && parentId != 0L) {
            throw new ShopDomainException("上级店铺必须是总店（无更上级店铺）");
        }
        if (auditStatus != AuditStatus.APPROVED) {
            throw new ShopDomainException("上级店铺须为已审核通过的总店");
        }
    }

    private void assertPending(String action) {
        if (auditStatus != AuditStatus.PENDING) {
            throw new ShopDomainException("仅待审店铺可" + action);
        }
    }

    public void bindId(Long id) {
        this.id = id;
    }

    public static Shop restore(Long id,
                        Long parentId,
                        String shopName,
                        String shopCode,
                        Integer ratio,
                        String phone,
                        Integer categoryId,
                        String province,
                        String city,
                        String district,
                        String address,
                        String legalPerson,
                        String idCardNo,
                        String businessLicenseNo,
                        ShopCertificates certificates,
                        AuditStatus auditStatus,
                        String auditReason,
                        LocalDateTime auditTime,
                        Long auditId,
                        boolean enabled,
                        LocalDateTime createTime,
                        LocalDateTime updateTime) {
        Shop shop = new Shop();
        shop.id = id;
        shop.parentId = parentId;
        shop.shopName = shopName;
        shop.shopCode = shopCode;
        shop.ratio = ratio;
        shop.phone = phone;
        shop.categoryId = categoryId;
        shop.province = province;
        shop.city = city;
        shop.district = district;
        shop.address = address;
        shop.legalPerson = legalPerson;
        shop.idCardNo = idCardNo;
        shop.businessLicenseNo = businessLicenseNo;
        shop.certificates = certificates;
        shop.auditStatus = auditStatus;
        shop.auditReason = auditReason;
        shop.auditTime = auditTime;
        shop.auditId = auditId;
        shop.enabled = enabled;
        shop.createTime = createTime;
        shop.updateTime = updateTime;
        return shop;
    }
}
