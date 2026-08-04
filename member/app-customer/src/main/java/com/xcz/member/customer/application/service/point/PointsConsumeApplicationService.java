package com.xcz.member.customer.application.service.point;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.domain.dto.points.PointsConsumeDTO;
import com.xcz.member.customer.domain.dto.points.PointsConsumeItemDTO;
import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import com.xcz.member.customer.domain.enums.PointsActionType;
import com.xcz.member.customer.domain.enums.PointsLogStatus;
import com.xcz.member.customer.domain.enums.UserCouponStatus;
import com.xcz.member.customer.domain.order.model.PurchaseLineItem;
import com.xcz.member.customer.domain.order.model.PurchaseSettlement;
import com.xcz.member.customer.domain.order.service.PurchaseSettlementService;
import com.xcz.member.customer.domain.service.*;
import com.xcz.member.customer.domain.vo.PointsReceiptItemVO;
import com.xcz.member.customer.infrastructure.cache.ProductStockCache;
import com.xcz.member.customer.infrastructure.cache.ShopCache;
import com.xcz.member.customer.infrastructure.cache.dto.OrderTokenCacheDTO;
import com.xcz.member.customer.infrastructure.entity.*;
import com.xcz.member.customer.utils.OrderQrcodeUtils;
import com.xcz.member.customer.utils.PayQrcodeUtils;
import com.xcz.member.customer.utils.ShopAccessUtils;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 积分消费写侧编排：结算、扣库存、扣积分、写流水与核销优惠券。
 * <p>仅由 Controller 或其他 Application Service 调用。</p>
 */
@Slf4j
@Service
public class PointsConsumeApplicationService {

    @Resource
    private MobiPointsAccountService mobiPointsAccountService;
    @Resource
    private MobiShopService mobiShopService;
    @Resource
    private MobiShopUserService mobiShopUserService;
    @Resource
    private MobiShopProductService mobiShopProductService;
    @Resource
    private MobiPointsLogService mobiPointsLogService;
    @Resource
    private MobiReceiptService mobiReceiptService;
    @Resource
    private MobiUserCouponsService mobiUserCouponsService;
    @Resource
    private MobiCouponTemplateService mobiCouponTemplateService;
    @Resource
    private PointsRequestIdempotencyService pointsRequestIdempotencyService;
    @Resource
    private PointsConsumeTransactionService pointsConsumeTransactionService;

    /**
     * 店员扫码扣款：校验付款码与权限后执行消费。
     *
     * @param dto 扣款请求（shopId、付款码、购物清单、requestId）
     * @return 本次实付积分
     */
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal staffConsume(PointsConsumeDTO dto) {
        Long shopId = dto.getShopId();
        ShopAccessUtils.assertShopStaff(mobiShopUserService, shopId);

        String requestId = dto.getRequestId().trim();

        Optional<Long> existingLogId = pointsRequestIdempotencyService.findCompletedLogId(requestId);
        if (existingLogId.isPresent()) {
            return resolveConsumePoints(existingLogId.get());
        }

        pointsRequestIdempotencyService.acquireOrThrow(requestId);

        Long userId = PayQrcodeUtils.tryConsumeUserId(dto.getToken());
        if (userId == null) {
            pointsRequestIdempotencyService.release(requestId);
            throw new ServiceException("付款码已过期或无效");
        }
        ShopAccessUtils.assertUserIsShopCustomer(mobiShopUserService, userId, shopId);

        if (resolveShopBrief(shopId) == null) {
            pointsRequestIdempotencyService.release(requestId);
            throw new ServiceException("店铺不存在");
        }

        try {
            ConsumeExecutionResult result = execute(
                    userId,
                    shopId,
                    dto.getItems(),
                    PointsLogStatus.VERIFIED.getCode(),
                    dto.getCouponId(),
                    requestId);
            pointsRequestIdempotencyService.complete(requestId, result.logId());
            log.info("店员扫码扣款成功 userId={} shopId={} logId={} points={}",
                    userId, shopId, result.logId(), result.payablePoints());
            return result.payablePoints();
        } catch (DuplicateKeyException ex) {
            pointsRequestIdempotencyService.release(requestId);
            MobiPointsLog existing = mobiPointsLogService.findByRequestId(requestId);
            if (existing != null) {
                pointsRequestIdempotencyService.complete(requestId, existing.getLogId());
                return resolveConsumePoints(existing.getLogId());
            }
            throw new ServiceException("请求处理中，请稍后重试");
        } catch (Exception ex) {
            pointsRequestIdempotencyService.release(requestId);
            throw ex;
        }
    }

