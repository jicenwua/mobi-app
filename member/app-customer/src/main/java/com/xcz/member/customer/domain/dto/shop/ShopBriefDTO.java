package com.xcz.member.customer.domain.dto.shop;

import com.xcz.member.customer.infrastructure.cache.dto.ShopBriefCacheDTO;
import com.xcz.member.customer.infrastructure.entity.MobiShop;

/**
 * 店铺简要读模型，用于列表与详情查询场景。
 */
public record ShopBriefDTO(
        Long id,
        Long parentId,
        String shopName,
        String shopCode,
        Integer ratio,
        String picture,
        String phone,
        Integer categoryId,
        String province,
        String city,
        String district,
        String address
) {

    public static ShopBriefDTO toDto(ShopBriefCacheDTO cacheDto) {
        if (cacheDto == null) {
            return null;
        }
        return new ShopBriefDTO(
                cacheDto.getId(),
                cacheDto.getParentId(),
                cacheDto.getShopName(),
                cacheDto.getShopCode(),
                cacheDto.getRatio(),
                cacheDto.getPicture(),
                cacheDto.getPhone(),
                cacheDto.getCategoryId(),
                cacheDto.getProvince(),
                cacheDto.getCity(),
                cacheDto.getDistrict(),
                cacheDto.getAddress()
        );
    }

    public static ShopBriefDTO of(MobiShop mobiShop) {
        return new ShopBriefDTO(
                mobiShop.getId(),
                mobiShop.getParentId(),
                mobiShop.getShopName(),
                mobiShop.getShopCode(),
                mobiShop.getRatio(),
                mobiShop.getPicture(),
                mobiShop.getPhone(),
                mobiShop.getCategoryId(),
                mobiShop.getProvince(),
                mobiShop.getCity(),
                mobiShop.getDistrict(),
                mobiShop.getAddress()
        );
    }

    /**
     * 解析积分挂载的总店 ID：分店铺共用总店积分账户。
     *
     * @return 总店 ID；当前店铺即为总店时返回自身 ID
     */
    public Long headShopId() {
        if (parentId != null && parentId != 0L) {
            return parentId;
        }
        return id;
    }
}
