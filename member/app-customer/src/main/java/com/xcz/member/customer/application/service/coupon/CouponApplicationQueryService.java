package com.xcz.member.customer.application.service.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.utils.PageUtils;
import com.xcz.member.customer.application.assemblers.CouponAssembler;
import com.xcz.member.customer.application.command.coupon.QueryCouponTemplateCommand;
import com.xcz.member.customer.application.command.coupon.QueryUserCouponCommand;
import com.xcz.member.customer.api.dto.response.coupon.CouponTemplateRes;
import com.xcz.member.customer.api.dto.response.coupon.UserCouponRes;
import com.xcz.member.customer.domain.dto.coupon.UserCouponBriefDTO;
import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import com.xcz.member.customer.domain.enums.CouponDistributionStatus;
import com.xcz.member.customer.domain.enums.UserCouponStatus;
import com.xcz.member.customer.domain.service.*;
import com.xcz.member.customer.infrastructure.cache.CouponStockCache;
import com.xcz.member.customer.infrastructure.entity.MobiCouponTemplate;
import com.xcz.member.customer.infrastructure.entity.MobiShop;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import com.xcz.member.customer.infrastructure.entity.MobiUserCoupons;
import com.xcz.member.customer.utils.ShopAccessUtils;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 优惠券读侧应用服务。
 */
@Service
public class CouponApplicationQueryService {

    @Resource
    private MobiCouponTemplateService mobiCouponTemplateService;
    @Resource
    private MobiUserCouponsService mobiUserCouponsService;
    @Resource
    private MobiShopUserService mobiShopUserService;
    @Resource
    private MobiShopService mobiShopService;
    @Resource
    private MobiUserService mobiUserService;
    //TODO 优惠券也要使用redis

    /**
     * 店员/店长：查看店铺全部券模板（含库存与阶段）。
     */
    public Page<CouponTemplateRes> pageTemplatesForStaff(QueryCouponTemplateCommand command) {
        ShopAccessUtils.assertShopStaff(mobiShopUserService, command.shopId());
        LocalDateTime now = LocalDateTime.now();
        List<MobiCouponTemplate> templates = mobiCouponTemplateService.list(
                new LambdaQueryWrapper<MobiCouponTemplate>()
                        .eq(MobiCouponTemplate::getShopId, command.shopId())
                        .orderByDesc(MobiCouponTemplate::getCreateTime));
        syncDistributionStatuses(templates, now);
        List<CouponTemplateRes> all = templates.stream()
                .map(template -> enrichRemaining(template, now))
                .map(CouponAssembler::toBriefDto)
                .map(CouponAssembler::toVo)
                .filter(vo -> matchDistributionStatus(vo.getDistributionStatus(), command.distributionStatus()))
                .toList();
        return slicePage(all, command.resolvedPageNum(), command.resolvedPageSize());
    }

