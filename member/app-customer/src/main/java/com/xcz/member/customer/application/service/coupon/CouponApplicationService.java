package com.xcz.member.customer.application.service.coupon;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.application.command.coupon.MutateCouponTemplateCommand;
import com.xcz.member.customer.domain.enums.CouponDistributionStatus;
import com.xcz.member.customer.domain.service.MobiCouponTemplateService;
import com.xcz.member.customer.domain.service.MobiShopUserService;
import com.xcz.member.customer.domain.service.MobiUserCouponsService;
import com.xcz.member.customer.domain.service.MobiUserService;
import com.xcz.member.customer.infrastructure.cache.CouponStockCache;
import com.xcz.member.customer.infrastructure.entity.MobiCouponTemplate;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import com.xcz.member.customer.infrastructure.entity.MobiUserCoupons;
import com.xcz.member.customer.utils.ShopAccessUtils;
import com.xcz.member.feign.dto.coupon.MobiUserCouponGrantFeign;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券写侧应用服务。
 */
@Service
public class CouponApplicationService {

    @Resource
    private MobiCouponTemplateService mobiCouponTemplateService;
    @Resource
    private MobiUserCouponsService mobiUserCouponsService;
    @Resource
    private MobiShopUserService mobiShopUserService;
    @Resource
    private MobiUserService mobiUserService;

    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(MutateCouponTemplateCommand command) {
        ShopAccessUtils.assertShopManager(mobiShopUserService, command.shopId());
        LocalDateTime now = LocalDateTime.now();
        MobiCouponTemplate statusProbe = MobiCouponTemplate.builder()
                .distributionStartTime(command.distributionStartTime())
                .distributionEndTime(command.distributionEndTime())
                .issuedQuantity(0L)
                .totalQuantity(command.totalQuantity())
                .build();
        MobiCouponTemplate template = MobiCouponTemplate.builder()
                .shopId(command.shopId())
                .couponName(command.couponName())
                .type(command.type())
                .thresholdAmount(command.thresholdAmount() == null ? BigDecimal.ZERO : command.thresholdAmount())
                .discountValue(command.discountValue())
                .validDays(command.validDays())
                .totalQuantity(command.totalQuantity())
                .issuedQuantity(0L)
                .distributionStartTime(command.distributionStartTime())
                .distributionEndTime(command.distributionEndTime())
                .distributionStatus(CouponDistributionStatus.resolve(statusProbe, now).getCode())
                .createTime(now)
                .build();
        mobiCouponTemplateService.save(template);
        return template.getTemplateId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(MutateCouponTemplateCommand command) {
        ShopAccessUtils.assertShopManager(mobiShopUserService, command.shopId());
        MobiCouponTemplate existing = loadAndAssertShop(command.templateId(), command.shopId());
        LocalDateTime now = LocalDateTime.now();
        boolean manuallyStopped = CouponDistributionStatus.isManuallyStoppedCode(existing.getDistributionStatus());
        CouponDistributionStatus status = CouponDistributionStatus.resolve(existing, now);

        if (manuallyStopped) {
            existing.setDistributionStartTime(command.distributionStartTime());
            existing.setDistributionEndTime(command.distributionEndTime());
        } else if (status == CouponDistributionStatus.NOT_STARTED || status == CouponDistributionStatus.DISABLED) {
            existing.setCouponName(command.couponName());
            existing.setType(command.type());
            existing.setThresholdAmount(command.thresholdAmount());
            existing.setDiscountValue(command.discountValue());
            existing.setValidDays(command.validDays());
            existing.setTotalQuantity(command.totalQuantity());
            existing.setDistributionStartTime(command.distributionStartTime());
            existing.setDistributionEndTime(command.distributionEndTime());
            if (existing.getDistributionStatus() == null
                    || CouponDistributionStatus.DISABLED.getCode() == existing.getDistributionStatus()) {
                existing.setDistributionStatus(CouponDistributionStatus.resolve(existing, now).getCode());
            }
        } else {
            existing.setDistributionStartTime(command.distributionStartTime());
            existing.setDistributionEndTime(command.distributionEndTime());
        }
        if (!manuallyStopped) {
            existing.setDistributionStatus(CouponDistributionStatus.resolve(existing, now).getCode());
        }
        mobiCouponTemplateService.updateById(existing);
    }

    /**
     * 调整发放时间（发放中、未开始或已手动停止时可修改，不改优惠规则与总量）。
     *
     * @param command 写操作命令
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjustDistribution(MutateCouponTemplateCommand command) {
        ShopAccessUtils.assertShopManager(mobiShopUserService, command.shopId());
        MobiCouponTemplate existing = loadAndAssertShop(command.templateId(), command.shopId());
        LocalDateTime now = LocalDateTime.now();
        CouponDistributionStatus status = CouponDistributionStatus.resolve(existing, now);
        if (status != CouponDistributionStatus.DISTRIBUTING
                && status != CouponDistributionStatus.NOT_STARTED
                && status != CouponDistributionStatus.MANUALLY_STOPPED) {
            throw new ServiceException("仅未开始、发放中或已手动停止的优惠券可调整发放时间", 400);
        }
        existing.setDistributionStartTime(command.distributionStartTime());
        existing.setDistributionEndTime(command.distributionEndTime());
        if (!CouponDistributionStatus.isManuallyStoppedCode(existing.getDistributionStatus())) {
            existing.setDistributionStatus(CouponDistributionStatus.resolve(existing, now).getCode());
        }
        mobiCouponTemplateService.updateById(existing);
    }

    /**
     * 追加优惠券库存（任意阶段均可追加，同步 Redis 与数据库总量）。
     *
     * @param command 写操作命令
     */
    @Transactional(rollbackFor = Exception.class)
    public void addStock(MutateCouponTemplateCommand command) {
        ShopAccessUtils.assertShopManager(mobiShopUserService, command.shopId());
        MobiCouponTemplate existing = loadAndAssertShop(command.templateId(), command.shopId());
        long addQuantity = command.addQuantity();
        long issued = existing.getIssuedQuantity() == null ? 0L : existing.getIssuedQuantity();
        if (CouponStockCache.isUnlimitedTotal(existing.getTotalQuantity())) {
            existing.setTotalQuantity(issued + addQuantity);
        } else {
            existing.setTotalQuantity(existing.getTotalQuantity() + addQuantity);
        }
        mobiCouponTemplateService.updateById(existing);
    }

    /**
     * 手动停止优惠券发放。
     *
     * @param templateId 模板 ID
     * @param shopId     店铺 ID（小程序端必传；管理后台可传 null）
     */
    @Transactional(rollbackFor = Exception.class)
    public void stopDistribution(Long templateId, Long shopId) {
        MobiCouponTemplate existing = resolveTemplate(templateId, shopId);
        ShopAccessUtils.assertShopManager(mobiShopUserService, existing.getShopId());
        existing.setDistributionStatus(CouponDistributionStatus.MANUALLY_STOPPED.getCode());
        mobiCouponTemplateService.updateById(existing);
    }

    /**
     * 恢复手动停止的优惠券发放（仅校验库存；时间由阶段解析自然体现为未开始/进行中/已结束）。
     *
     * @param templateId 模板 ID
     * @param shopId     店铺 ID（小程序端必传；管理后台可传 null）
     */
    @Transactional(rollbackFor = Exception.class)
    public void resumeDistribution(Long templateId, Long shopId) {
        MobiCouponTemplate existing = resolveTemplate(templateId, shopId);
        ShopAccessUtils.assertShopManager(mobiShopUserService, existing.getShopId());
        CouponDistributionStatus status = CouponDistributionStatus.resolve(existing, LocalDateTime.now());
        if (status != CouponDistributionStatus.MANUALLY_STOPPED) {
            throw new ServiceException("仅手动停止的优惠券可恢复发放", 400);
        }
        Long remaining = CouponDistributionStatus.remainingStock(existing);
        if (remaining != null && remaining <= 0L) {
            throw new ServiceException("库存已耗尽，请先追加库存", 400);
        }
        LocalDateTime now = LocalDateTime.now();
        existing.setDistributionStatus(CouponDistributionStatus.resolveFromTimeAndStock(
                existing.getDistributionStartTime(),
                existing.getDistributionEndTime(),
                existing.getIssuedQuantity(),
                existing.getTotalQuantity(),
                now).getCode());
        mobiCouponTemplateService.updateById(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeTemplate(Long templateId) {
        MobiCouponTemplate existing = requireTemplate(templateId);
        ShopAccessUtils.assertShopManager(mobiShopUserService, existing.getShopId());
        if (CouponDistributionStatus.isDistributing(existing, LocalDateTime.now())) {
            throw new ServiceException("发放中的优惠券不能删除，请先手动停止发放", 400);
        }
        mobiCouponTemplateService.removeById(templateId);
    }

    /**
     * 管理后台：向指定用户发放优惠券。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long grantUserCoupon(MobiUserCouponGrantFeign body) {
        if (body == null || body.getUserId() == null || body.getShopId() == null || body.getTemplateId() == null) {
            throw new ServiceException("用户、店铺与模板 ID 均不能为空", 400);
        }
        MobiUser user = mobiUserService.getById(body.getUserId());
        if (user == null) {
            throw new ServiceException("用户不存在", 404);
        }
        MobiCouponTemplate template = mobiCouponTemplateService.getById(body.getTemplateId());
        if (template == null || !template.getShopId().equals(body.getShopId())) {
            throw new ServiceException("优惠券不存在", 404);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = template.getValidDays() == null ? null : now.plusDays(template.getValidDays());
        MobiUserCoupons coupon = MobiUserCoupons.builder()
                .userId(body.getUserId())
                .shopId(body.getShopId())
                .templateId(body.getTemplateId())
                .status(0)
                .receiveTime(now)
                .expireTime(expireTime)
                .build();
        mobiUserCouponsService.save(coupon);
        template.setIssuedQuantity((template.getIssuedQuantity() == null ? 0L : template.getIssuedQuantity()) + 1L);
        mobiCouponTemplateService.updateById(template);
        return coupon.getUserCouponId();
    }

    /**
     * 管理后台：删除用户持券（未使用方可删）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeUserCoupon(Long userCouponId) {
        MobiUserCoupons coupon = mobiUserCouponsService.getById(userCouponId);
        if (coupon == null) {
            throw new ServiceException("用户优惠券不存在", 404);
        }
        if (coupon.getStatus() != null && coupon.getStatus() == 1) {
            throw new ServiceException("已使用的优惠券不可删除", 400);
        }
        mobiUserCouponsService.removeById(userCouponId);
    }

    /**
     * 用户领取优惠券（Redis 原子扣减库存，失败时回滚）。
     *
     * @param shopId     店铺 ID
     * @param templateId 券模板 ID
     * @return 用户持券 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long claim(Long shopId, Long templateId) {
        ShopAccessUtils.assertShopCustomer(mobiShopUserService, shopId);
        MobiCouponTemplate template = mobiCouponTemplateService.getById(templateId);
        if (template == null || !template.getShopId().equals(shopId)) {
            throw new ServiceException("优惠券不存在", 404);
        }
        if (!CouponDistributionStatus.isDistributing(template, LocalDateTime.now())) {
            throw new ServiceException("优惠券当前不可领取", 400);
        }
        Long userCouponId = mobiCouponTemplateService.claimForUser(template, SecurityUtils.getUserId());
        if (userCouponId == null) {
            throw new ServiceException("领取失败，可能已领完或您已领取过", 400);
        }
        return userCouponId;
    }

    private MobiCouponTemplate requireTemplate(Long templateId) {
        MobiCouponTemplate template = mobiCouponTemplateService.getById(templateId);
        if (template == null) {
            throw new ServiceException("优惠券不存在", 404);
        }
        return template;
    }

    private MobiCouponTemplate resolveTemplate(Long templateId, Long shopId) {
        return shopId != null ? loadAndAssertShop(templateId, shopId) : requireTemplate(templateId);
    }

    /** 加载券模板并校验店铺归属。 */
    private MobiCouponTemplate loadAndAssertShop(Long templateId, Long shopId) {
        MobiCouponTemplate template = mobiCouponTemplateService.getById(templateId);
        if (template == null) {
            throw new ServiceException("优惠券不存在", 404);
        }
        if (!template.getShopId().equals(shopId)) {
            throw new ServiceException("店铺 ID 与优惠券不匹配", 400);
        }
        return template;
    }
}
