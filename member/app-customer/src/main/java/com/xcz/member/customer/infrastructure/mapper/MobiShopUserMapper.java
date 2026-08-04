package com.xcz.member.customer.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcz.member.customer.infrastructure.entity.MobiShopUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 店铺用户权限 Mapper
 */
@Mapper
public interface MobiShopUserMapper extends BaseMapper<MobiShopUser> {

    /**
     * 批量更新用户在多个店铺的角色。
     *
     * @param userId   用户 ID
     * @param roleType 目标角色
     * @param list     待更新记录
     * @return 影响行数
     */
    int update(@Param("userId") Long userId, @Param("roleType") Integer roleType, @Param("list") List<MobiShopUser> list);

    /**
     * 批量更新用户在多个店铺的最后进入时间。
     *
     * @param userId 用户 ID
     * @param list   待更新记录（shopId、lastEnterTime）
     * @return 影响行数
     */
    int batchUpdateLastEnterTime(@Param("userId") Long userId, @Param("list") List<MobiShopUser> list);
}
