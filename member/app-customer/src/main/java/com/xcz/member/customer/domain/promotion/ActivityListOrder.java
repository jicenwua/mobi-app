package com.xcz.member.customer.domain.promotion;

import com.xcz.member.customer.domain.dto.activity.ActivityDetailDTO;
import com.xcz.member.customer.domain.enums.ActivityStatus;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 店员/店长活动列表排序：先展示进行中，再展示其余；组内按结束时间升序。
 */
public final class ActivityListOrder {

    private ActivityListOrder() {
    }

    /**
     * 店员活动列表排序。
     * <p>
     * 第一组：进行中（结束时间升序，最晚结束的排在后面）；<br>
     * 第二组：非进行中（结束时间升序，最早结束的排在前面）。
     */
    public static List<ActivityDetailDTO> sortForStaff(List<ActivityDetailDTO> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        Comparator<LocalDateTime> endAsc = Comparator.nullsLast(Comparator.naturalOrder());
        return list.stream()
                .sorted((left, right) -> {
                    boolean leftOngoing = isOngoingGroup(left);
                    boolean rightOngoing = isOngoingGroup(right);
                    if (leftOngoing != rightOngoing) {
                        return leftOngoing ? -1 : 1;
                    }
                    return endAsc.compare(left.getEndTime(), right.getEndTime());
                })
                .toList();
    }

    /**
     * 判读活动是否在进行中
     */
    private static boolean isOngoingGroup(ActivityDetailDTO detail) {
        return detail != null && ActivityStatus.ONGOING.getCode().equals(detail.getStatus());
    }
}
