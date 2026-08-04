package com.xcz.member.customer.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.member.customer.domain.dto.points.PointsShopQuery;
import com.xcz.member.customer.domain.vo.PointsLogBriefVO;
import com.xcz.member.customer.infrastructure.entity.MobiPointsLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 积分变动流水 Mapper。
 */
@Mapper
public interface MobiPointsLogMapper extends BaseMapper<MobiPointsLog> {

    /**
     * 分页查询店铺积分流水。
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 流水分页结果
     */
    Page<PointsLogBriefVO> selectShopLogPage(Page<PointsLogBriefVO> page, @Param("query") PointsShopQuery query);

    /**
     * 按流水 ID 查询消费详情。
     *
     * @param logId 流水 ID
     * @return 流水详情
     */
    PointsLogBriefVO selectLogDetail(@Param("logId") Long logId);
}
