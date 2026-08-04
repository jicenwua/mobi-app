package com.xcz.member.customer.application.service.point;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.domain.enums.PointsActionType;
import com.xcz.member.customer.domain.enums.PointsLogStatus;
import com.xcz.member.customer.domain.enums.UserCouponStatus;
import com.xcz.member.customer.domain.order.model.PurchaseLineItem;
import com.xcz.member.customer.domain.order.model.PurchaseSettlement;
import com.xcz.member.customer.domain.service.*;
import com.xcz.member.customer.infrastructure.entity.MobiPointsAccount;
import com.xcz.member.customer.infrastructure.entity.MobiPointsLog;
import com.xcz.member.customer.infrastructure.entity.MobiReceipt;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 积分消费事务内写操作，与前置校验分离以缩短锁持有时间。
 */
@Service
class PointsConsumeTransactionService {

    @Resource
    private MobiPointsAccountService mobiPointsAccountService;
    @Resource
    private MobiShopProductService mobiShopProductService;
    @Resource
    private MobiPointsLogService mobiPointsLogService;
    @Resource
    private MobiReceiptService mobiReceiptService;
    @Resource
    private MobiUserCouponsService mobiUserCouponsService;

    @Transactional(rollbackFor = Exception.class)
    public PointsConsumeApplicationService.ConsumeExecutionResult commit(
            Long userId,
            Long shopId,
            PurchaseSettlement settlement,
            MobiPointsAccount account,
            int logStatus,
            Long userCouponId,
            String requestId) {
        BigDecimal preBase = account.getBasePoints() == null ? BigDecimal.ZERO : account.getBasePoints();
        BigDecimal preBonus = account.getBonusPoints() == null ? BigDecimal.ZERO : account.getBonusPoints();
        BigDecimal totalPoints = settlement.payablePoints();

        List<long[]> stockDeductions = settlement.items().stream()
                .map(line -> new long[]{line.productId(), line.count()})
                .toList();
        mobiShopProductService.deductStockBatch(stockDeductions);

        BigDecimal deductBonus = preBonus.min(totalPoints);
        BigDecimal deductBase = totalPoints.subtract(deductBonus);
        int deducted = mobiPointsAccountService.deductPoints(userId, shopId, deductBase, deductBonus, totalPoints);
        if (deducted == 0) {
            rollbackStockDeductions(stockDeductions);
            throw new ServiceException("积分不足或账户已变更，请重试");
        }

        BigDecimal afterBonus = preBonus.subtract(deductBonus);
        BigDecimal afterBase = preBase.subtract(deductBase);

        LocalDateTime now = LocalDateTime.now();
        MobiPointsLog pointsLog = MobiPointsLog.builder()
                .userId(userId)
                .shopId(shopId)
                .couponId(userCouponId)
                .actionType(PointsActionType.CONSUME.getCode())
                .preBasePoints(preBase)
                .preBonusPoints(preBonus)
                .changeBase(deductBase.negate())
                .changeBonus(deductBonus.negate())
                .afterBasePoints(afterBase)
                .afterBonusPoints(afterBonus)
                .status(logStatus)
                .requestId(requestId)
                .createTime(now)
                .consumeTime(logStatus == PointsLogStatus.VERIFIED.getCode() ? now : null)
                .build();
        mobiPointsLogService.save(pointsLog);

        List<MobiReceipt> receipts = new ArrayList<>();
        for (PurchaseLineItem line : settlement.items()) {
            receipts.add(MobiReceipt.builder()
                    .logId(pointsLog.getLogId())
                    .productId(line.productId())
                    .productName(line.productName())
                    .count(line.count())
                    .price(line.unitPrice())
                    .userId(userId)
                    .createTime(LocalDateTime.now())
                    .build());
        }
        mobiReceiptService.saveBatch(receipts);

        if (userCouponId != null) {
            markCouponUsed(userCouponId);
        }

        return new PointsConsumeApplicationService.ConsumeExecutionResult(
                pointsLog.getLogId(),
                settlement.originalPoints(),
                settlement.discountPoints(),
                totalPoints,
                afterBase,
                afterBonus);
    }

    private void rollbackStockDeductions(List<long[]> stockDeductions) {
        for (long[] item : stockDeductions) {
            mobiShopProductService.rollbackDeduct(item[0], item[1]);
        }
    }

    private void markCouponUsed(Long userCouponId) {
        int updated = mobiUserCouponsService.markUsedIfUnused(
                userCouponId,
                LocalDateTime.now(),
                UserCouponStatus.UNUSED.getCode(),
                UserCouponStatus.USED.getCode());
        if (updated == 0) {
            throw new ServiceException("优惠券不可用或已被使用");
        }
    }
}
