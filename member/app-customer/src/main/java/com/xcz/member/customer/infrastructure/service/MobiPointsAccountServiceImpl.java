package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.domain.dto.points.PointsShopQuery;
import com.xcz.member.customer.domain.service.MobiPointsAccountService;
import com.xcz.member.customer.domain.vo.PointsAccountVO;
import com.xcz.member.customer.infrastructure.entity.MobiPointsAccount;
import com.xcz.member.customer.infrastructure.mapper.MobiPointsAccountMapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 店铺会员积分账户基础设施服务：账户查询与创建。
 */
@Service
public class MobiPointsAccountServiceImpl extends ServiceImpl<MobiPointsAccountMapper, MobiPointsAccount> implements MobiPointsAccountService {

    @Resource
    private MobiPointsAccountMapper mobiPointsAccountMapper;

    /**
     * 分页查询店铺下会员积分账户。
     *
     * @param query 店铺 ID、关键字、分页参数
     * @return 积分账户 VO 分页
     */
    @Override
    public Page<PointsAccountVO> pageByShop(PointsShopQuery query) {
        Page<PointsAccountVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return mobiPointsAccountMapper.selectShopPointsPage(page, query);
    }

    /**
     * 查询用户在指定店铺的积分账户。
     *
     * @param userId 用户 ID
     * @param shopId 店铺 ID
     * @return 积分账户；不存在时返回 null
     */
    @Override
    public MobiPointsAccount getByUserIdAndShopId(Long userId, Long shopId) {
        return mobiPointsAccountMapper.selectOne(
                new LambdaQueryWrapper<MobiPointsAccount>()
                        .eq(MobiPointsAccount::getUserId, userId)
                        .eq(MobiPointsAccount::getShopId, shopId)
        );
    }

    /**
     * 获取或懒创建积分账户（并发下依赖唯一索引兜底）。
     *
     * @param userId 用户 ID
     * @param shopId 店铺 ID
     * @return 已存在或新创建的积分账户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MobiPointsAccount getOrCreateAccount(Long userId, Long shopId) {
        MobiPointsAccount account = getByUserIdAndShopId(userId, shopId);
        if (account == null) {
            account = MobiPointsAccount.builder()
                    .userId(userId)
                    .shopId(shopId)
                    .basePoints(BigDecimal.ZERO)
                    .bonusPoints(BigDecimal.ZERO)
                    .totalUsedPoints(BigDecimal.ZERO)
                    .updateTime(LocalDateTime.now())
                    .build();
            try {
                save(account);
            } catch (DuplicateKeyException ex) {
                account = getByUserIdAndShopId(userId, shopId);
                if (account == null) {
                    throw new ServiceException("积分账户创建失败");
                }
            }
        }
        return account;
    }

    /**
     * 按 userId + shopId 复合主键更新账户余额字段。
     *
     * @param account 含最新余额的账户实体
     */
    @Override
    public void updateBalance(MobiPointsAccount account) {
        if (account == null || account.getUserId() == null || account.getShopId() == null) {
            throw new ServiceException("积分账户主键无效");
        }
        boolean updated = update(account, new LambdaUpdateWrapper<MobiPointsAccount>()
                .eq(MobiPointsAccount::getUserId, account.getUserId())
                .eq(MobiPointsAccount::getShopId, account.getShopId()));
        if (!updated) {
            throw new ServiceException("积分账户更新失败");
        }
    }

    /**
     * 原子扣减积分（基础分 + 赠送分），返回受影响行数。
     */
    @Override
    public int deductPoints(Long userId, Long shopId, BigDecimal deductBase, BigDecimal deductBonus, BigDecimal total) {
        return mobiPointsAccountMapper.deductPoints(userId, shopId, deductBase, deductBonus, total);
    }

    /**
     * 原子退还积分（退款/撤销场景），返回受影响行数。
     */
    @Override
    public int refundPoints(Long userId, Long shopId, BigDecimal refundBase, BigDecimal refundBonus, BigDecimal refundTotal) {
        return mobiPointsAccountMapper.refundPoints(userId, shopId, refundBase, refundBonus, refundTotal);
    }

    /**
     * 后台调整积分（可增可减），返回受影响行数。
     */
    @Override
    public int adjustPoints(Long userId, Long shopId, BigDecimal deltaBase, BigDecimal deltaBonus) {
        if (userId == null || shopId == null) {
            throw new ServiceException("积分账户主键无效");
        }
        if (deltaBase == null) {
            deltaBase = BigDecimal.ZERO;
        }
        if (deltaBonus == null) {
            deltaBonus = BigDecimal.ZERO;
        }
        if (deltaBase.compareTo(BigDecimal.ZERO) == 0 && deltaBonus.compareTo(BigDecimal.ZERO) == 0) {
            throw new ServiceException("调整数量无效");
        }
        return mobiPointsAccountMapper.adjustPoints(userId, shopId, deltaBase, deltaBonus);
    }
}
