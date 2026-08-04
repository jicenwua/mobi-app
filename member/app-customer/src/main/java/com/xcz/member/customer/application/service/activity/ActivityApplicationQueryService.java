package com.xcz.member.customer.application.service.activity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.PageUtils;
import com.xcz.member.customer.application.assemblers.ActivityAssembler;
import com.xcz.member.customer.application.command.activity.QueryActivityCommand;
import com.xcz.member.customer.api.dto.response.activity.ActivityRes;
import com.xcz.member.customer.domain.dto.activity.ActivityDetailDTO;
import com.xcz.member.customer.domain.dto.activity.ActivityRuleDTO;
import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import com.xcz.member.customer.domain.enums.ActivityKind;
import com.xcz.member.customer.domain.enums.ActivityStatus;
import com.xcz.member.customer.domain.promotion.ActivityListOrder;
import com.xcz.member.customer.domain.service.MobiActivityRuleService;
import com.xcz.member.customer.domain.service.MobiActivityService;
import com.xcz.member.customer.domain.service.MobiShopService;
import com.xcz.member.customer.domain.service.MobiShopUserService;
import com.xcz.member.customer.infrastructure.cache.ActivityCache;
import com.xcz.member.customer.infrastructure.entity.MobiActivity;
import com.xcz.member.customer.infrastructure.entity.MobiActivityRule;
import com.xcz.member.customer.utils.ShopAccessUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 活动读侧应用服务。
 */
@Service
public class ActivityApplicationQueryService {

    @Resource
    private MobiActivityService mobiActivityService;
    @Resource
    private MobiActivityRuleService mobiActivityRuleService;
    @Resource
    private MobiShopUserService mobiShopUserService;
    @Resource
    private MobiShopService mobiShopService;

    /**
     * 管理后台 Feign：活动列表（店铺可选，无店铺时按活动 ID 倒序）。
     */
    public Page<ActivityRes> pageForFeign(QueryActivityCommand command) {
        if (command.shopId() == null) {
            return pageAllForFeign(command);
        }
        List<ActivityRes> all = mapFeignList(
                ActivityListOrder.sortForStaff(
                                shopActivities(command.shopId()).stream()
                                        .map(this::refreshStatus)
                                        .toList()).stream()
                        .filter(detail -> command.kind() == null || command.kind().equals(detail.getKind()))
                        .filter(detail -> matchStatus(detail.getStatus(), command.status()))
                        .filter(detail -> command.activityType() == null || command.activityType().equals(detail.getActivityType()))
                        .toList());
        return slicePage(all, command.resolvedPageNum(), command.resolvedPageSize());
    }

    private Page<ActivityRes> pageAllForFeign(QueryActivityCommand command) {
        LocalDateTime now = LocalDateTime.now();
        int pageNum = command.resolvedPageNum();
        int pageSize = command.resolvedPageSize();
        LambdaQueryWrapper<MobiActivity> wrapper = new LambdaQueryWrapper<MobiActivity>()
                .eq(command.kind() != null, MobiActivity::getKind, command.kind())
                .eq(command.status() != null, MobiActivity::getStatus, command.status())
                .eq(command.activityType() != null, MobiActivity::getActivityType, command.activityType())
                .orderByDesc(MobiActivity::getActivityId);
        Page<MobiActivity> entityPage = mobiActivityService.page(new Page<>(pageNum, pageSize), wrapper);
        syncActivityStatuses(entityPage.getRecords(), now);
        List<ActivityDetailDTO> details = toDetailList(entityPage.getRecords());
        Map<Long, String> shopNames = loadShopNameMap(details);
        List<ActivityRes> records = details.stream()
                .map(this::refreshStatus)
                .map(detail -> detail.toBuilder().shopName(shopNames.get(detail.getShopId())).build())
                .map(ActivityAssembler::toVo)
                .toList();
        Page<ActivityRes> page = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        page.setRecords(records);
        return page;
    }

