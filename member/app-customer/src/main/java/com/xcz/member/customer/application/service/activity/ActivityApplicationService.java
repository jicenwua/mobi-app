package com.xcz.member.customer.application.service.activity;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.application.command.activity.MutateActivityCommand;
import com.xcz.member.customer.domain.dto.activity.ActivityRuleDTO;
import com.xcz.member.customer.domain.enums.ActivityKind;
import com.xcz.member.customer.domain.enums.ActivityStatus;
import com.xcz.member.customer.domain.promotion.ActivityScheduleConflictChecker;
import com.xcz.member.customer.domain.promotion.ActivityTimeNormalizer;
import com.xcz.member.customer.domain.service.MobiActivityRuleService;
import com.xcz.member.customer.domain.service.MobiActivityService;
import com.xcz.member.customer.domain.service.MobiShopUserService;
import com.xcz.member.customer.infrastructure.entity.MobiActivity;
import com.xcz.member.customer.infrastructure.entity.MobiActivityRule;
import com.xcz.member.customer.utils.ShopAccessUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 活动写侧应用服务。
 */
@Service
public class ActivityApplicationService {

    @Resource
    private MobiActivityService mobiActivityService;
    @Resource
    private MobiActivityRuleService mobiActivityRuleService;
    @Resource
    private MobiShopUserService mobiShopUserService;

    /**
     * 创建活动（店长权限；公告不可含规则）。
     *
     * @param command 写操作命令
     * @return 新活动 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(MutateActivityCommand command) {
        ShopAccessUtils.assertShopManager(mobiShopUserService, command.shopId());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = ActivityTimeNormalizer.normalizeStartForCreate(command.startTime(), now);
        LocalDateTime endTime = ActivityTimeNormalizer.normalizeEnd(command.endTime());
        ActivityTimeNormalizer.assertValidRange(startTime, endTime);
        assertNoScheduleConflict(command.shopId(), command.kind(), command.activityType(),
                startTime, endTime, null);
        MobiActivity statusProbe = MobiActivity.builder()
                .startTime(startTime)
                .endTime(endTime)
                .build();
        MobiActivity activity = MobiActivity.builder()
                .shopId(command.shopId())
                .activityName(command.activityName())
                .description(command.description())
                .kind(command.kind())
                .activityType(Objects.equals(ActivityKind.ANNOUNCEMENT.getCode(), command.kind()) ? null : command.activityType())
                .startTime(startTime)
                .endTime(endTime)
                .status(ActivityStatus.resolve(statusProbe, now).getCode())
                .createTime(now)
                .updateTime(now)
                .build();
        mobiActivityService.save(activity);
        saveRules(activity.getActivityId(), command.kind(), command.rules());
        return activity.getActivityId();
    }

    /**
     * 更新活动：未开始可改全部字段；其余阶段可改时间与描述；任意阶段均可改时间。
     * 手动停止的活动仅更新时间，状态保持手动停止，需调用启用接口后才会重新生效。
     *
     * @param command 写操作命令
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(MutateActivityCommand command) {
        ShopAccessUtils.assertShopManager(mobiShopUserService, command.shopId());
        MobiActivity existing = loadAndAssertShop(command.activityId(), command.shopId());
        LocalDateTime now = LocalDateTime.now();
        boolean manuallyStopped = ActivityStatus.MANUALLY_STOPPED.getCode().equals(existing.getStatus());
        ActivityStatus status = ActivityStatus.resolve(existing, now);

        LocalDateTime startTime;
        LocalDateTime endTime;
        if (manuallyStopped) {
            startTime = ActivityTimeNormalizer.normalizeStartForAdjust(command.startTime(), existing);
            endTime = ActivityTimeNormalizer.normalizeEnd(command.endTime());
        } else if (status == ActivityStatus.NOT_STARTED) {
            existing.setActivityName(command.activityName());
            existing.setDescription(command.description());
            existing.setKind(command.kind());
            existing.setActivityType(ActivityKind.ANNOUNCEMENT.getCode() == command.kind() ? null : command.activityType());
            startTime = ActivityTimeNormalizer.normalizeStartForUpdate(command.startTime(), existing);
            endTime = ActivityTimeNormalizer.normalizeEnd(command.endTime());
            mobiActivityRuleService.removeByActivityId(existing.getActivityId());
            saveRules(existing.getActivityId(), command.kind(), command.rules());
        } else {
            if (command.description() != null) {
                existing.setDescription(command.description());
            }
            startTime = ActivityTimeNormalizer.normalizeStartForAdjust(command.startTime(), existing);
            endTime = ActivityTimeNormalizer.normalizeEnd(command.endTime());
        }

        ActivityTimeNormalizer.assertValidRange(startTime, endTime);
        Integer conflictKind = manuallyStopped || status != ActivityStatus.NOT_STARTED
                ? existing.getKind() : command.kind();
        Integer conflictType = manuallyStopped || status != ActivityStatus.NOT_STARTED
                ? existing.getActivityType()
                : (ActivityKind.ANNOUNCEMENT.getCode() == command.kind() ? null : command.activityType());
        assertNoScheduleConflict(command.shopId(), conflictKind, conflictType,
                startTime, endTime, command.activityId());
        existing.setStartTime(startTime);
        existing.setEndTime(endTime);
        existing.setUpdateTime(now);
        if (!manuallyStopped) {
            existing.setStatus(ActivityStatus.resolveFromTime(startTime, endTime, now).getCode());
        }
        mobiActivityService.updateById(existing);
    }

    /**
     * 重新启用手动停止的活动：按当前时间窗口重算状态后生效。
     *
     * @param activityId 活动 ID
     * @param shopId     店铺 ID（小程序端必传；管理后台可传 null）
     */
    @Transactional(rollbackFor = Exception.class)
    public void enable(Long activityId, Long shopId) {
        MobiActivity existing = resolveActivity(activityId, shopId);
        ShopAccessUtils.assertShopManager(mobiShopUserService, existing.getShopId());
        if (!ActivityStatus.MANUALLY_STOPPED.getCode().equals(existing.getStatus())) {
            throw new ServiceException("仅手动停止的活动可重新启用", 400);
        }
        LocalDateTime now = LocalDateTime.now();
        assertNoScheduleConflict(existing.getShopId(), existing.getKind(), existing.getActivityType(),
                existing.getStartTime(), existing.getEndTime(), existing.getActivityId());
        existing.setStatus(ActivityStatus.resolveFromTime(
                existing.getStartTime(), existing.getEndTime(), now).getCode());
        existing.setUpdateTime(now);
        mobiActivityService.updateById(existing);
    }

