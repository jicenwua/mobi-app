package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.member.customer.domain.dto.points.PointsShopQuery;
import com.xcz.member.customer.domain.enums.PointsLogStatus;
import com.xcz.member.customer.domain.service.MobiPointsLogService;
import com.xcz.member.customer.domain.service.MobiReceiptService;
import com.xcz.member.customer.domain.vo.PointsLogBriefVO;
import com.xcz.member.customer.domain.vo.PointsReceiptItemVO;
import com.xcz.member.customer.infrastructure.entity.MobiPointsLog;
import com.xcz.member.customer.infrastructure.mapper.MobiPointsLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 积分变动流水服务实现：分页查询、幂等检索与核销状态更新。
 */
@Service
public class MobiPointsLogServiceImpl extends ServiceImpl<MobiPointsLogMapper, MobiPointsLog> implements MobiPointsLogService {

    @Resource
    private MobiPointsLogMapper mobiPointsLogMapper;
    @Resource
    private MobiReceiptService mobiReceiptService;

    /**
     * 分页查询店铺积分流水，可选附带消费小票明细。
     *
     * @param query        查询条件
     * @param includeItems 是否在结果中填充小票商品行
     * @return 流水分页
     */
    @Override
    public Page<PointsLogBriefVO> pageLogByShop(PointsShopQuery query, boolean includeItems) {
        Page<PointsLogBriefVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        page = mobiPointsLogMapper.selectShopLogPage(page, query);
        if (includeItems) {
            attachReceiptItems(page.getRecords());
        }
        return page;
    }

    /**
     * 查询单条流水详情（含小票商品行）。
     *
     * @param logId 流水 ID
     * @return 流水详情；不存在时返回 null
     */
    @Override
    public PointsLogBriefVO getLogDetail(Long logId) {
        if (logId == null) {
            return null;
        }
        PointsLogBriefVO detail = mobiPointsLogMapper.selectLogDetail(logId);
        if (detail == null) {
            return null;
        }
        attachReceiptItems(List.of(detail));
        return detail;
    }

    /**
     * 按幂等 requestId 查找流水（防重复提交）。
     *
     * @param requestId 客户端请求唯一标识
     * @return 已存在的流水；未找到时返回 null
     */
    @Override
    public MobiPointsLog findByRequestId(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return null;
        }
        return lambdaQuery()
                .eq(MobiPointsLog::getRequestId, requestId.trim())
                .last("LIMIT 1")
                .one();
    }

    /**
     * 将待核销订单标记为已核销（乐观条件更新，返回 0 表示状态不匹配或不存在）。
     *
     * @param logId       流水 ID
     * @param shopId      店铺 ID
     * @param consumeTime 核销时间
     * @return 受影响行数
     */
    @Override
    public int verifyPendingOrder(Long logId, Long shopId, LocalDateTime consumeTime) {
        if (logId == null || shopId == null) {
            return 0;
        }
        return lambdaUpdate()
                .eq(MobiPointsLog::getLogId, logId)
                .eq(MobiPointsLog::getShopId, shopId)
                .eq(MobiPointsLog::getStatus, PointsLogStatus.PENDING_VERIFY.getCode())
                .set(MobiPointsLog::getStatus, PointsLogStatus.VERIFIED.getCode())
                .set(MobiPointsLog::getConsumeTime, consumeTime)
                .update() ? 1 : 0;
    }

    /**
     * 为流水记录批量附加消费小票商品明细。
     *
     * @param records 流水列表
     */
    private void attachReceiptItems(List<PointsLogBriefVO> records) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }
        List<Long> logIds = records.stream()
                .map(PointsLogBriefVO::getLogId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (logIds.isEmpty()) {
            return;
        }
        Map<Long, List<PointsReceiptItemVO>> itemsMap = mobiReceiptService.mapItemsByLogIds(logIds);
        for (PointsLogBriefVO row : records) {
            List<PointsReceiptItemVO> items = itemsMap.getOrDefault(row.getLogId(), Collections.emptyList());
            row.setItems(items);
        }
    }
}
