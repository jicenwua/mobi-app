package com.xcz.member.customer.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.customer.infrastructure.entity.MobiShopProductCategory;

import java.util.List;

/**
 * 店铺商品分类服务。
 */
public interface MobiShopProductCategoryService extends IService<MobiShopProductCategory> {

    /**
     * 按店铺 ID 查询全部分类（按 sortOrder 升序）。
     *
     * @param shopId 店铺 ID
     * @return 分类列表
     */
    List<MobiShopProductCategory> listByShopId(Long shopId);
}
