package com.xcz.member.customer.application.service.shop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.api.dto.response.shop.ShopStatisticsRes;
import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import com.xcz.member.customer.domain.service.MobiShopService;
import com.xcz.member.customer.domain.service.MobiShopUserService;
import com.xcz.member.customer.domain.vo.ShopIncreasePointsVO;
import com.xcz.member.customer.domain.vo.ShopStatisticsDailyVO;
import com.xcz.member.customer.infrastructure.entity.MobiShop;
import com.xcz.member.customer.infrastructure.mapper.ShopStatisticsMapper;
import com.xcz.member.customer.utils.ShopAccessUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 店铺统计读侧应用服务。
 */
@Service
public class ShopStatisticsApplicationQueryService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_STAT_DAYS = 366;

    @Resource
    private ShopStatisticsMapper shopStatisticsMapper;
    @Resource
    private MobiShopService mobiShopService;
    @Resource
    private MobiShopUserService mobiShopUserService;
    /**
     * 小程序端：店长/店员查看店铺统计。
     *
     * @param shopId       当前管理店铺
     * @param filterShopId 筛选店铺（总店可传分店 ID 或本店 ID；null 时总店汇总全部门店）
     */
    public ShopStatisticsRes getForStaff(Long shopId, Long filterShopId, String startDate, String endDate) {
        if (shopId == null) {
            throw new ServiceException("店铺 ID 不能为空");
        }
        ShopAccessUtils.assertShopStaff(mobiShopUserService, shopId);
        return buildStatistics(shopId, filterShopId, true, resolveDateRange(startDate, endDate));
    }

    /**
     * 管理后台：可按店铺筛选，不传 shopId 时统计全系统。
     */
    public ShopStatisticsRes getForAdmin(Long shopId, Long filterShopId, String startDate, String endDate) {
        DateRange range = resolveDateRange(startDate, endDate);
        if (shopId == null) {
            return buildSystemStatistics(range);
        }
        return buildStatistics(shopId, filterShopId, false, range);
    }

    private ShopStatisticsRes buildSystemStatistics(DateRange range) {
        List<Long> allShopIds = mobiShopService.list(new LambdaQueryWrapper<MobiShop>()
                        .select(MobiShop::getId)
                        .eq(MobiShop::getAuditStatus, 1)
                        .eq(MobiShop::getIsEnabled, 1))
                .stream()
                .map(MobiShop::getId)
                .toList();
        return assembleResult(allShopIds, Map.of(), false, List.of(), "all", null, range);
    }

    private ShopStatisticsRes buildStatistics(Long contextShopId,
                                              Long filterShopId,
                                              boolean staffContext,
                                              DateRange range) {
        ShopBriefDTO contextShop = requireShop(contextShopId);
        boolean headShop = isHeadShop(contextShop);
        List<ShopStatisticsRes.BranchShop> branches = headShop ? listBranches(contextShopId) : List.of();

        ResolvedScope scope = resolveScope(contextShopId, filterShopId, headShop, branches, staffContext);
        Map<Long, Integer> ratioMap = loadRatioMap(scope.shopIds());

        return assembleResult(
                scope.shopIds(),
                ratioMap,
                headShop,
                branches,
                scope.scopeLabel(),
                scope.filterShopId(),
                range);
    }

    private ShopStatisticsRes assembleResult(List<Long> shopIds,
                                             Map<Long, Integer> ratioMap,
                                             boolean headShop,
                                             List<ShopStatisticsRes.BranchShop> branches,
                                             String scope,
                                             Long filterShopId,
                                             DateRange range) {
        if (shopIds.isEmpty()) {
            return emptyResult(headShop, branches, scope, filterShopId, range);
        }

        LocalDateTime periodStart = range.startDateTime();
        LocalDateTime periodEnd = range.endExclusiveDateTime();

        Long todayNewUsers = defaultLong(shopStatisticsMapper.countNewUsers(shopIds, periodStart, periodEnd));
        Long todayOrders = defaultLong(shopStatisticsMapper.countOrders(shopIds, periodStart, periodEnd));
        BigDecimal todayRechargeAmount = calcRechargeAmount(
                shopStatisticsMapper.sumIncreasePointsByShop(shopIds, periodStart, periodEnd),
                ratioMap);

        List<ShopStatisticsRes.TrendPoint> trend = buildTrend(shopIds, ratioMap, range);
        BigDecimal totalPointsUsed = trend.stream()
                .map(ShopStatisticsRes.TrendPoint::getPointsUsed)
                .map(ShopStatisticsApplicationQueryService::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ShopStatisticsRes.builder()
                .todayNewUsers(todayNewUsers)
                .todayOrders(todayOrders)
                .totalPointsUsed(totalPointsUsed)
                .todayRechargeAmount(todayRechargeAmount)
                .trend(trend)
                .headShop(headShop)
                .branches(branches)
                .scope(scope)
                .filterShopId(filterShopId)
                .build();
    }

    private List<ShopStatisticsRes.TrendPoint> buildTrend(List<Long> shopIds,
                                                          Map<Long, Integer> ratioMap,
                                                          DateRange range) {
        LocalDateTime trendStart = range.startDateTime();
        LocalDateTime trendEnd = range.endExclusiveDateTime();
        Map<LocalDate, Long> newUsersMap = toDailyLongMap(
                shopStatisticsMapper.groupNewUsersByDay(shopIds, trendStart, trendEnd),
                true);
        Map<LocalDate, Long> ordersMap = toDailyLongMap(
                shopStatisticsMapper.groupOrdersByDay(shopIds, trendStart, trendEnd),
                false);
        Map<LocalDate, BigDecimal> pointsUsedMap = toDailyDecimalMap(
                shopStatisticsMapper.groupPointsUsedByDay(shopIds, trendStart, trendEnd));
        Map<LocalDate, BigDecimal> increasePointsMap = toDailyDecimalMap(
                shopStatisticsMapper.groupIncreasePointsByDay(shopIds, trendStart, trendEnd));

        List<ShopStatisticsRes.TrendPoint> trend = new ArrayList<>(range.dayCount());
        for (LocalDate day = range.start(); !day.isAfter(range.end()); day = day.plusDays(1)) {
            BigDecimal dayIncrease = increasePointsMap.getOrDefault(day, BigDecimal.ZERO);
            trend.add(ShopStatisticsRes.TrendPoint.builder()
                    .date(day.format(DATE_FMT))
                    .newUsers(newUsersMap.getOrDefault(day, 0L))
                    .orders(ordersMap.getOrDefault(day, 0L))
                    .pointsUsed(pointsUsedMap.getOrDefault(day, BigDecimal.ZERO))
                    .rechargeAmount(convertPointsToAmount(dayIncrease, ratioMap))
                    .build());
        }
        return trend;
    }

    private Map<LocalDate, Long> toDailyLongMap(List<ShopStatisticsDailyVO> rows, boolean newUsers) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (ShopStatisticsDailyVO row : rows) {
            if (row.getStatDate() == null) {
                continue;
            }
            map.put(row.getStatDate(), newUsers ? defaultLong(row.getNewUsers()) : defaultLong(row.getOrders()));
        }
        return map;
    }

    private Map<LocalDate, BigDecimal> toDailyDecimalMap(List<ShopStatisticsDailyVO> rows) {
        Map<LocalDate, BigDecimal> map = new HashMap<>();
        for (ShopStatisticsDailyVO row : rows) {
            if (row.getStatDate() == null) {
                continue;
            }
            BigDecimal value = row.getPointsUsed() != null
                    ? row.getPointsUsed()
                    : row.getIncreasePoints();
            map.put(row.getStatDate(), defaultDecimal(value));
        }
        return map;
    }

    private BigDecimal calcRechargeAmount(List<ShopIncreasePointsVO> rows, Map<Long, Integer> ratioMap) {
        if (rows == null || rows.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ShopIncreasePointsVO row : rows) {
            if (row.getShopId() == null) {
                continue;
            }
            Integer ratio = ratioMap.get(row.getShopId());
            if (ratio == null || ratio <= 0) {
                continue;
            }
            BigDecimal points = defaultDecimal(row.getIncreasePoints());
            if (points.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            total = total.add(points.divide(BigDecimal.valueOf(ratio), 2, RoundingMode.HALF_UP));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal convertPointsToAmount(BigDecimal points, Map<Long, Integer> ratioMap) {
        if (points == null || points.compareTo(BigDecimal.ZERO) <= 0 || ratioMap.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int avgRatio = (int) Math.round(ratioMap.values().stream()
                .filter(r -> r != null && r > 0)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(1));
        if (avgRatio <= 0) {
            avgRatio = 1;
        }
        return points.divide(BigDecimal.valueOf(avgRatio), 2, RoundingMode.HALF_UP);
    }

    private Map<Long, Integer> loadRatioMap(List<Long> shopIds) {
        if (shopIds.isEmpty()) {
            return Map.of();
        }
        List<ShopBriefDTO> briefs = mobiShopService.listBriefByIds(shopIds);
        Map<Long, Integer> ratioMap = new LinkedHashMap<>();
        for (ShopBriefDTO brief : briefs) {
            if (brief.id() != null && brief.ratio() != null) {
                ratioMap.put(brief.id(), brief.ratio());
            }
        }
        return ratioMap;
    }

    private ResolvedScope resolveScope(Long contextShopId,
                                       Long filterShopId,
                                       boolean headShop,
                                       List<ShopStatisticsRes.BranchShop> branches,
                                       boolean staffContext) {
        if (filterShopId != null) {
            assertAccessibleShop(contextShopId, filterShopId, headShop, branches);
            if (filterShopId.equals(contextShopId)) {
                return new ResolvedScope(List.of(contextShopId), "self", filterShopId);
            }
            return new ResolvedScope(List.of(filterShopId), "shop", filterShopId);
        }

        if (headShop) {
            List<Long> shopIds = new ArrayList<>();
            shopIds.add(contextShopId);
            for (ShopStatisticsRes.BranchShop branch : branches) {
                shopIds.add(branch.getId());
            }
            return new ResolvedScope(shopIds, "all", null);
        }

        return new ResolvedScope(List.of(contextShopId), staffContext ? "self" : "shop", null);
    }

    private void assertAccessibleShop(Long contextShopId,
                                      Long filterShopId,
                                      boolean headShop,
                                      List<ShopStatisticsRes.BranchShop> branches) {
        if (filterShopId.equals(contextShopId)) {
            return;
        }
        if (!headShop) {
            throw new ServiceException("无权查看该店铺统计");
        }
        boolean allowed = branches.stream().anyMatch(b -> filterShopId.equals(b.getId()));
        if (!allowed) {
            throw new ServiceException("分店不存在或不属于当前总店");
        }
    }

    private List<ShopStatisticsRes.BranchShop> listBranches(Long headShopId) {
        return mobiShopService.list(new LambdaQueryWrapper<MobiShop>()
                        .select(MobiShop::getId, MobiShop::getShopName)
                        .eq(MobiShop::getParentId, headShopId)
                        .eq(MobiShop::getAuditStatus, 1)
                        .eq(MobiShop::getIsEnabled, 1)
                        .orderByAsc(MobiShop::getId))
                .stream()
                .map(shop -> ShopStatisticsRes.BranchShop.builder()
                        .id(shop.getId())
                        .shopName(shop.getShopName())
                        .build())
                .collect(Collectors.toList());
    }

    private ShopBriefDTO requireShop(Long shopId) {
        List<ShopBriefDTO> briefs = mobiShopService.listBriefByIds(List.of(shopId));
        if (briefs.isEmpty() || briefs.get(0) == null) {
            throw new ServiceException("店铺不存在");
        }
        return briefs.get(0);
    }

    private boolean isHeadShop(ShopBriefDTO shop) {
        return shop.parentId() == null || shop.parentId() == 0L;
    }

    private ShopStatisticsRes emptyResult(boolean headShop,
                                          List<ShopStatisticsRes.BranchShop> branches,
                                          String scope,
                                          Long filterShopId,
                                          DateRange range) {
        List<ShopStatisticsRes.TrendPoint> trend = new ArrayList<>(range.dayCount());
        for (LocalDate day = range.start(); !day.isAfter(range.end()); day = day.plusDays(1)) {
            trend.add(ShopStatisticsRes.TrendPoint.builder()
                    .date(day.format(DATE_FMT))
                    .newUsers(0L)
                    .orders(0L)
                    .pointsUsed(BigDecimal.ZERO)
                    .rechargeAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        return ShopStatisticsRes.builder()
                .todayNewUsers(0L)
                .todayOrders(0L)
                .totalPointsUsed(BigDecimal.ZERO)
                .todayRechargeAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .trend(trend)
                .headShop(headShop)
                .branches(branches)
                .scope(scope)
                .filterShopId(filterShopId)
                .build();
    }

    private static Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private DateRange resolveDateRange(String startDate, String endDate) {
        LocalDate end = parseDate(endDate);
        if (end == null) {
            end = LocalDate.now();
        }
        LocalDate start = parseDate(startDate);
        if (start == null) {
            start = end;
        }
        if (start.isAfter(end)) {
            throw new ServiceException("开始日期不能晚于结束日期");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_STAT_DAYS) {
            throw new ServiceException("统计时间范围不能超过366天");
        }
        return new DateRange(start, end);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim(), DATE_FMT);
    }

    private record DateRange(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime() {
            return start.atStartOfDay();
        }

        LocalDateTime endExclusiveDateTime() {
            return end.plusDays(1).atStartOfDay();
        }

        int dayCount() {
            return (int) ChronoUnit.DAYS.between(start, end) + 1;
        }
    }

    private record ResolvedScope(List<Long> shopIds, String scopeLabel, Long filterShopId) {
    }
}
