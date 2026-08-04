package com.xcz.member.customer.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.member.customer.domain.dto.points.PointsShopQuery;
import com.xcz.member.customer.domain.vo.PointsAccountVO;
import com.xcz.member.customer.infrastructure.entity.MobiPointsAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 店铺会员积分账户 Mapper。
 */
@Mapper
public interface MobiPointsAccountMapper extends BaseMapper<MobiPointsAccount> {

    /**
     * 分页查询店铺下会员积分账户。
     *
     * @param page  分页参数
     * @param query 查询条件（shopId 必填）
     * @return 积分账户分页结果
     */
    Page<PointsAccountVO> selectShopPointsPage(Page<PointsAccountVO> page, @Param("query") PointsShopQuery query);

    /**
     * 原子扣减积分：余额不足时影响行数为 0。
     *
     * @param userId      用户 ID
     * @param shopId      店铺 ID
     * @param deductBase  扣减基础积分
     * @param deductBonus 扣减赠送积分
     * @param total       扣减合计（用于余额校验）
     * @return 影响行数
     */
    int deductPoints(@Param("userId") Long userId,
                     @Param("shopId") Long shopId,
                     @Param("deductBase") BigDecimal deductBase,
                     @Param("deductBonus") BigDecimal deductBonus,
                     @Param("total") BigDecimal total);

    /**
     * 原子退还积分（退款场景）。
     *
     * @param userId      用户 ID
     * @param shopId      店铺 ID
     * @param refundBase  退还基础积分
     * @param refundBonus 退还赠送积分
     * @param refundTotal 退还合计（用于冲减累计消耗）
     * @return 影响行数
     */
    int refundPoints(@Param("userId") Long userId,
                     @Param("shopId") Long shopId,
                     @Param("refundBase") BigDecimal refundBase,
                     @Param("refundBonus") BigDecimal refundBonus,
                     @Param("refundTotal") BigDecimal refundTotal);

    /**
     * 原子调整积分（可增可减，余额不足时影响行数为 0）。
     */
    int adjustPoints(@Param("userId") Long userId,
                     @Param("shopId") Long shopId,
                     @Param("deltaBase") BigDecimal deltaBase,
                     @Param("deltaBonus") BigDecimal deltaBonus);
}
