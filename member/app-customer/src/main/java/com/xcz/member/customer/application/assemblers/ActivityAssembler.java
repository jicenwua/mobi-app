package com.xcz.member.customer.application.assemblers;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.customer.api.dto.request.activity.ActivityReq;
import com.xcz.member.customer.api.dto.response.activity.ActivityRes;
import com.xcz.member.customer.api.dto.response.activity.ActivityRuleRes;
import com.xcz.member.customer.application.command.activity.MutateActivityCommand;
import com.xcz.member.customer.application.command.activity.QueryActivityCommand;
import com.xcz.member.customer.domain.dto.activity.ActivityBriefDTO;
import com.xcz.member.customer.domain.dto.activity.ActivityDetailDTO;
import com.xcz.member.customer.domain.dto.activity.ActivityRuleDTO;
import com.xcz.member.customer.domain.enums.ActivityKind;
import com.xcz.member.customer.domain.enums.ActivityType;
import com.xcz.member.customer.domain.promotion.ActivityTimeNormalizer;
import com.xcz.member.customer.infrastructure.entity.MobiActivity;
import com.xcz.member.customer.infrastructure.entity.MobiActivityRule;
import com.xcz.member.feign.dto.activity.MobiActivityDetailVO;
import com.xcz.member.feign.dto.activity.MobiActivityFeign;
import com.xcz.member.feign.dto.activity.MobiActivityRuleFeign;
import com.xcz.member.feign.dto.activity.MobiActivitySaveDTO;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 活动对象转换器：Controller Req ↔ Command ↔ 领域读模型 ↔ API Res。
 * 写操作按场景校验字段后组装为 {@link MutateActivityCommand}。
 */
public final class ActivityAssembler {

    private ActivityAssembler() {
    }

    /**
     * 将 HTTP 查询请求转为活动列表查询命令。
     *
     * @param dto 活动查询参数
     * @return 查询命令
     */
    public static QueryActivityCommand toQueryCommand(ActivityReq dto) {
        if (dto.getShopId() == null) {
            throw new ServiceException("店铺 ID 不能为空", 400);
        }
        return new QueryActivityCommand(
                dto.getShopId(),
                dto.getKind(),
                dto.getPageNum(),
                dto.getPageSize(),
                dto.getStatus(),
                null
        );
    }

    /**
     * 管理后台 Feign 活动列表查询命令。
     */
    public static QueryActivityCommand toFeignQueryCommand(Long shopId, int pageNum, int pageSize,
                                                         Integer status, Integer activityType) {
        return new QueryActivityCommand(shopId, null, pageNum, pageSize, status, activityType);
    }

    /**
     * 活动响应 → Feign 列表 DTO。
     */
    public static MobiActivityFeign toFeign(ActivityRes source) {
        if (source == null) {
            return null;
        }
        return MobiActivityFeign.builder()
                .activityId(source.getActivityId())
                .shopId(source.getShopId())
                .shopName(source.getShopName())
                .activityName(source.getActivityName())
                .activityType(source.getActivityType())
                .startTime(source.getStartTime())
                .endTime(source.getEndTime())
                .status(source.getStatus())
                .createTime(source.getCreateTime())
                .build();
    }

    /**
     * 活动响应 → Feign 详情 DTO。
     */
    public static MobiActivityDetailVO toDetailFeign(ActivityRes source) {
        if (source == null) {
            return null;
        }
        return MobiActivityDetailVO.builder()
                .activityId(source.getActivityId())
                .shopId(source.getShopId())
                .shopName(source.getShopName())
                .activityName(source.getActivityName())
                .activityType(source.getActivityType())
                .startTime(source.getStartTime())
                .endTime(source.getEndTime())
                .status(source.getStatus())
                .createTime(source.getCreateTime())
                .rules(toFeignRules(source.getRules()))
                .build();
    }

