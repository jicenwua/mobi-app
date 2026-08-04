package com.xcz.member.customer.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcz.member.customer.domain.vo.PointsReceiptItemVO;
import com.xcz.member.customer.infrastructure.entity.MobiReceipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消费收据 Mapper。
 */
@Mapper
public interface MobiReceiptMapper extends BaseMapper<MobiReceipt> {

    /**
     * 按流水 ID 批量查询消费商品明细。
     *
     * @param logIds 流水 ID 列表
     * @return 商品明细列表
     */
    List<PointsReceiptItemVO> selectItemsByLogIds(@Param("logIds") List<Long> logIds);
}
