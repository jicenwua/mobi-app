package com.xcz.member.customer.infrastructure.shop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.domain.shop.model.Shop;
import com.xcz.member.customer.domain.shop.repository.ShopRepository;
import com.xcz.member.customer.infrastructure.cache.ShopCache;
import com.xcz.member.customer.infrastructure.entity.MobiShop;
import com.xcz.member.customer.infrastructure.mapper.MobiShopMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 店铺聚合仓储实现。
 */
@Repository
public class ShopRepositoryImpl implements ShopRepository {

    @Resource
    private MobiShopMapper mobiShopMapper;


    @Override
    public Shop save(Shop shop) {
        MobiShop po = ShopConverter.toPo(shop);
        if (po.getId() == null) {
            mobiShopMapper.insert(po);
            if (po.getId() == null) {
                throw new ServiceException("保存店铺失败", 500);
            }
            shop.bindId(po.getId());
        } else {
            po.setUpdateTime(shop.getUpdateTime());
            if (mobiShopMapper.updateById(po) <= 0) {
                throw new ServiceException("更新店铺失败", 500);
            }
        }
        ShopCache.evictShop(shop.getId(), shop.getShopCode());
        return shop;
    }

    @Override
    public Optional<Shop> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ShopConverter.toDomain(mobiShopMapper.selectById(id)));
    }


    @Override
    public Optional<Shop> findByShopCode(String shopCode) {
        if (shopCode == null || shopCode.isBlank()) {
            return Optional.empty();
        }
        MobiShop po = mobiShopMapper.selectOne(
                new LambdaQueryWrapper<MobiShop>().eq(MobiShop::getShopCode, shopCode.trim()),
                false
        );
        return Optional.ofNullable(ShopConverter.toDomain(po));
    }


    @Override
    public boolean existsByShopCode(String shopCode) {
        if (shopCode == null || shopCode.isBlank()) {
            return false;
        }
        return mobiShopMapper.selectCount(
                new LambdaQueryWrapper<MobiShop>().eq(MobiShop::getShopCode, shopCode)
        ) > 0;
    }
}