    private BigDecimal resolveConsumePoints(Long logId) {
        MobiPointsLog existing = mobiPointsLogService.getById(logId);
        if (existing == null) {
            throw new ServiceException("订单不存在");
        }
        return safeDecimal(existing.getChangeBase()).add(safeDecimal(existing.getChangeBonus())).abs();
    }

    /**
     * 店员扫码核销待使用订单（积分已在下单时扣除）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long staffVerifyOrder(Long shopId, String token) {
        ShopAccessUtils.assertShopStaff(mobiShopUserService, shopId);

        OrderTokenCacheDTO payload = OrderQrcodeUtils.getPayload(token);
        if (payload != null) {
            return verifyOrderByPayload(shopId, token, payload);
        }

        throw new ServiceException("订单码已过期或无效");
    }

    private Long verifyOrderByPayload(Long shopId, String token, OrderTokenCacheDTO payload) {
        if (!shopId.equals(payload.getShopId())) {
            throw new ServiceException("订单不属于当前店铺");
        }

        MobiPointsLog pointsLog = mobiPointsLogService.getById(payload.getLogId());
        if (pointsLog == null) {
            throw new ServiceException("订单不存在");
        }
        if (!shopId.equals(pointsLog.getShopId())) {
            throw new ServiceException("订单与店铺不匹配");
        }

        Integer status = pointsLog.getStatus();
        if (status != null && status == PointsLogStatus.VERIFIED.getCode()) {
            OrderQrcodeUtils.invalidate(token);
            return pointsLog.getLogId();
        }
        if (status == null || status != PointsLogStatus.PENDING_VERIFY.getCode()) {
            throw new ServiceException("订单不可核销");
        }

        OrderTokenCacheDTO consumed = OrderQrcodeUtils.tryConsumePayload(token);
        if (consumed == null) {
            pointsLog = mobiPointsLogService.getById(payload.getLogId());
            if (pointsLog != null
                    && pointsLog.getStatus() != null
                    && pointsLog.getStatus() == PointsLogStatus.VERIFIED.getCode()) {
                return pointsLog.getLogId();
            }
            throw new ServiceException("订单码已过期或无效");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = mobiPointsLogService.verifyPendingOrder(pointsLog.getLogId(), shopId, now);
        if (updated == 0) {
            pointsLog = mobiPointsLogService.getById(payload.getLogId());
            if (pointsLog != null
                    && pointsLog.getStatus() != null
                    && pointsLog.getStatus() == PointsLogStatus.VERIFIED.getCode()) {
                return pointsLog.getLogId();
            }
            throw new ServiceException("订单不可核销");
        }

        log.info("订单核销成功 shopId={} logId={}", shopId, pointsLog.getLogId());
        return pointsLog.getLogId();
    }

    /**
     * 会员自助退款：仅待使用订单可退，返还积分并恢复库存。
     */
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal memberRefundOrder(Long logId, Long shopId) {
        Long userId = SecurityUtils.getUserId();
        ShopAccessUtils.assertShopMember(mobiShopUserService, shopId);

        MobiPointsLog pointsLog = mobiPointsLogService.getById(logId);
        if (pointsLog == null) {
            throw new ServiceException("订单不存在");
        }
        if (!userId.equals(pointsLog.getUserId())) {
            throw new ServiceException("无权操作该订单");
        }
        if (!shopId.equals(pointsLog.getShopId())) {
            throw new ServiceException("订单与店铺不匹配");
        }
        if (pointsLog.getActionType() == null || pointsLog.getActionType() != PointsActionType.CONSUME.getCode()) {
            throw new ServiceException("该记录不可退款");
        }
        if (pointsLog.getStatus() == null || pointsLog.getStatus() != PointsLogStatus.PENDING_VERIFY.getCode()) {
            throw new ServiceException("订单不可退款");
        }

        BigDecimal refundBase = safeDecimal(pointsLog.getChangeBase()).negate();
        BigDecimal refundBonus = safeDecimal(pointsLog.getChangeBonus()).negate();
        BigDecimal refundTotal = refundBase.add(refundBonus);
        if (refundTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("退款积分异常");
        }

        restoreStockForOrder(logId);

        int refunded = mobiPointsAccountService.refundPoints(userId, shopId, refundBase, refundBonus, refundTotal);
        if (refunded == 0) {
            throw new ServiceException("积分退还失败");
        }

        pointsLog.setStatus(PointsLogStatus.CANCELLED.getCode());
        pointsLog.setConsumeTime(LocalDateTime.now());
        mobiPointsLogService.updateById(pointsLog);

        MobiPointsAccount account = mobiPointsAccountService.getByUserIdAndShopId(userId, shopId);
        BigDecimal remaining = safeDecimal(account == null ? null : account.getBasePoints())
                .add(safeDecimal(account == null ? null : account.getBonusPoints()));
        log.info("订单退款成功 userId={} shopId={} logId={} refund={}", userId, shopId, logId, refundTotal);
        return remaining;
    }

