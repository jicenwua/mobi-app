package com.xcz.member.customer.infrastructure.mapper;

import com.xcz.member.customer.domain.vo.ShopIncreasePointsVO;
import com.xcz.member.customer.domain.vo.ShopStatisticsDailyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 店铺统计聚合查询。
 */
@Mapper
public interface ShopStatisticsMapper {

    Long countNewUsers(@Param("shopIds") List<Long> shopIds,
                       @Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime);

    Long countOrders(@Param("shopIds") List<Long> shopIds,
                     @Param("startTime") LocalDateTime startTime,
                     @Param("endTime") LocalDateTime endTime);

    BigDecimal sumPointsUsed(@Param("shopIds") List<Long> shopIds,
                             @Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime);

    List<ShopIncreasePointsVO> sumIncreasePointsByShop(@Param("shopIds") List<Long> shopIds,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    List<ShopStatisticsDailyVO> groupNewUsersByDay(@Param("shopIds") List<Long> shopIds,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    List<ShopStatisticsDailyVO> groupOrdersByDay(@Param("shopIds") List<Long> shopIds,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    List<ShopStatisticsDailyVO> groupPointsUsedByDay(@Param("shopIds") List<Long> shopIds,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    List<ShopStatisticsDailyVO> groupIncreasePointsByDay(@Param("shopIds") List<Long> shopIds,
                                                         @Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime);
}