    /**
     * 管理后台 Feign：券模板列表（店铺可选，无店铺时按模板 ID 倒序）。
     */
    public Page<CouponTemplateRes> pageTemplatesForFeign(Long shopId, int pageNum, int pageSize) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<MobiCouponTemplate> wrapper = new LambdaQueryWrapper<MobiCouponTemplate>()
                .eq(shopId != null, MobiCouponTemplate::getShopId, shopId)
                .orderByDesc(MobiCouponTemplate::getTemplateId);
        Page<MobiCouponTemplate> entityPage = mobiCouponTemplateService.page(new Page<>(pageNum, pageSize), wrapper);
        syncDistributionStatuses(entityPage.getRecords(), now);
        List<CouponTemplateRes> records = entityPage.getRecords().stream()
                .map(template -> enrichRemaining(template, now))
                .map(CouponAssembler::toBriefDto)
                .map(CouponAssembler::toVo)
                .toList();
        Page<CouponTemplateRes> page = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        page.setRecords(records);
        return enrichShopNames(page);
    }

    private Page<CouponTemplateRes> enrichShopNames(Page<CouponTemplateRes> page) {
        if (page.getRecords() == null || page.getRecords().isEmpty()) {
            return page;
        }
        Map<Long, String> shopNames = loadShopNameMap(page.getRecords());
        page.setRecords(page.getRecords().stream()
                .map(vo -> vo.toBuilder().shopName(shopNames.get(vo.getShopId())).build())
                .toList());
        return page;
    }

    private Map<Long, String> loadShopNameMap(List<CouponTemplateRes> rows) {
        List<Long> shopIds = rows.stream()
                .map(CouponTemplateRes::getShopId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (shopIds.isEmpty()) {
            return Map.of();
        }
        return mobiShopService.listBriefByIds(shopIds).stream()
                .collect(Collectors.toMap(ShopBriefDTO::id, ShopBriefDTO::shopName, (a, b) -> a));
    }

    /**
     * 管理后台 Feign：券模板详情。
     */
    public CouponTemplateRes getTemplateForFeign(Long templateId) {
        MobiCouponTemplate template = mobiCouponTemplateService.getById(templateId);
        if (template == null) {
            throw new com.xcz.commons.core.exception.ServiceException("优惠券不存在", 404);
        }
        LocalDateTime now = LocalDateTime.now();
        syncDistributionStatus(template, now);
        CouponTemplateRes res = CouponAssembler.toVo(
                CouponAssembler.toBriefDto(enrichRemaining(template, now)));
        if (res != null && res.getShopId() != null) {
            Map<Long, String> shopNames = loadShopNameMap(List.of(res));
            res.setShopName(shopNames.get(res.getShopId()));
        }
        return res;
    }

    /**
     * 管理后台 Feign：用户持券分页。
     */
    public Page<UserCouponRes> pageUserCouponsForFeign(Long userId, Long shopId, Long templateId, Integer status,
                                                       int pageNum, int pageSize) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<MobiUserCoupons> wrapper = new LambdaQueryWrapper<MobiUserCoupons>()
                .eq(userId != null, MobiUserCoupons::getUserId, userId)
                .eq(shopId != null, MobiUserCoupons::getShopId, shopId)
                .eq(templateId != null, MobiUserCoupons::getTemplateId, templateId)
                .orderByDesc(MobiUserCoupons::getReceiveTime);
        applyStatusFilter(wrapper, status, now);
        Page<MobiUserCoupons> entityPage = mobiUserCouponsService.page(
                new Page<>(pageNum, pageSize), wrapper);
        List<MobiUserCoupons> normalized = entityPage.getRecords().stream()
                .map(this::normalizeStatus)
                .toList();
        List<UserCouponRes> records = toUserBriefBatch(normalized).stream()
                .map(CouponAssembler::toVo)
                .toList();
        Page<UserCouponRes> page = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        page.setRecords(records);
        return page;
    }

    /**
     * 顾客：店铺当前可领取的优惠券。
     *
     * @param shopId 店铺 ID
     * @return 可领取券模板列表
     */
    public List<CouponTemplateRes> listDistributingForCustomer(Long shopId) {
        ShopAccessUtils.assertShopCustomer(mobiShopUserService, shopId);
        Long userId = SecurityUtils.getUserId();
        //查询用户的优惠券id
        Set<Long> claimedTemplateIds = mobiUserCouponsService.list(
                        new LambdaQueryWrapper<MobiUserCoupons>()
                                .eq(MobiUserCoupons::getUserId, userId)
                                .eq(MobiUserCoupons::getShopId, shopId)
                                .select(MobiUserCoupons::getTemplateId))
                .stream()
                .map(MobiUserCoupons::getTemplateId)
                .collect(Collectors.toSet());
        LocalDateTime now = LocalDateTime.now();
        //查询店铺所有的优惠券
        List<MobiCouponTemplate> templates = mobiCouponTemplateService.list(
                new LambdaQueryWrapper<MobiCouponTemplate>()
                        .eq(MobiCouponTemplate::getShopId, shopId)
                        .orderByDesc(MobiCouponTemplate::getCreateTime)
        );
        syncDistributionStatuses(templates, now);
        return templates.stream()
                .filter(template -> Objects.equals(
                        template.getDistributionStatus(), CouponDistributionStatus.DISTRIBUTING.getCode()))
                .filter(template -> !claimedTemplateIds.contains(template.getTemplateId()))
                .peek(CouponStockCache::warmTemplate)
                .map(template -> enrichRemaining(template, now))
                .filter(this::hasClaimableStock)
                .map(CouponAssembler::toBriefDto)
                .map(CouponAssembler::toVo)
                .toList();
    }

    /**
     * 顾客：我的优惠券（支持按店铺/状态筛选，分页返回全部记录）。
     */
    public Page<UserCouponRes> pageMyCoupons(QueryUserCouponCommand command) {
        if (command.shopId() != null) {
            ShopAccessUtils.assertShopCustomer(mobiShopUserService, command.shopId());
        }
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<MobiUserCoupons> wrapper = new LambdaQueryWrapper<MobiUserCoupons>()
                .eq(MobiUserCoupons::getUserId, userId)
                .eq(command.shopId() != null, MobiUserCoupons::getShopId, command.shopId())
                .orderByDesc(MobiUserCoupons::getReceiveTime);
        applyStatusFilter(wrapper, command.status(), now);
        Page<MobiUserCoupons> entityPage = mobiUserCouponsService.page(
                new Page<>(command.resolvedPageNum(), command.resolvedPageSize()), wrapper);
        List<MobiUserCoupons> normalized = entityPage.getRecords().stream()
                .map(this::normalizeStatus)
                .toList();
        List<UserCouponRes> records = toUserBriefBatch(normalized).stream()
                .map(CouponAssembler::toVo)
                .toList();
        Page<UserCouponRes> page = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        page.setRecords(records);
        return page;
    }

    /**
     * 按持券状态筛选（含未同步的过期未使用券）。
     */
    private void applyStatusFilter(LambdaQueryWrapper<MobiUserCoupons> wrapper, Integer status, LocalDateTime now) {
        if (status == null) {
            return;
        }
        if (status == UserCouponStatus.UNUSED.getCode()) {
            wrapper.eq(MobiUserCoupons::getStatus, UserCouponStatus.UNUSED.getCode())
                    .and(w -> w.isNull(MobiUserCoupons::getExpireTime)
                            .or()
                            .gt(MobiUserCoupons::getExpireTime, now));
            return;
        }
        if (status == UserCouponStatus.USED.getCode()) {
            wrapper.eq(MobiUserCoupons::getStatus, UserCouponStatus.USED.getCode());
            return;
        }
        if (status == UserCouponStatus.EXPIRED.getCode()) {
            wrapper.and(w -> w.eq(MobiUserCoupons::getStatus, UserCouponStatus.EXPIRED.getCode())
                    .or(or -> or.eq(MobiUserCoupons::getStatus, UserCouponStatus.UNUSED.getCode())
                            .isNotNull(MobiUserCoupons::getExpireTime)
                            .le(MobiUserCoupons::getExpireTime, now)));
        }
    }

    /**
     * 有限库存券在剩余为 0 时不可领取；无限库存始终可领。
     */
    private boolean hasClaimableStock(MobiCouponTemplate template) {
        if (CouponStockCache.isUnlimitedTotal(template.getTotalQuantity())) {
            return true;
        }
        Long remaining = CouponDistributionStatus.remainingStock(template);
        return remaining != null && remaining > 0L;
    }

    /**
     * 合并 Redis 实时库存到模板已发放数。
     */
    private MobiCouponTemplate enrichRemaining(MobiCouponTemplate template, LocalDateTime now) {
        if (CouponStockCache.isUnlimitedTotal(template.getTotalQuantity())) {
            return template;
        }
        long redisRemaining = CouponStockCache.getRemaining(template.getTemplateId());
        if (redisRemaining > 0
                && !CouponStockCache.isUnlimitedRemaining(redisRemaining)
                && CouponDistributionStatus.isDistributing(template, now)) {
            long issued = template.getTotalQuantity() - redisRemaining;
            long dbIssued = template.getIssuedQuantity() == null ? 0L : template.getIssuedQuantity();
            template.setIssuedQuantity(Math.max(issued, dbIssued));
        }
        return template;
    }

    /**
     * 将已过期的未使用券标记为已过期并回写数据库。
     */
    private MobiUserCoupons normalizeStatus(MobiUserCoupons coupon) {
        if (coupon.getStatus() != null && coupon.getStatus() == UserCouponStatus.UNUSED.getCode()
                && coupon.getExpireTime() != null
                && LocalDateTime.now().isAfter(coupon.getExpireTime())) {
            coupon.setStatus(UserCouponStatus.EXPIRED.getCode());
            mobiUserCouponsService.updateById(coupon);
        }
        return coupon;
    }

    /**
     * 批量将用户持券实体转为简要读模型，避免 N+1 查库。
     */
    private List<UserCouponBriefDTO> toUserBriefBatch(List<MobiUserCoupons> coupons) {
        if (coupons == null || coupons.isEmpty()) {
            return List.of();
        }
        List<Long> templateIds = coupons.stream()
                .map(MobiUserCoupons::getTemplateId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> shopIds = coupons.stream()
                .map(MobiUserCoupons::getShopId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> userIds = coupons.stream()
                .map(MobiUserCoupons::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MobiCouponTemplate> templateMap = templateIds.isEmpty()
                ? Map.of()
                : mobiCouponTemplateService.listByIds(templateIds).stream()
                .collect(Collectors.toMap(MobiCouponTemplate::getTemplateId, t -> t, (a, b) -> a));
        Map<Long, MobiShop> shopMap = shopIds.isEmpty()
                ? Map.of()
                : mobiShopService.listByIds(shopIds).stream()
                .collect(Collectors.toMap(MobiShop::getId, s -> s, (a, b) -> a));
        Map<Long, MobiUser> userMap = userIds.isEmpty()
                ? Map.of()
                : mobiUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(MobiUser::getUserId, u -> u, (a, b) -> a));
        return coupons.stream()
                .map(coupon -> toUserBrief(coupon, templateMap, shopMap, userMap))
                .toList();
    }

    /**
     * 将用户持券实体转为简要读模型（附带模板与店铺信息）。
     */
    private UserCouponBriefDTO toUserBrief(MobiUserCoupons coupon,
                                           Map<Long, MobiCouponTemplate> templateMap,
                                           Map<Long, MobiShop> shopMap,
                                           Map<Long, MobiUser> userMap) {
        MobiCouponTemplate template = templateMap.get(coupon.getTemplateId());
        MobiShop shop = shopMap.get(coupon.getShopId());
        MobiUser user = userMap.get(coupon.getUserId());
        return UserCouponBriefDTO.builder()
                .userCouponId(coupon.getUserCouponId())
                .userId(coupon.getUserId())
                .nickname(user == null ? null : user.getNickname())
                .shopId(coupon.getShopId())
                .templateId(coupon.getTemplateId())
                .couponName(template == null ? null : template.getCouponName())
                .type(template == null ? null : template.getType())
                .thresholdAmount(template == null ? null : template.getThresholdAmount())
                .discountValue(template == null ? null : template.getDiscountValue())
                .status(coupon.getStatus())
                .receiveTime(coupon.getReceiveTime())
                .usedTime(coupon.getUsedTime())
                .expireTime(coupon.getExpireTime())
                .shopName(shop == null ? null : shop.getShopName())
                .build();
    }

    /**
     * 按发放状态编码过滤列表。
     */
    private boolean matchDistributionStatus(Integer actualStatus, Integer expectedStatus) {
        if (expectedStatus == null) {
            return true;
        }
        return expectedStatus.equals(actualStatus);
    }

    /**
     * 将单条券模板发放状态与当前时间对齐并写回数据库。
     */
    private void syncDistributionStatus(MobiCouponTemplate template, LocalDateTime now) {
        if (template == null) {
            return;
        }
        CouponDistributionStatus resolved = CouponDistributionStatus.resolve(template, now);
        if (resolved != null && resolved.getCode() != template.getDistributionStatus()) {
            template.setDistributionStatus(resolved.getCode());
            mobiCouponTemplateService.updateById(template);
        }
    }

    /**
     * 批量将券模板发放状态与当前时间对齐并写回数据库。
     */
    private void syncDistributionStatuses(List<MobiCouponTemplate> templates, LocalDateTime now) {
        if (templates == null || templates.isEmpty()) {
            return;
        }
        for (MobiCouponTemplate template : templates) {
            syncDistributionStatus(template, now);
        }
    }

    /**
     * 对内存列表进行分页切片。
     */
    private <T> Page<T> slicePage(List<T> all, int pageNum, int pageSize) {
        Page<T> page = new Page<>(pageNum, pageSize, PageUtils.sliceTotal(all));
        page.setRecords(PageUtils.slice(all, pageNum, pageSize));
        return page;
    }
}
