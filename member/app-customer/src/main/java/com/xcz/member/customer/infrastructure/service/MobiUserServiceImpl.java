package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.customer.domain.service.MobiUserService;
import com.xcz.member.customer.infrastructure.cache.UserCache;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import com.xcz.member.customer.infrastructure.mapper.MobiUserMapper;
import com.xcz.member.feign.dto.user.MobiUserFeign;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户信息服务实现类。
 * <p>
 * 资料更新成功后自动刷新 {@link UserCache}，应用层无需手动维护用户资料缓存。
 */
@Service
public class MobiUserServiceImpl extends ServiceImpl<MobiUserMapper, MobiUser> implements MobiUserService {

    @Resource
    private MobiUserMapper mobiUserMapper;

    /**
     * 更新用户资料后刷新 Redis 用户资料缓存。
     */
    @Override
    public boolean updateById(MobiUser entity) {
        boolean updated = super.updateById(entity);
        if (updated) {
            refreshUserInfoCache(entity.getUserId());
        }
        return updated;
    }

    /**
     * 保存或更新用户后刷新 Redis 用户资料缓存。
     */
    @Override
    public boolean saveOrUpdate(MobiUser entity) {
        boolean saved = super.saveOrUpdate(entity);
        if (saved && entity.getUserId() != null) {
            refreshUserInfoCache(entity.getUserId());
        }
        return saved;
    }

    /**
     * 管理后台分页查询小程序用户
     *
     * @param userId   用户 ID（可选）
     * @param nickname 昵称关键字（可选）
     * @param phone    手机号关键字（可选）
     * @param openid   OpenID 关键字（可选）
     * @param status   状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 用户分页结果
     */
    @Override
    public Page<MobiUser> pageForSys(Long userId, String nickname, String phone, String openid, Integer status,
                                     int pageNum, int pageSize) {
        LambdaQueryWrapper<MobiUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, MobiUser::getUserId, userId);
        wrapper.like(StringUtils.isNotEmpty(nickname), MobiUser::getNickname, nickname);
        wrapper.like(StringUtils.isNotEmpty(phone), MobiUser::getPhone, phone);
        wrapper.like(StringUtils.isNotEmpty(openid), MobiUser::getOpenid, openid);
        wrapper.eq(status != null, MobiUser::getStatus, status);
        wrapper.orderByDesc(MobiUser::getCreateTime);
        return mobiUserMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 管理后台更新小程序用户资料
     *
     * @param body 用户 ID、昵称、手机号、状态等可更新字段
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateForSys(MobiUserFeign body) {
        if (body == null || body.getUserId() == null) {
            throw new ServiceException("用户ID不能为空");
        }
        MobiUser existing = mobiUserMapper.selectById(body.getUserId());
        if (existing == null) {
            throw new ServiceException("用户不存在");
        }
        MobiUser patch = MobiUser.builder().userId(body.getUserId()).build();
        if (StringUtils.isNotEmpty(body.getNickname())) {
            patch.setNickname(body.getNickname());
        }
        if (body.getPhone() != null) {
            patch.setPhone(body.getPhone().isBlank() ? null : body.getPhone());
        }
        if (body.getStatus() != null) {
            if (body.getStatus() != 0 && body.getStatus() != 1) {
                throw new ServiceException("状态值无效");
            }
            patch.setStatus(body.getStatus());
        }
        patch.setUpdateTime(LocalDateTime.now());
        mobiUserMapper.updateById(patch);
        refreshUserInfoCache(body.getUserId());
    }

    /**
     * 从数据库加载最新资料并写入 Redis 用户资料缓存。
     */
    private void refreshUserInfoCache(Long userId) {
        if (userId == null) {
            return;
        }
        MobiUser user = mobiUserMapper.selectById(userId);
        if (user != null) {
            UserCache.saveUserInfo(user);
        }
    }

}
