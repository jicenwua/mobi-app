package com.xcz.member.customer.domain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.customer.domain.dto.points.PointsShopQuery;
import com.xcz.member.customer.domain.vo.PointsAccountVO;
import com.xcz.member.customer.infrastructure.entity.MobiPointsAccount;

import java.math.BigDecimal;

/**
 * 店铺会员积分余额服务层
 */
public interface MobiPointsAccountService extends IService<MobiPointsAccount> {
    /**
     * 分页查询店铺下用户积分账户
     *
     * @param query 查询条件（shopId、昵称、手机号、排序、分页）
     * @return 积分账户分页结果
     */
    Page<PointsAccountVO> pageByShop(PointsShopQuery query);

    /**
     * 根据用户ID和店铺ID查询积分账户
     *
     * @param userId 用户ID
     * @param shopId 店铺ID
     * @return 积分账户信息
     */
    MobiPointsAccount getByUserIdAndShopId(Long userId, Long shopId);

    /**
     * 创建或获取积分账户
     *
     * @param userId 用户ID
     * @param shopId 店铺ID
     * @return 积分账户信息
     */
    MobiPointsAccount getOrCreateAccount(Long userId, Long shopId);

    /**
     * 按联合主键（userId + shopId）更新积分余额。
     *
     * @param account 待更新账户（须含 userId、shopId）
     */
    void updateBalance(MobiPointsAccount account);

    /**
     * 原子扣减积分，余额不足时返回 0。
     *
     * @param userId      用户 ID
     * @param shopId      店铺 ID
     * @param deductBase  扣减基础积分
     * @param deductBonus 扣减赠送积分
     * @param total       扣减合计
     * @return 影响行数
     */
    int deductPoints(Long userId, Long shopId, BigDecimal deductBase, BigDecimal deductBonus, BigDecimal total);

    /**
     * 原子退还积分（退款场景）。
     *
     * @param userId      用户 ID
     * @param shopId      店铺 ID
     * @param refundBase  退还基础积分
     * @param refundBonus 退还赠送积分
     * @param refundTotal 退还合计
     * @return 影响行数
     */
    int refundPoints(Long userId, Long shopId, BigDecimal refundBase, BigDecimal refundBonus, BigDecimal refundTotal);

    /**
     * 原子调整积分（可增可减）。
     */
    int adjustPoints(Long userId, Long shopId, BigDecimal deltaBase, BigDecimal deltaBonus);
}
