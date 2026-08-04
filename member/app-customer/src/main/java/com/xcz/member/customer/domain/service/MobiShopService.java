package com.xcz.member.customer.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.customer.api.dto.response.shop.ShopDetailRes;
import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import com.xcz.member.customer.infrastructure.entity.MobiShop;

import java.util.List;

/**
 * 店铺业务服务
 */
public interface MobiShopService extends IService<MobiShop> {


    /**
     * 按店铺 ID 批量查询简要信息（直接读库，不走缓存）。
     *
     * @param shopIds 店铺 ID 列表
     * @return 店铺简要读模型列表
     */
    List<ShopBriefDTO> listBriefByIds(List<Long> shopIds);

    /**
     * 获取店铺详情读模型（含商品列表与轮播图 URL），优先读 Redis 缓存。
     * <p>适用于会员进店页、扫码邀请预览等场景；不含当前用户角色与积分，
     * 需角色或积分时请使用应用层店铺查询接口。</p>
     *
     * @param shopId 店铺 ID
     * @return 店铺详情 VO
     */
    ShopDetailRes getShopDetail(Long shopId);

    /**
     * 获取店铺详情读模型（含商品列表与轮播图 URL），优先读 Redis 缓存。
     * <p>适用于会员进店页、扫码邀请预览等场景；不含当前用户角色与积分，
     * 需角色或积分时请使用应用层店铺查询接口。</p>
     *
     * @param  shopCode 店铺 ID
     * @return 店铺详情 VO
     */
    ShopDetailRes getShopDetail(String  shopCode);

}