    /**
     * 手动结束活动。
     *
     * @param activityId 活动 ID
     * @param shopId     店铺 ID（小程序端必传；管理后台可传 null）
     */
    @Transactional(rollbackFor = Exception.class)
    public void stop(Long activityId, Long shopId) {
        MobiActivity existing = resolveActivity(activityId, shopId);
        ShopAccessUtils.assertShopManager(mobiShopUserService, existing.getShopId());
        existing.setStatus(ActivityStatus.MANUALLY_STOPPED.getCode());
        existing.setUpdateTime(LocalDateTime.now());
        mobiActivityService.updateById(existing);
    }

    /**
     * 删除活动（进行中的活动不可删除）。
     *
     * @param activityId 活动 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long activityId) {
        MobiActivity existing = requireActivity(activityId);
        ShopAccessUtils.assertShopManager(mobiShopUserService, existing.getShopId());
        if (ActivityStatus.isOngoing(existing, LocalDateTime.now())) {
            throw new ServiceException("进行中的活动不能删除，请先手动结束", 400);
        }
        mobiActivityRuleService.removeByActivityId(activityId);
        mobiActivityService.removeById(activityId);
    }

    /** 校验同店铺、同满赠子类型下是否存在时间重合的有效活动。 */
    private void assertNoScheduleConflict(Long shopId, Integer kind, Integer activityType,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          Long excludeActivityId) {
        List<MobiActivity> shopActivities = mobiActivityService.lambdaQuery()
                .eq(MobiActivity::getShopId, shopId)
                .list();
        ActivityScheduleConflictChecker.assertNoConflict(
                shopId, kind, activityType, startTime, endTime, excludeActivityId, shopActivities);
    }

    /** 保存满赠规则（公告跳过）。 */
    private void saveRules(Long activityId, Integer kind, List<ActivityRuleDTO> rules) {
        if (ActivityKind.ANNOUNCEMENT.getCode() == kind || rules == null || rules.isEmpty()) {
            return;
        }
        List<MobiActivityRule> entities = rules.stream()
                .map(rule -> MobiActivityRule.builder()
                        .activityId(activityId)
                        .thresholdAmount(rule.getThresholdAmount())
                        .giftPoints(rule.getGiftPoints())
                        .build())
                .toList();
        mobiActivityRuleService.saveBatch(entities);
    }

    private MobiActivity requireActivity(Long activityId) {
        MobiActivity activity = mobiActivityService.getById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在", 404);
        }
        return activity;
    }

    private MobiActivity resolveActivity(Long activityId, Long shopId) {
        return shopId != null ? loadAndAssertShop(activityId, shopId) : requireActivity(activityId);
    }

    /** 加载活动并校验店铺归属。 */
    private MobiActivity loadAndAssertShop(Long activityId, Long shopId) {
        MobiActivity activity = mobiActivityService.getById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在", 404);
        }
        if (!activity.getShopId().equals(shopId)) {
            throw new ServiceException("店铺 ID 与活动不匹配", 400);
        }
        return activity;
    }
}
