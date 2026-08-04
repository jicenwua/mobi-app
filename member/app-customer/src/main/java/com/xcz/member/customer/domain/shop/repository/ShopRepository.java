package com.xcz.member.customer.domain.shop.repository;

import com.xcz.member.customer.domain.shop.model.Shop;

import java.util.Optional;

/**
 * 店铺聚合仓储
 */
public interface ShopRepository {

    /**
     * 保存或更新店铺聚合根。
     *
     * @param shop 店铺聚合根
     * @return 保存后的店铺聚合根（含持久化 ID）
     */
    Shop save(Shop shop);

    /**
     * 按主键查询店铺聚合根。
     *
     * @param id 店铺 ID
     * @return 店铺聚合根
     */
    Optional<Shop> findById(Long id);

    /**
     * 按店铺编码查询店铺聚合根。
     *
     * @param shopCode 店铺编码
     * @return 店铺聚合根
     */
    Optional<Shop> findByShopCode(String shopCode);

    /**
     * 判断店铺编码是否已存在。
     *
     * @param shopCode 店铺编码
     * @return 是否已存在
     */
    boolean existsByShopCode(String shopCode);
}
