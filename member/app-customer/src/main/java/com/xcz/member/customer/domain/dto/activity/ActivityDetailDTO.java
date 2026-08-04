package com.xcz.member.customer.domain.dto.activity;

import com.xcz.member.customer.infrastructure.cache.dto.ActivityDetailCacheDTO;
import com.xcz.member.customer.infrastructure.cache.dto.ActivityRuleCacheDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动详情领域读模型（应用层内部使用，可写入 Redis 缓存）。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDetailDTO {

    /***活动 ID**/
    private Long activityId;
    /***店铺 ID**/
    private Long shopId;
    /***店铺名称（管理后台列表展示，可选）**/
    private String shopName;
    /***活动名称**/
    private String activityName;
    /***活动描述**/
    private String description;
    /***活动种类：1-满赠活动 2-公告**/
    private Integer kind;
    /***满赠子类型：1-充值满赠 2-消费满赠**/
    private Integer activityType;
    /***开始时间**/
    private LocalDateTime startTime;
    /***结束时间**/
    private LocalDateTime endTime;
    /***活动状态：0-未开始 1-进行中 2-已结束 3-手动停止**/
    private Integer status;
    /***创建时间**/
    private LocalDateTime createTime;
    /***修改时间**/
    private LocalDateTime updateTime;
    /***满赠规则列表（公告为空列表）**/
    private List<ActivityRuleDTO> rules;

    public static ActivityDetailDTO toDto(ActivityDetailCacheDTO cacheDTO) {
        if (cacheDTO == null) {
            return null;
        }
        return ActivityDetailDTO.builder()
                .activityId(cacheDTO.getActivityId())
                .shopId(cacheDTO.getShopId())
                .activityName(cacheDTO.getActivityName())
                .description(cacheDTO.getDescription())
                .kind(cacheDTO.getKind())
                .activityType(cacheDTO.getActivityType())
                .startTime(cacheDTO.getStartTime())
                .endTime(cacheDTO.getEndTime())
                .status(cacheDTO.getStatus())
                .createTime(cacheDTO.getCreateTime())
                .rules(ActivityRuleCacheDTO.toDtoList(cacheDTO.getRules()))
                .build();
    }

}
