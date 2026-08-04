package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.member.customer.domain.service.MobiActivityService;
import com.xcz.member.customer.infrastructure.cache.ActivityCache;
import com.xcz.member.customer.infrastructure.entity.MobiActivity;
import com.xcz.member.customer.infrastructure.mapper.MobiActivityMapper;
import org.springframework.stereotype.Service;

import java.io.Serializable;

/**
 * 活动持久化实现。
 * <p>
 * 写操作成功后自动失效 {@link ActivityCache}，应用层无需手动清理。
 */
@Service
public class MobiActivityServiceImpl extends ServiceImpl<MobiActivityMapper, MobiActivity>
        implements MobiActivityService {

    /**
     * 新增活动后失效店铺活动缓存。
     */
    @Override
    public boolean save(MobiActivity entity) {
        boolean saved = super.save(entity);
        if (saved) {
            evictActivityCacheAfterMutation(entity);
        }
        return saved;
    }

    /**
     * 更新活动后失效店铺活动缓存与活动索引。
     */
    @Override
    public boolean updateById(MobiActivity entity) {
        boolean updated = super.updateById(entity);
        if (updated) {
            evictActivityCacheAfterMutation(entity);
        }
        return updated;
    }

    /**
     * 删除活动后失效店铺活动缓存与活动索引。
     */
    @Override
    public boolean removeById(Serializable id) {
        MobiActivity existing = getById(id);
        boolean removed = super.removeById(id);
        if (removed && existing != null) {
            evictActivityCacheAfterMutation(existing);
        }
        return removed;
    }

    /**
     * 失效店铺活动列表与活动详情缓存。
     */
    private static void evictActivityCacheAfterMutation(MobiActivity activity) {
        if (activity == null) {
            return;
        }
        if (activity.getShopId() != null) {
            ActivityCache.evictShop(activity.getShopId());
        }
        if (activity.getActivityId() != null) {
            ActivityCache.evictRef(activity.getActivityId());
        }
    }
}