    /**
     * Feign 保存 DTO → 创建/更新命令。
     */
    public static MutateActivityCommand toSaveCommand(MobiActivitySaveDTO dto) {
        if (dto == null) {
            throw new ServiceException("活动数据不能为空", 400);
        }
        ActivityReq req = new ActivityReq();
        req.setShopId(dto.getShopId());
        req.setActivityName(dto.getActivityName());
        req.setActivityType(dto.getActivityType());
        req.setStartTime(dto.getStartTime());
        req.setEndTime(dto.getEndTime());
        req.setKind(ActivityKind.PROMOTION.getCode());
        req.setRules(toRuleDtos(dto.getRules()));
        if (dto.getActivityId() == null) {
            return toCreateCommand(req);
        }
        return toUpdateCommand(dto.getActivityId(), req);
    }

    private static List<ActivityRuleDTO> toRuleDtos(List<MobiActivityRuleFeign> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        return rules.stream()
                .map(rule -> ActivityRuleDTO.builder()
                        .thresholdAmount(rule.getThresholdAmount())
                        .giftPoints(rule.getGiftPoints())
                        .build())
                .toList();
    }

    private static List<MobiActivityRuleFeign> toFeignRules(List<ActivityRuleRes> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        return rules.stream()
                .map(rule -> MobiActivityRuleFeign.builder()
                        .ruleId(rule.getRuleId())
                        .activityId(rule.getActivityId())
                        .thresholdAmount(rule.getThresholdAmount())
                        .giftPoints(rule.getGiftPoints())
                        .build())
                .toList();
    }

    /**
     * 将 HTTP 请求体转为创建活动命令（校验全量字段）。
     *
     * @param dto 活动信息
     * @return 写操作命令
     */
    public static MutateActivityCommand toCreateCommand(ActivityReq dto) {
        assertShopId(dto.getShopId());
        assertFullPayload(dto);
        return new MutateActivityCommand(
                null,
                dto.getShopId(),
                dto.getActivityName(),
                dto.getDescription(),
                dto.getKind(),
                dto.getActivityType(),
                dto.getStartTime(),
                dto.getEndTime(),
                dto.getRules()
        );
    }

    /**
     * 将 HTTP 请求体转为全量更新活动命令（校验全量字段）。
     *
     * @param activityId 活动 ID
     * @param dto        活动信息
     * @return 写操作命令
     */
    public static MutateActivityCommand toUpdateCommand(Long activityId, ActivityReq dto) {
        assertActivityId(activityId);
        assertShopId(dto.getShopId());
        assertFullPayload(dto);
        return new MutateActivityCommand(
                activityId,
                dto.getShopId(),
                dto.getActivityName(),
                dto.getDescription(),
                dto.getKind(),
                dto.getActivityType(),
                dto.getStartTime(),
                dto.getEndTime(),
                dto.getRules()
        );
    }

    /**
     * 将 HTTP 请求体转为活动时间调整命令（仅校验时间与店铺）。
     *
     * @param activityId 活动 ID
     * @param dto        时间与描述
     * @return 写操作命令
     */
    public static MutateActivityCommand toAdjustTimeCommand(Long activityId, ActivityReq dto) {
        assertActivityId(activityId);
        assertShopId(dto.getShopId());
        assertOptionalTimeRange(dto.getStartTime(), dto.getEndTime());
        return new MutateActivityCommand(
                activityId,
                dto.getShopId(),
                null,
                dto.getDescription(),
                null,
                null,
                dto.getStartTime(),
                dto.getEndTime(),
                null
        );
    }

    /**
     * 将满赠规则实体转为领域 DTO。
     */
    public static ActivityRuleDTO toRuleDto(MobiActivityRule rule) {
        return ActivityRuleDTO.builder()
                .ruleId(rule.getRuleId())
                .activityId(rule.getActivityId())
                .thresholdAmount(rule.getThresholdAmount())
                .giftPoints(rule.getGiftPoints())
                .build();
    }

    /**
     * 将活动实体转为列表简要读模型。
     */
    public static ActivityBriefDTO toBriefDto(MobiActivity entity) {
        return ActivityBriefDTO.builder()
                .activityId(entity.getActivityId())
                .shopId(entity.getShopId())
                .activityName(entity.getActivityName())
                .description(entity.getDescription())
                .kind(entity.getKind())
                .activityType(entity.getActivityType())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .build();
    }