    /**
     * 执行积分消费：结算 → 校验库存 → 扣库存 → 原子扣积分 → 写流水与小票 → 核销优惠券。
     *
     * @param userId       消费用户 ID
     * @param shopId       店铺 ID
     * @param items        商品明细
     * @param logStatus    流水状态
     * @param userCouponId 可选优惠券 ID
     * @return 执行结果
     */
    public ConsumeExecutionResult execute(Long userId,
                                          Long shopId,
                                          List<PointsConsumeItemDTO> items,
                                          int logStatus,
                                          Long userCouponId,
                                          String requestId) {
        Map<Long, MobiShopProduct> productMap = loadProductMap(shopId, items);
        MobiCouponTemplate couponTemplate = resolveCouponTemplate(userId, shopId, userCouponId);
        PurchaseSettlement settlement = PurchaseSettlementService.settle(
                items, productMap, userCouponId, couponTemplate);

        MobiPointsAccount account = getOrCreateAccount(userId, shopId);
        BigDecimal preBase = safeDecimal(account.getBasePoints());
        BigDecimal preBonus = safeDecimal(account.getBonusPoints());
        BigDecimal totalPoints = settlement.payablePoints();
        BigDecimal available = preBase.add(preBonus);
        if (available.compareTo(totalPoints) < 0) {
            throw new ServiceException("积分不足，当前剩余 " + available.stripTrailingZeros().toPlainString() + " 积分");
        }

        assertStockAvailable(settlement.items());
        return pointsConsumeTransactionService.commit(
                userId, shopId, settlement, account, logStatus, userCouponId, requestId);
    }

    /**
     * 批量加载结算商品并构建 Map，避免 N+1 查询。
     */
    private Map<Long, MobiShopProduct> loadProductMap(Long shopId, List<PointsConsumeItemDTO> items) {
        List<Long> productIds = PurchaseSettlementService.collectProductIds(items);
        List<MobiShopProduct> products = mobiShopProductService.listByProductIds(productIds);
        return PurchaseSettlementService.buildProductMap(shopId, items, products);
    }