    private List<ActivityRes> mapFeignList(List<ActivityDetailDTO> details) {
        Map<Long, String> shopNames = loadShopNameMap(details);
        return details.stream()
                .map(detail -> detail.toBuilder().shopName(shopNames.get(detail.getShopId())).build())
                .map(ActivityAssembler::toVo)
                .toList();
    }

    private Map<Long, String> loadShopNameMap(List<ActivityDetailDTO> details) {
        if (details == null || details.isEmpty()) {
            return Map.of();
        }
        List<Long> shopIds = details.stream()
                .map(ActivityDetailDTO::getShopId)
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
     * 查询活动详情（已加入店铺的成员均可查看；管理后台跳过成员校验并附带店铺名）。
     */
    public ActivityRes getDetail(Long activityId) {
        ActivityDetailDTO cached = ActivityCache.getByActivityId(activityId);
        ActivityRes res;
        if (cached != null) {
            res = ActivityAssembler.toVo(refreshStatus(cached));
        } else {
            MobiActivity entity = mobiActivityService.getById(activityId);
            if (entity == null) {
                throw new ServiceException("活动不存在", 404);
            }
            assertReadable(entity);
            syncActivityStatus(entity, LocalDateTime.now());
            ActivityDetailDTO detail = buildDetail(entity);
            ActivityCache.put(entity.getShopId(), detail);
            res = ActivityAssembler.toVo(detail);
        }
        if (ShopAccessUtils.isFeignInvoke() && res != null && res.getShopId() != null) {
            Map<Long, String> shopNames = loadShopNameMap(List.of(
                    ActivityDetailDTO.builder().shopId(res.getShopId()).build()));
            res.setShopName(shopNames.get(res.getShopId()));
        }
        return res;
    }

    /**
     * 店员/店长：查看店铺全部活动（可按状态筛选），含满赠规则，走 Redis。
     */
    public Page<ActivityRes> pageForStaff(QueryActivityCommand command) {
        ShopAccessUtils.assertShopStaff(mobiShopUserService, command.shopId());
        List<ActivityRes> all = ActivityListOrder.sortForStaff(
                        shopActivities(command.shopId()).stream()
                                .map(this::refreshStatus)
                                .toList()).stream()
                .filter(detail -> command.kind() == null || command.kind().equals(detail.getKind()))
                .filter(detail -> matchStatus(detail.getStatus(), command.status()))
                .map(ActivityAssembler::toVo)
                .toList();
        return slicePage(all, command.resolvedPageNum(), command.resolvedPageSize());
    }

    /**
     * 查看店铺当前进行中的满赠活动（不含公告），含满赠规则，走 Redis。
     */
    public List<ActivityRes> listOngoingForCustomer(Long shopId) {
        ShopAccessUtils.assertShopMember(mobiShopUserService, shopId);
        return shopActivities(shopId).stream()
                .map(this::refreshStatus)
                .filter(this::isPromotionKind)
                .filter(detail -> ActivityStatus.ONGOING.getCode().equals(detail.getStatus()))
                .map(ActivityAssembler::toVo)
                .toList();
    }

    /**
     * 获取店铺最新公告（含详情字段，走 Redis）。
     */
    public ActivityRes getLatestAnnouncement(Long shopId) {
        ShopAccessUtils.assertShopMember(mobiShopUserService, shopId);
        ActivityDetailDTO detail = shopActivities(shopId).stream()
                .map(this::refreshStatus)   //获取的时候实时刷新活动状态
                .filter(item -> ActivityKind.ANNOUNCEMENT.getCode().equals(item.getKind())) //过滤需要的活动类型
                .filter(item -> ActivityStatus.ONGOING.getCode().equals(item.getStatus()))  //过滤正在进行的活动
                .max(Comparator.comparing(ActivityDetailDTO::getCreateTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        return ActivityAssembler.toVo(detail);
    }

    /**
     * 读取店铺全部活动（优先 Redis Map，未命中回源 DB）。
     */
    private List<ActivityDetailDTO> shopActivities(Long shopId) {
        return ActivityCache.getShopActivities(shopId, () -> loadShopDetailsFromDb(shopId));
    }

    /**
     * 从数据库加载店铺全量活动详情（含规则）。
     */
    private List<ActivityDetailDTO> loadShopDetailsFromDb(Long shopId) {
        LocalDateTime now = LocalDateTime.now();
        List<MobiActivity> activities = mobiActivityService.list(
                new LambdaQueryWrapper<MobiActivity>()
                        .eq(MobiActivity::getShopId, shopId));
        syncActivityStatuses(activities, now);
        return toDetailList(activities);
    }

    /**
     * 批量组装活动详情读模型（调用前需已通过 syncActivityStatuses 对齐 status）。
     */
    private List<ActivityDetailDTO> toDetailList(List<MobiActivity> activities) {
        if (activities == null || activities.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ActivityRuleDTO>> rulesMap = loadRulesMap(
                activities.stream().map(MobiActivity::getActivityId).toList());
        return activities.stream()
                .map(entity -> ActivityAssembler.toDetailDto(entity, rulesMap.get(entity.getActivityId())))
                .toList();
    }

    /**
     * 从数据库组装活动详情读模型。
     */
    private ActivityDetailDTO buildDetail(MobiActivity entity) {
        List<ActivityRuleDTO> rules = mobiActivityRuleService.listByActivityId(entity.getActivityId()).stream()
                .sorted(Comparator.comparing(MobiActivityRule::getThresholdAmount))
                .map(ActivityAssembler::toRuleDto)
                .toList();
        return ActivityAssembler.toDetailDto(entity, rules);
    }

    /**
     * 批量加载活动满赠规则并按活动 ID 分组。
     */
    private Map<Long, List<ActivityRuleDTO>> loadRulesMap(List<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Map.of();
        }
        return mobiActivityRuleService.list(
                        new LambdaQueryWrapper<MobiActivityRule>().in(MobiActivityRule::getActivityId, activityIds))
                .stream()
                .map(ActivityAssembler::toRuleDto)
                .collect(Collectors.groupingBy(ActivityRuleDTO::getActivityId));
    }

    /**
     * 按当前时间刷新活动状态（避免缓存过期）。
     */
    private ActivityDetailDTO refreshStatus(ActivityDetailDTO detail) {
        if (detail == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        MobiActivity probe = MobiActivity.builder()
                .status(detail.getStatus())
                .startTime(detail.getStartTime())
                .endTime(detail.getEndTime())
                .build();
        ActivityStatus status = ActivityStatus.resolve(probe, now);
        return detail.toBuilder()
                .status(status == null ? detail.getStatus() : status.getCode())
                .build();
    }

    /**
     * 将单条活动状态与当前时间对齐并写回数据库。
     */
    private void syncActivityStatus(MobiActivity activity, LocalDateTime now) {
        if (activity == null) {
            return;
        }
        ActivityStatus resolved = ActivityStatus.resolve(activity, now);
        if (resolved != null && !resolved.getCode().equals(activity.getStatus())) {
            activity.setStatus(resolved.getCode());
            activity.setUpdateTime(now);
            mobiActivityService.updateById(activity);
        }
    }

    /**
     * 批量将活动状态与当前时间对齐并写回数据库。
     */
    private void syncActivityStatuses(List<MobiActivity> activities, LocalDateTime now) {
        if (activities == null || activities.isEmpty()) {
            return;
        }
        for (MobiActivity activity : activities) {
            syncActivityStatus(activity, now);
        }
    }

    /**
     * 判断是否活动不是公告
     */
    private boolean isPromotionKind(ActivityDetailDTO detail) {
        return detail.getKind() == null || ActivityKind.PROMOTION.getCode().equals(detail.getKind());
    }

    /**
     * 校验当前用户是否有权查看该活动（已加入店铺的任意角色均可访问）。
     */
    private void assertReadable(MobiActivity entity) {
        ShopAccessUtils.assertShopMember(mobiShopUserService, entity.getShopId());
    }

    /**
     * 按状态编码过滤列表。
     */
    private boolean matchStatus(Integer actualStatus, Integer expectedStatus) {
        if (expectedStatus == null) {
            return true;
        }
        return expectedStatus.equals(actualStatus);
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
