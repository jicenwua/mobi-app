package com.xcz.member.customer.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.customer.domain.vo.PointsReceiptItemVO;
import com.xcz.member.customer.infrastructure.entity.MobiReceipt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户消费小票服务层
 */
public interface MobiReceiptService extends IService<MobiReceipt> {
    /**
     * 按积分流水 ID 批量查询小票商品行
     *
     * @param logIds 积分流水 ID 集合
     * @return key 为 logId，value 为商品明细列表
     */
    Map<Long, List<PointsReceiptItemVO>> mapItemsByLogIds(Collection<Long> logIds);
}