    /**
     * 校验 Redis 实时库存是否满足购买数量。
     */
    private void assertStockAvailable(List<PurchaseLineItem> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        List<Long> productIds = lines.stream().map(PurchaseLineItem::productId).distinct().toList();
        mobiShopProductService.ensureStockCachedBatch(productIds);
        for (PurchaseLineItem line : lines) {
            long available = ProductStockCache.getAvailableStock(line.productId());
            if (available != ProductStockCache.UNLIMITED_STOCK && available < line.count()) {
                throw new ServiceException("商品「" + line.productName() + "」库存不足");
            }
        }
    }

    private void restoreStockForOrder(Long logId) {
        Map<Long, List<PointsReceiptItemVO>> itemsMap = mobiReceiptService.mapItemsByLogIds(List.of(logId));
        List<PointsReceiptItemVO> items = itemsMap.getOrDefault(logId, List.of());
        for (PointsReceiptItemVO item : items) {
            if (item.getCount() == null || item.getCount() <= 0) {
                continue;
            }
            Long productId = item.getProductId();
            if (productId == null) {
                log.warn("退款小票缺少 productId，跳过库存回滚 logId={} productName={}", logId, item.getProductName());
                continue;
            }
            mobiShopProductService.rollbackDeduct(productId, item.getCount());
        }
    }

    private MobiPointsAccount getOrCreateAccount(Long userId, Long shopId) {
        return mobiPointsAccountService.getOrCreateAccount(userId, shopId);
    }

    private MobiCouponTemplate resolveCouponTemplate(Long userId, Long shopId, Long userCouponId) {
        if (userCouponId == null) {
            return null;
        }
        MobiUserCoupons userCoupon = loadUsableCoupon(userId, shopId, userCouponId);
        MobiCouponTemplate template = mobiCouponTemplateService.getById(userCoupon.getTemplateId());
        if (template == null) {
            throw new ServiceException("优惠券不存在");
        }
        return template;
    }

    private MobiUserCoupons loadUsableCoupon(Long userId, Long shopId, Long userCouponId) {
        MobiUserCoupons coupon = mobiUserCouponsService.getById(userCouponId);
        if (coupon == null) {
            throw new ServiceException("优惠券不存在");
        }
        if (!userId.equals(coupon.getUserId())) {
            throw new ServiceException("无权使用该优惠券");
        }
        if (!shopId.equals(coupon.getShopId())) {
            throw new ServiceException("优惠券不适用于当前店铺");
        }
        if (coupon.getStatus() == null || coupon.getStatus() != UserCouponStatus.UNUSED.getCode()) {
            throw new ServiceException("优惠券不可用");
        }
        if (coupon.getExpireTime() != null && LocalDateTime.now().isAfter(coupon.getExpireTime())) {
            throw new ServiceException("优惠券已过期");
        }
        return coupon;
    }

    private ShopBriefDTO resolveShopBrief(Long shopId) {
        ShopBriefDTO cached = ShopCache.getByIds(List.of(shopId)).get(shopId);
        if (cached != null) {
            return cached;
        }
        var shop = mobiShopService.getById(shopId);
        if (shop == null) {
            return null;
        }
        ShopBriefDTO brief = ShopBriefDTO.of(shop);
        ShopCache.putAll(List.of(brief));
        return brief;
    }

    private static BigDecimal safeDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 积分消费执行结果。
     *
     * @param logId           流水 ID
     * @param originalPoints  原价积分
     * @param discountPoints  优惠抵扣
     * @param payablePoints   实付积分
     * @param afterBase       扣后基础积分
     * @param afterBonus      扣后赠送积分
     */
    public record ConsumeExecutionResult(
            Long logId,
            BigDecimal originalPoints,
            BigDecimal discountPoints,
            BigDecimal payablePoints,
            BigDecimal afterBase,
            BigDecimal afterBonus
    ) {
    }
}
