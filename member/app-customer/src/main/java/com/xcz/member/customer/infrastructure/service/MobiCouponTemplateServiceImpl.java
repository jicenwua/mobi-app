package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.member.customer.domain.enums.CouponDistributionStatus;
import com.xcz.member.customer.domain.service.MobiCouponTemplateService;
import com.xcz.member.customer.domain.service.MobiUserCouponsService;
import com.xcz.member.customer.infrastructure.cache.CouponStockCache;
import com.xcz.member.customer.infrastructure.entity.MobiCouponTemplate;
import com.xcz.member.customer.infrastructure.entity.MobiUserCoupons;
import com.xcz.member.customer.infrastructure.mapper.MobiCouponTemplateMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

/**
 * 优惠券模板持久化实现。
 * <p>
 * 写操作成功后自动同步 {@link CouponStockCache}，应用层无需手动维护 Redis 库存。
 */
@Service
public class MobiCouponTemplateServiceImpl extends ServiceImpl<MobiCouponTemplateMapper, MobiCouponTemplate>
        implements MobiCouponTemplateService {

    @Resource
    private MobiCouponTemplateMapper mobiCouponTemplateMapper;
    @Resource
    private MobiUserCouponsService mobiUserCouponsService;

    /** 新增券模板后同步 Redis 发放库存。 */
    @Override
    public boolean save(MobiCouponTemplate entity) {
        boolean saved = super.save(entity);
        if (saved) {
            syncCouponCacheAfterMutation(entity);
        }
        return saved;
    }

    /** 更新券模板后同步或清除 Redis 发放库存。 */
    @Override
    public boolean updateById(MobiCouponTemplate entity) {
        boolean updated = super.updateById(entity);
        if (updated) {
            syncCouponCacheAfterMutation(entity);
        }
        return updated;
    }

    /** 删除券模板后清除 Redis 发放库存。 */
    @Override
    public boolean removeById(Serializable id) {
        MobiCouponTemplate existing = getById(id);
        boolean removed = super.removeById(id);
        if (removed && existing != null && existing.getTemplateId() != null) {
            CouponStockCache.evict(existing.getTemplateId());
        }
        return removed;
    }

    /**
     * 更新优惠券模板剩余可发放数量。
     *
     * @param templateId 模板 ID
     * @param remaining  剩余数量
     */
    @Override
    public void updateRemaining(Long templateId, long remaining) {
        mobiCouponTemplateMapper.updateRemaining(templateId, remaining);
    }

    /**
     * 用户领取优惠券：内部完成 Redis 预热、扣减、持券入库与模板计数更新。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long claimForUser(MobiCouponTemplate template, Long userId) {
        CouponStockCache.warmTemplate(template);
        MobiUserCoupons pending = CouponStockCache.tryClaim(template.getTemplateId());
        if (pending == null) {
            return null;
        }
        pending.setUserId(userId);
        try {
            mobiUserCouponsService.save(pending);
            template.setIssuedQuantity((template.getIssuedQuantity() == null ? 0L : template.getIssuedQuantity()) + 1L);
            updateById(template);
            return pending.getUserCouponId();
        } catch (RuntimeException ex) {
            CouponStockCache.rollbackClaim(template.getTemplateId(), userId);
            throw ex;
        }
    }

    /** 按数据库剩余量与发放状态同步 Redis 券库存（手动停止时清除）。 */
    private static void syncCouponCacheAfterMutation(MobiCouponTemplate template) {
        if (template == null || template.getTemplateId() == null) {
            return;
        }
        if (CouponDistributionStatus.isManuallyStoppedCode(template.getDistributionStatus())) {
            CouponStockCache.evict(template.getTemplateId());
            return;
        }
        Long remaining = CouponDistributionStatus.remainingStock(template);
        if (remaining != null) {
            CouponStockCache.resetStock(template.getTemplateId(), remaining);
        }
        CouponStockCache.warmTemplate(template);
    }
}
