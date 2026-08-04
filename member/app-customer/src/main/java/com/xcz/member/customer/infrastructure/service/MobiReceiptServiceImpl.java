package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.member.customer.domain.service.MobiReceiptService;
import com.xcz.member.customer.domain.vo.PointsReceiptItemVO;
import com.xcz.member.customer.infrastructure.entity.MobiReceipt;
import com.xcz.member.customer.infrastructure.mapper.MobiReceiptMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户消费小票服务实现类
 */
@Service
public class MobiReceiptServiceImpl extends ServiceImpl<MobiReceiptMapper, MobiReceipt> implements MobiReceiptService {
    @Resource
    private MobiReceiptMapper mobiReceiptMapper;

    /**
     * 按积分流水 ID 批量查询小票商品行
     *
     * @param logIds 积分流水 ID 集合
     * @return key 为 logId，value 为该流水下的商品明细列表
     */
    @Override
    public Map<Long, List<PointsReceiptItemVO>> mapItemsByLogIds(Collection<Long> logIds) {
        if (CollectionUtils.isEmpty(logIds)) {
            return Collections.emptyMap();
        }
        List<PointsReceiptItemVO> rows = mobiReceiptMapper.selectItemsByLogIds(List.copyOf(logIds));
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyMap();
        }
        return rows.stream().collect(Collectors.groupingBy(PointsReceiptItemVO::getLogId));
    }
}
