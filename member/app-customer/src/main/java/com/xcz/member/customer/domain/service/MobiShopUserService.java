package com.xcz.member.customer.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.customer.infrastructure.entity.MobiShopUser;

import java.util.List;

/**
 * 店铺用户关系服务
 */
public interface MobiShopUserService extends IService<MobiShopUser> {

    /**
     * 查询用户关联的店铺列表
     *
     * @param userId 用户 ID
     * @return 店铺用户关系列表
     */
    List<MobiShopUser> listByUserId(Long userId);

    /**
     * 查询用户在店铺中的关系
     *
     * @param shopId 店铺 ID
     * @param userId 用户 ID
     * @return 店铺用户关系；不存在时返回 null
     */
    MobiShopUser getByShopIdAndUserId(Long shopId, Long userId);

    /**
     * 添加店铺用户关系
     *
     * @param shopId 店铺 ID
     * @param userId 用户 ID
     * @param role   角色（1-店长, 2-店员, 3-顾客）
     * @return 是否添加成功
     */
    boolean addShopUser(Long shopId, Long userId, Integer role);

    /**
     * 设置用户为店长
     *
     * @param shopId 店铺 ID
     * @param userId 用户 ID
     * @return 是否设置成功
     */
    boolean setShopManager(Long shopId, Long userId);

    /**
     * 用户以顾客身份加入店铺（同一 userId + shopId 仅一条记录）：
     * 无关系时新建顾客；已有关系时仅更新进入时间。
     *
     * @param shopId 店铺 ID
     * @param userId 用户 ID
     * @return 是否处理成功
     */
    boolean enterShopAsCustomer(Long shopId, Long userId);

    /**
     * 将用户设为店铺店员（无关系时新建；顾客升级为店员；已是店长/店员则失败）
     *
     * @param shopId 店铺 ID
     * @param userId 用户 ID
     * @return 是否成功
     */
    boolean assignShopClerk(Long shopId, Long userId);

    /**
     * 查询店铺全部店员（含历史编码 4）。
     *
     * @param shopId 店铺 ID
     * @return 店员关系列表
     */
    List<MobiShopUser> listClerksByShopId(Long shopId);

    /**
     * 移除店员身份（降为顾客，用于离职等场景）。
     *
     * @param shopId 店铺 ID
     * @param userId 用户 ID
     * @return 是否成功
     */
    boolean removeShopClerk(Long shopId, Long userId);

    /**
     * 记录用户进入店铺时间（写入 Redis，供定时任务同步至数据库）。
     *
     * @param shopId 店铺 ID
     * @param userId 用户 ID
     */
    void recordShopEnter(Long shopId, Long userId);
}
