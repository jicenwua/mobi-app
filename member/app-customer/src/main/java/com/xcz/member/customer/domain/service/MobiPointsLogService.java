package com.xcz.member.customer.domain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.customer.domain.dto.points.PointsShopQuery;
import com.xcz.member.customer.domain.vo.PointsLogBriefVO;
import com.xcz.member.customer.infrastructure.entity.MobiPointsLog;

import java.time.LocalDateTime;

/**
 * 积分变动流水服务
 */
public interface MobiPointsLogService extends IService<MobiPointsLog> {

    /**
     * 分页查询店铺积分流水。
     *
     * @param query        查询条件（shopId 必填，可含 userId、筛选与分页）
     * @param includeItems 是否在列表中附带小票商品明细（管理端 true，小程序列表 false）
     * @return 流水分页结果
     */
    Page<PointsLogBriefVO> pageLogByShop(PointsShopQuery query, boolean includeItems);

    /**
     * 按流水 ID 查询消费详情（含商品明细与优惠券信息）。
     *
     * @param logId 流水 ID
     * @return 流水详情；不存在时返回 null
     */
    PointsLogBriefVO getLogDetail(Long logId);

    /**
     * 按幂等请求 ID 查询消费流水。
     */
    MobiPointsLog findByRequestId(String requestId);

    /**
     * 将待使用订单核销为已核销（乐观锁，仅 status=待使用时更新）。
     *
     * @return 更新行数
     */
    int verifyPendingOrder(Long logId, Long shopId, LocalDateTime consumeTime);
}
