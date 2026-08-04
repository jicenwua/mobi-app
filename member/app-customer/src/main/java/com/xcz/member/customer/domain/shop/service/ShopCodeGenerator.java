package com.xcz.member.customer.domain.shop.service;

import com.xcz.member.customer.domain.shop.repository.ShopRepository;
import com.xcz.member.customer.utils.ShopCodeUtils;

/**
 * 店铺编码生成端口
 */
public class ShopCodeGenerator {

    private ShopCodeGenerator() {
    }

    /**
     * 生成全局唯一的店铺编码。
     *
     * @param shopRepository 用于校验编码唯一性的仓储
     * @return 新店铺编码
     */
    public static String nextCode(ShopRepository shopRepository) {
        return ShopCodeUtils.generateUniqueCode(shopRepository::existsByShopCode);
    }
}