    /**
     * 将活动实体转为详情读模型。
     */
    public static ActivityDetailDTO toDetailDto(MobiActivity entity, List<ActivityRuleDTO> rules) {
        return ActivityDetailDTO.builder()
                .activityId(entity.getActivityId())
                .shopId(entity.getShopId())
                .activityName(entity.getActivityName())
                .description(entity.getDescription())
                .kind(entity.getKind())
                .activityType(entity.getActivityType())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .rules(rules == null ? Collections.emptyList() : rules)
                .build();
    }

    /**
     * 将活动简要读模型转为 API 响应。
     */
    public static ActivityRes toVo(ActivityBriefDTO source) {
        if (source == null) {
            return null;
        }
        return ActivityRes.builder()
                .activityId(source.getActivityId())
                .shopId(source.getShopId())
                .activityName(source.getActivityName())
                .description(source.getDescription())
                .kind(source.getKind())
                .activityType(source.getActivityType())
                .startTime(source.getStartTime())
                .endTime(source.getEndTime())
                .status(source.getStatus())
                .createTime(source.getCreateTime())
                .build();
    }

    /**
     * 将活动详情读模型转为 API 响应。
     */
    public static ActivityRes toVo(ActivityDetailDTO source) {
        if (source == null) {
            return null;
        }
        return ActivityRes.builder()
                .activityId(source.getActivityId())
                .shopId(source.getShopId())
                .shopName(source.getShopName())
                .activityName(source.getActivityName())
                .description(source.getDescription())
                .kind(source.getKind())
                .activityType(source.getActivityType())
                .startTime(source.getStartTime())
                .endTime(source.getEndTime())
                .status(source.getStatus())
                .createTime(source.getCreateTime())
                .rules(toRuleVos(source.getRules()))
                .build();
    }

    private static List<ActivityRuleRes> toRuleVos(List<ActivityRuleDTO> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        return rules.stream()
                .map(rule -> ActivityRuleRes.builder()
                        .ruleId(rule.getRuleId())
                        .activityId(rule.getActivityId())
                        .thresholdAmount(rule.getThresholdAmount())
                        .giftPoints(rule.getGiftPoints())
                        .build())
                .toList();
    }

    private static void assertShopId(Long shopId) {
        if (shopId == null) {
            throw new ServiceException("店铺 ID 不能为空", 400);
        }
    }

    private static void assertActivityId(Long activityId) {
        if (activityId == null) {
            throw new ServiceException("活动 ID 不能为空", 400);
        }
    }

    private static void assertOptionalTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null && endTime == null) {
            return;
        }
        LocalDateTime effectiveStart = startTime != null ? startTime : LocalDateTime.now();
        ActivityTimeNormalizer.assertValidRange(effectiveStart, endTime);
    }

    private static void assertFullPayload(ActivityReq dto) {
        if (StringUtils.isEmpty(dto.getActivityName())) {
            throw new ServiceException("活动名称不能为空", 400);
        }
        if (dto.getKind() == null) {
            dto.setKind(ActivityKind.PROMOTION.getCode());
        }
        if (ActivityKind.getByCode(dto.getKind()) == null) {
            throw new ServiceException("活动种类无效", 400);
        }
        assertOptionalTimeRange(dto.getStartTime(), dto.getEndTime());
        ActivityKind kind = ActivityKind.getByCode(dto.getKind());
        if (kind == ActivityKind.ANNOUNCEMENT) {
            if (StringUtils.isEmpty(dto.getDescription())) {
                throw new ServiceException("公告正文不能为空", 400);
            }
            if (dto.getRules() != null && !dto.getRules().isEmpty()) {
                throw new ServiceException("公告不能配置满赠规则", 400);
            }
        } else {
            if (ActivityType.getByCode(dto.getActivityType()) == null) {
                throw new ServiceException("满赠活动类型无效", 400);
            }
            assertRules(dto.getRules());
        }
    }

    private static void assertRules(List<ActivityRuleDTO> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new ServiceException("至少配置一条满赠规则", 400);
        }
        for (ActivityRuleDTO rule : rules) {
            if (rule.getThresholdAmount() == null || rule.getThresholdAmount() <= 0) {
                throw new ServiceException("门槛积分必须大于 0", 400);
            }
            if (rule.getGiftPoints() == null || rule.getGiftPoints() <= 0) {
                throw new ServiceException("赠送积分必须大于 0", 400);
            }
        }
    }
}
