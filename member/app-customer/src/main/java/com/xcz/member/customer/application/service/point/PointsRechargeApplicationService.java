package com.xcz.member.customer.application.service.point;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.api.dto.response.activity.ActivityRes;
import com.xcz.member.customer.application.service.activity.ActivityApplicationQueryService;
import com.xcz.member.customer.domain.dto.activity.ActivityRuleDTO;
import com.xcz.member.customer.domain.dto.points.PointsRechargeDTO;
import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import com.xcz.member.customer.domain.enums.ActivityType;
import com.xcz.member.customer.domain.enums.PointsActionType;
import com.xcz.member.customer.domain.enums.PointsLogStatus;
import com.xcz.member.customer.domain.promotion.RechargeGiftCalculator;
import com.xcz.member.customer.domain.service.MobiPointsAccountService;
import com.xcz.member.customer.domain.service.MobiPointsLogService;
import com.xcz.member.customer.domain.service.MobiShopService;
import com.xcz.member.customer.domain.service.MobiShopUserService;
import com.xcz.member.customer.domain.vo.PointsRechargeResultVO;
import com.xcz.member.customer.infrastructure.cache.ShopCache;
import com.xcz.member.customer.infrastructure.entity.MobiPointsAccount;
import com.xcz.member.customer.infrastructure.entity.MobiPointsLog;
import com.xcz.member.customer.utils.ShopAccessUtils;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 店员线下充值积分（基础积分 + 充值满赠，单条流水）。
 */
@Service
public class PointsRechargeApplicationService {

    private static final String RECHARGE_REMARK = "充值积分";

    @Resource
    private MobiPointsAccountService mobiPointsAccountService;
    @Resource
    private MobiPointsLogService mobiPointsLogService;
    @Resource
    private MobiShopService mobiShopService;
    @Resource
    private MobiShopUserService mobiShopUserService;
    @Resource
    private ActivityApplicationQueryService activityApplicationQueryService;
    @Resource
    private PointsRequestIdempotencyService pointsRequestIdempotencyService;

    @Transactional(rollbackFor = Exception.class)
    public PointsRechargeResultVO staffRecharge(PointsRechargeDTO dto) {
        Long shopId = dto.getShopId();
        ShopAccessUtils.assertShopStaff(mobiShopUserService, shopId);

        String requestId = dto.getRequestId().trim();
        Optional<Long> existingLogId = pointsRequestIdempotencyService.findCompletedLogId(requestId);
        if (existingLogId.isPresent()) {
            return buildResultFromLog(existingLogId.get());
        }

        pointsRequestIdempotencyService.acquireOrThrow(requestId);

        ShopBriefDTO shopBrief = resolveShopBrief(shopId);
        if (shopBrief == null) {
            pointsRequestIdempotencyService.release(requestId);
            throw new ServiceException("店铺不存在");
        }
        Integer ratio = shopBrief.ratio();
        if (ratio == null || ratio <= 0) {
            pointsRequestIdempotencyService.release(requestId);
            throw new ServiceException("店铺充值比率未配置");
        }

        Long userId = dto.getUserId();
        ShopAccessUtils.assertUserIsShopCustomer(mobiShopUserService, userId, shopId);

        BigDecimal amountYuan = dto.getAmountYuan().setScale(2, RoundingMode.HALF_UP);
        BigDecimal basePoints = amountYuan.multiply(BigDecimal.valueOf(ratio)).setScale(0, RoundingMode.HALF_UP);
        if (basePoints.compareTo(BigDecimal.ZERO) <= 0) {
            pointsRequestIdempotencyService.release(requestId);
            throw new ServiceException("充值所得积分须大于 0");
        }

        BigDecimal giftPoints = calcRechargeGift(shopId, basePoints);
        Long pointsShopId = shopBrief.headShopId();

        try {
            MobiPointsAccount account = mobiPointsAccountService.getOrCreateAccount(userId, pointsShopId);
            BigDecimal preBase = safe(account.getBasePoints());
            BigDecimal preBonus = safe(account.getBonusPoints());

            int updated = mobiPointsAccountService.adjustPoints(userId, pointsShopId, basePoints, giftPoints);
            if (updated == 0) {
                throw new ServiceException("积分充值失败，请重试");
            }

            BigDecimal afterBase = preBase.add(basePoints);
            BigDecimal afterBonus = preBonus.add(giftPoints);
            LocalDateTime now = LocalDateTime.now();
            MobiPointsLog pointsLog = MobiPointsLog.builder()
                    .userId(userId)
                    .shopId(pointsShopId)
                    .actionType(PointsActionType.INCREASE.getCode())
                    .preBasePoints(preBase)
                    .preBonusPoints(preBonus)
                    .changeBase(basePoints)
                    .changeBonus(giftPoints)
                    .afterBasePoints(afterBase)
                    .afterBonusPoints(afterBonus)
                    .status(PointsLogStatus.RECHARGE.getCode())
                    .remark(RECHARGE_REMARK)
                    .createTime(now)
                    .consumeTime(now)
                    .requestId(requestId)
                    .build();
            mobiPointsLogService.save(pointsLog);
            pointsRequestIdempotencyService.complete(requestId, pointsLog.getLogId());

            BigDecimal remaining = afterBase.add(afterBonus);
            return PointsRechargeResultVO.builder()
                    .logId(pointsLog.getLogId())
                    .basePoints(basePoints)
                    .giftPoints(giftPoints)
                    .totalPoints(basePoints.add(giftPoints))
                    .remainingPoints(remaining)
                    .build();
        } catch (DuplicateKeyException ex) {
            pointsRequestIdempotencyService.release(requestId);
            MobiPointsLog existing = mobiPointsLogService.findByRequestId(requestId);
            if (existing != null) {
                pointsRequestIdempotencyService.complete(requestId, existing.getLogId());
                return buildResultFromLog(existing.getLogId());
            }
            throw new ServiceException("请求处理中，请稍后重试");
        } catch (Exception ex) {
            pointsRequestIdempotencyService.release(requestId);
            throw ex;
        }
    }

