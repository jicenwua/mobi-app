package com.xcz.member.customer.application.service.point;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.domain.dto.points.PointsPurchaseDTO;
import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import com.xcz.member.customer.domain.enums.PointsLogStatus;
import com.xcz.member.customer.domain.service.MobiPointsLogService;
import com.xcz.member.customer.domain.service.MobiShopService;
import com.xcz.member.customer.domain.service.MobiShopUserService;
import com.xcz.member.customer.domain.vo.PointsPurchaseResultVO;
import com.xcz.member.customer.infrastructure.cache.ShopCache;
import com.xcz.member.customer.infrastructure.entity.MobiPointsLog;
import com.xcz.member.customer.utils.ShopAccessUtils;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 会员自助购买应用服务。
 * <p>职责：权限与店铺上下文解析、确认下单（委托 {@link PointsConsumeApplicationService}）。</p>
 */
@Service
public class PurchaseApplicationService {

    @Resource
    private PointsConsumeApplicationService pointsConsumeApplicationService;
    @Resource
    private PointsRequestIdempotencyService pointsRequestIdempotencyService;
    @Resource
    private MobiShopUserService mobiShopUserService;
    @Resource
    private MobiShopService mobiShopService;
    @Resource
    private MobiPointsLogService mobiPointsLogService;

    /**
     * 确认购买：扣减积分、库存并直接完成消费。
     *
     * @param dto 购买请求（须携带 requestId 防重复提交）
     * @return 购买结果
     */
    @Transactional(rollbackFor = Exception.class)
    public PointsPurchaseResultVO purchase(PointsPurchaseDTO dto) {
        PurchaseContext ctx = resolveContext(dto);
        Long userId = ctx.userId();
        String requestId = dto.getRequestId().trim();

        Optional<Long> existingLogId = pointsRequestIdempotencyService.findCompletedLogId(requestId);
        if (existingLogId.isPresent()) {
            return buildResultFromLog(existingLogId.get(), userId);
        }

        pointsRequestIdempotencyService.acquireOrThrow(requestId);

        try {
            PointsConsumeApplicationService.ConsumeExecutionResult result =
                    pointsConsumeApplicationService.execute(
                            userId,
                            ctx.shopId(),
                            dto.getItems(),
                            PointsLogStatus.PENDING_VERIFY.getCode(),
                            dto.getUserCouponId(),
                            requestId);
            pointsRequestIdempotencyService.complete(requestId, result.logId());
            return toPurchaseResult(result);
        } catch (DuplicateKeyException ex) {
            pointsRequestIdempotencyService.release(requestId);
            return buildResultFromDuplicateRequest(requestId, userId);
        } catch (Exception ex) {
            pointsRequestIdempotencyService.release(requestId);
            throw ex;
        }
    }

    private PointsPurchaseResultVO buildResultFromDuplicateRequest(String requestId, Long userId) {
        MobiPointsLog log = mobiPointsLogService.findByRequestId(requestId);
        if (log == null || !userId.equals(log.getUserId())) {
            throw new ServiceException("请求处理中，请稍后重试");
        }
        pointsRequestIdempotencyService.complete(requestId, log.getLogId());
        return buildResultFromLog(log.getLogId(), userId);
    }

    /**
     * 解析购买上下文：校验店铺成员身份并加载店铺信息。
     */
    private PurchaseContext resolveContext(PointsPurchaseDTO dto) {
        Long shopId = dto.getShopId();
        Long userId = SecurityUtils.getUserId();
        ShopAccessUtils.assertShopMember(mobiShopUserService, shopId);

        ShopBriefDTO shop = resolveShopBrief(shopId);
        if (shop == null) {
            throw new ServiceException("店铺不存在");
        }
        return new PurchaseContext(userId, shopId, shop);
    }

    /**
     * 从已完成流水重建购买结果（幂等重放）。
     */
    private PointsPurchaseResultVO buildResultFromLog(Long logId, Long userId) {
        MobiPointsLog log = mobiPointsLogService.getById(logId);
        if (log == null || !userId.equals(log.getUserId())) {
            throw new ServiceException("订单不存在");
        }
        BigDecimal consume = safeDecimal(log.getChangeBase()).add(safeDecimal(log.getChangeBonus())).abs();
        int remaining = safeDecimal(log.getAfterBasePoints()).add(safeDecimal(log.getAfterBonusPoints())).intValue();
        return PointsPurchaseResultVO.builder()
                .logId(logId)
                .consumePoints(consume)
                .remainingPoints(remaining)
                .build();
    }

    private PointsPurchaseResultVO toPurchaseResult(PointsConsumeApplicationService.ConsumeExecutionResult result) {
        int remaining = safeDecimal(result.afterBase()).add(safeDecimal(result.afterBonus())).intValue();
        return PointsPurchaseResultVO.builder()
                .logId(result.logId())
                .originalPoints(result.originalPoints())
                .discountPoints(result.discountPoints())
                .consumePoints(result.payablePoints())
                .remainingPoints(remaining)
                .build();
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

    private record PurchaseContext(Long userId, Long shopId, ShopBriefDTO shop) {
    }
}
