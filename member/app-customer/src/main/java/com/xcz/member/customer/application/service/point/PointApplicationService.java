package com.xcz.member.customer.application.service.point;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import com.xcz.member.customer.domain.enums.PointsActionType;
import com.xcz.member.customer.domain.enums.PointsLogStatus;
import com.xcz.member.customer.domain.service.MobiPointsAccountService;
import com.xcz.member.customer.domain.service.MobiPointsLogService;
import com.xcz.member.customer.domain.service.MobiShopService;
import com.xcz.member.customer.infrastructure.cache.ShopCache;
import com.xcz.member.customer.infrastructure.entity.MobiPointsAccount;
import com.xcz.member.customer.infrastructure.entity.MobiPointsLog;
import com.xcz.member.feign.dto.points.PointsAdjustFeign;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 积分写侧应用服务（管理后台手动调整等）。
 */
@Service
public class PointApplicationService {

    @Resource
    private MobiPointsAccountService mobiPointsAccountService;
    @Resource
    private MobiPointsLogService mobiPointsLogService;
    @Resource
    private MobiShopService mobiShopService;

    /**
     * 管理后台：手动增减用户基础/赠送积分。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjustPoints(PointsAdjustFeign body) {
        if (body == null || body.getUserId() == null || body.getShopId() == null) {
            throw new ServiceException("用户与店铺不能为空", 400);
        }
        if (body.getPointType() == null || (body.getPointType() != 1 && body.getPointType() != 2)) {
            throw new ServiceException("请选择积分类型", 400);
        }
        if (body.getDirection() == null || (body.getDirection() != 1 && body.getDirection() != 2)) {
            throw new ServiceException("请选择操作类型", 400);
        }
        BigDecimal amount = body.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("调整数量须大于 0", 400);
        }
        String remark = body.getRemark() == null ? "" : body.getRemark().trim();
        if (!StringUtils.hasText(remark)) {
            throw new ServiceException("请填写备注", 400);
        }
        if (remark.length() > 200) {
            throw new ServiceException("备注不能超过200字", 400);
        }

        Long shopId = resolvePointsShopId(body.getShopId());
        Long userId = body.getUserId();
        boolean adjustBase = body.getPointType() == 1;
        boolean isAdd = body.getDirection() == 1;
        BigDecimal delta = isAdd ? amount : amount.negate();
        BigDecimal deltaBase = adjustBase ? delta : BigDecimal.ZERO;
        BigDecimal deltaBonus = adjustBase ? BigDecimal.ZERO : delta;

        MobiPointsAccount account = mobiPointsAccountService.getOrCreateAccount(userId, shopId);
        BigDecimal preBase = safe(account.getBasePoints());
        BigDecimal preBonus = safe(account.getBonusPoints());

        if (!isAdd) {
            BigDecimal available = adjustBase ? preBase : preBonus;
            if (available.compareTo(amount) < 0) {
                String label = adjustBase ? "基础积分" : "赠送积分";
                throw new ServiceException(label + "不足，当前剩余 " + available.stripTrailingZeros().toPlainString());
            }
        }

        int updated = mobiPointsAccountService.adjustPoints(userId, shopId, deltaBase, deltaBonus);
        if (updated == 0) {
            throw new ServiceException("积分调整失败，请重试");
        }

        BigDecimal afterBase = preBase.add(deltaBase);
        BigDecimal afterBonus = preBonus.add(deltaBonus);
        LocalDateTime now = LocalDateTime.now();
        MobiPointsLog pointsLog = MobiPointsLog.builder()
                .userId(userId)
                .shopId(shopId)
                .actionType(isAdd ? PointsActionType.INCREASE.getCode() : PointsActionType.CONSUME.getCode())
                .preBasePoints(preBase)
                .preBonusPoints(preBonus)
                .changeBase(deltaBase)
                .changeBonus(deltaBonus)
                .afterBasePoints(afterBase)
                .afterBonusPoints(afterBonus)
                .status(isAdd ? PointsLogStatus.ADMIN_ADD.getCode() : PointsLogStatus.ADMIN_DEDUCT.getCode())
                .remark(remark)
                .createTime(now)
                .consumeTime(now)
                .build();
        mobiPointsLogService.save(pointsLog);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long resolvePointsShopId(Long shopId) {
        Map<Long, ShopBriefDTO> cached = ShopCache.getByIds(List.of(shopId));
        ShopBriefDTO brief = cached.get(shopId);
        if (brief == null) {
            List<ShopBriefDTO> loaded = mobiShopService.listBriefByIds(List.of(shopId));
            if (loaded.isEmpty()) {
                throw new ServiceException("店铺不存在");
            }
            brief = loaded.getFirst();
            ShopCache.putAll(loaded);
        }
        return brief.headShopId();
    }
}