    private BigDecimal calcRechargeGift(Long shopId, BigDecimal basePoints) {
        List<ActivityRes> ongoing = activityApplicationQueryService.listOngoingForCustomer(shopId);
        BigDecimal bestGift = BigDecimal.ZERO;
        for (ActivityRes activity : ongoing) {
            if (!Integer.valueOf(ActivityType.RECHARGE_GIFT.getCode()).equals(activity.getActivityType())) {
                continue;
            }
            List<ActivityRuleDTO> rules = activity.getRules() == null
                    ? List.of()
                    : activity.getRules().stream()
                    .map(rule -> ActivityRuleDTO.builder()
                            .thresholdAmount(rule.getThresholdAmount())
                            .giftPoints(rule.getGiftPoints())
                            .build())
                    .toList();
            BigDecimal gift = RechargeGiftCalculator.calcGiftPoints(basePoints, rules);
            if (gift.compareTo(bestGift) > 0) {
                bestGift = gift;
            }
        }
        return bestGift;
    }

    private PointsRechargeResultVO buildResultFromLog(Long logId) {
        MobiPointsLog log = mobiPointsLogService.getById(logId);
        if (log == null) {
            throw new ServiceException("充值记录不存在");
        }
        BigDecimal base = safe(log.getChangeBase());
        BigDecimal gift = safe(log.getChangeBonus());
        BigDecimal afterBase = safe(log.getAfterBasePoints());
        BigDecimal afterBonus = safe(log.getAfterBonusPoints());
        return PointsRechargeResultVO.builder()
                .logId(logId)
                .basePoints(base)
                .giftPoints(gift)
                .totalPoints(base.add(gift))
                .remainingPoints(afterBase.add(afterBonus))
                .build();
    }

    private ShopBriefDTO resolveShopBrief(Long shopId) {
        Map<Long, ShopBriefDTO> cached = ShopCache.getByIds(List.of(shopId));
        ShopBriefDTO brief = cached.get(shopId);
        if (brief == null) {
            List<ShopBriefDTO> loaded = mobiShopService.listBriefByIds(List.of(shopId));
            if (loaded.isEmpty()) {
                return null;
            }
            brief = loaded.getFirst();
            ShopCache.putAll(loaded);
        }
        return brief;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
