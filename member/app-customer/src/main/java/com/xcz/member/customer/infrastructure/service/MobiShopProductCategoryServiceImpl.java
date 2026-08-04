package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.member.customer.domain.service.MobiShopProductCategoryService;
import com.xcz.member.customer.infrastructure.cache.ProductCache;
import com.xcz.member.customer.infrastructure.entity.MobiShopProductCategory;
import com.xcz.member.customer.infrastructure.mapper.MobiShopProductCategoryMapper;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 店铺商品分类持久化服务实现。
 * <p>
 * 写操作成功后自动失效 {@link ProductCache} 中对应店铺的商品目录缓存。
 */
@Service
public class MobiShopProductCategoryServiceImpl
        extends ServiceImpl<MobiShopProductCategoryMapper, MobiShopProductCategory>
        implements MobiShopProductCategoryService {

    /**
     * 新增分类后失效店铺商品目录缓存。
     */
    @Override
    public boolean save(MobiShopProductCategory entity) {
        boolean saved = super.save(entity);
        if (saved) {
            ProductCache.evict(entity.getShopId());
        }
        return saved;
    }

    /**
     * 批量新增分类后失效涉及店铺的商品目录缓存。
     */
    @Override
    public boolean saveBatch(Collection<MobiShopProductCategory> entityList) {
        boolean saved = super.saveBatch(entityList);
        if (saved) {
            entityList.stream()
                    .map(MobiShopProductCategory::getShopId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(ProductCache::evict);
        }
        return saved;
    }

    /**
     * 批量更新分类后失效涉及店铺的商品目录缓存。
     */
    @Override
    public boolean updateBatchById(Collection<MobiShopProductCategory> entityList) {
        boolean updated = super.updateBatchById(entityList);
        if (updated) {
            entityList.stream()
                    .map(MobiShopProductCategory::getShopId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(ProductCache::evict);
        }
        return updated;
    }

    /**
     * 删除分类后失效店铺商品目录缓存。
     */
    @Override
    public boolean removeById(Serializable id) {
        MobiShopProductCategory existing = getById(id);
        boolean removed = super.removeById(id);
        if (removed && existing != null) {
            ProductCache.evict(existing.getShopId());
        }
        return removed;
    }

    /**
     * 查询店铺下全部商品分类，按 sortOrder、categoryId 升序。
     *
     * @param shopId 店铺 ID
     * @return 分类列表
     */
    @Override
    public List<MobiShopProductCategory> listByShopId(Long shopId) {
        return list(new LambdaQueryWrapper<MobiShopProductCategory>()
                .eq(MobiShopProductCategory::getShopId, shopId)
                .orderByAsc(MobiShopProductCategory::getSortOrder)
                .orderByAsc(MobiShopProductCategory::getCategoryId));
    }
}
