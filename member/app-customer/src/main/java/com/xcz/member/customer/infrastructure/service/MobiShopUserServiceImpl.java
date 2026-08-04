package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.domain.enums.ShopUserRole;
import com.xcz.member.customer.domain.service.MobiShopUserService;
import com.xcz.member.customer.infrastructure.entity.MobiShopUser;
import com.xcz.member.customer.infrastructure.mapper.MobiShopUserMapper;
import com.xcz.member.customer.infrastructure.cache.ShopCache;
import com.xcz.member.customer.utils.ShopAccessUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 店铺与用户关系的基础设施服务实现。
 * <p>
 * 负责维护 userId + shopId 维度的角色关系；凡变更角色的写操作在成功后
 * 会自动清除 {@link ShopAccessUtils} 中的角色缓存，避免调用方遗漏导致权限不一致。
 */
@Service
public class MobiShopUserServiceImpl extends ServiceImpl<MobiShopUserMapper, MobiShopUser> implements MobiShopUserService {

    /**
     * 根据用户 ID 查询其关联的全部店铺关系。
     *
     * @param userId 用户 ID
     * @return 店铺用户关系列表，按创建时间降序
     */
    @Override
    public List<MobiShopUser> listByUserId(Long userId) {
        LambdaQueryWrapper<MobiShopUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MobiShopUser::getUserId, userId);
        wrapper.orderByDesc(MobiShopUser::getCreateTime);
        return list(wrapper);
    }

    /**
     * 查询指定用户在指定店铺中的关系记录。
     *
     * @param shopId 店铺 ID
     * @param userId 用户 ID
     * @return 关系实体；不存在时返回 null
     */
    @Override
    public MobiShopUser getByShopIdAndUserId(Long shopId, Long userId) {
        LambdaQueryWrapper<MobiShopUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MobiShopUser::getShopId, shopId)
                .eq(MobiShopUser::getUserId, userId);
        return getOne(wrapper);
    }

    /**
     * 新建店铺用户关系（同一 userId + shopId 不可重复）。
     *
     * @param shopId 店铺 ID
     * @param userId 用户 ID
     * @param role   角色编码（1-店长, 2-店员, 3-顾客）
     * @return 是否保存成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addShopUser(Long shopId, Long userId, Integer role) {
        MobiShopUser existing = getByShopIdAndUserId(shopId, userId);
        if (existing != null) {
            throw new ServiceException("已在该店铺中");
        }

        MobiShopUser shopUser = MobiShopUser.builder()
                .shopId(shopId)
                .userId(userId)
                .role(role)
                .createTime(LocalDateTime.now())
                .build();
        boolean saved = save(shopUser);
        if (saved) {
            ShopAccessUtils.clearRoleCache(userId);
        }
        return saved;
    }

    /**
     * 将用户设为指定店铺的店长。
     *
     * @param shopId 店铺 ID
     * @param userId 用户 ID
     * @return 是否设置成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setShopManager(Long shopId, Long userId) {
        return addShopUser(shopId, userId, ShopUserRole.MANAGER.getCode());
    }

    /**
     * 用户以顾客身份进入店铺：无关系时新建顾客记录；已有关系时合并顾客角色并更新最近进入时间。
     *
     * @param shopId 店铺 ID
     * @param userId 用户 ID
     * @return 是否处理成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enterShopAsCustomer(Long shopId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        MobiShopUser existing = getByShopIdAndUserId(shopId, userId);
        if (existing != null) {
            existing.setRole(ShopUserRole.mergeCustomerRole(existing.getRole()));
            existing.setLastEnterTime(now);
            boolean updated = updateById(existing);
            if (updated) {
                ShopAccessUtils.clearRoleCache(userId);
                recordShopEnter(shopId, userId);
            }
            return updated;
        }

        MobiShopUser shopUser = MobiShopUser.builder()
                .shopId(shopId)
                .userId(userId)
                .role(ShopUserRole.CUSTOMER.getCode())
                .lastEnterTime(now)
                .createTime(now)
                .build();
        boolean saved = save(shopUser);
        if (saved) {
            ShopAccessUtils.clearRoleCache(userId);
            recordShopEnter(shopId, userId);
        }
        return saved;
    }

    /**
     * 将用户设为店铺店员：无关系时新建；顾客可升级为店员；已是店长/店员则拒绝。
     *
     * @param shopId 店铺 ID
     * @param userId 用户 ID
     * @return 是否设置成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignShopClerk(Long shopId, Long userId) {
        MobiShopUser existing = getByShopIdAndUserId(shopId, userId);
        if (existing == null) {
            MobiShopUser shopUser = MobiShopUser.builder()
                    .shopId(shopId)
                    .userId(userId)
                    .role(ShopUserRole.CLERK.getCode())
                    .createTime(LocalDateTime.now())
                    .build();
            boolean saved = save(shopUser);
            if (saved) {
                ShopAccessUtils.clearRoleCache(userId);
            }
            return saved;
        }
        Integer roleCode = existing.getRole();
        if (ShopUserRole.MANAGER.getCode().equals(roleCode)) {
            throw new ServiceException("该用户已是店长");
        }
        if (ShopUserRole.CLERK.getCode().equals(roleCode) || Integer.valueOf(4).equals(roleCode)) {
            throw new ServiceException("该用户已是店员");
        }
        existing.setRole(ShopUserRole.CLERK.getCode());
        boolean updated = updateById(existing);
        if (updated) {
            ShopAccessUtils.clearRoleCache(userId);
        }
        return updated;
    }

    /**
     * 查询店铺下全部店员（含历史角色编码 4）。
     *
     * @param shopId 店铺 ID
     * @return 店员关系列表
     */
    @Override
    public List<MobiShopUser> listClerksByShopId(Long shopId) {
        LambdaQueryWrapper<MobiShopUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MobiShopUser::getShopId, shopId)
                .in(MobiShopUser::getRole, ShopUserRole.CLERK.getCode(), 4)
                .orderByDesc(MobiShopUser::getCreateTime);
        return list(wrapper);
    }

    /**
     * 移除店员身份（降为顾客），店长不可被移除。
     *
     * @param shopId 店铺 ID
     * @param userId 用户 ID
     * @return 是否移除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeShopClerk(Long shopId, Long userId) {
        MobiShopUser existing = getByShopIdAndUserId(shopId, userId);
        if (existing == null) {
            throw new ServiceException("该用户不在本店");
        }
        Integer role = existing.getRole();
        if (ShopUserRole.MANAGER.getCode().equals(role)) {
            throw new ServiceException("不能移除店长");
        }
        if (!ShopUserRole.CLERK.getCode().equals(role) && !Integer.valueOf(4).equals(role)) {
            throw new ServiceException("该用户不是店员");
        }
        existing.setRole(ShopUserRole.CUSTOMER.getCode());
        boolean updated = updateById(existing);
        if (updated) {
            ShopAccessUtils.clearRoleCache(userId);
        }
        return updated;
    }

    /**
     * 记录用户进入店铺时间（Redis 缓存，定时任务落库）。
     */
    @Override
    public void recordShopEnter(Long shopId, Long userId) {
        ShopCache.recordEnter(userId, shopId);
    }
}
