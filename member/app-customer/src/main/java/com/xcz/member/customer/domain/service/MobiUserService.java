package com.xcz.member.customer.domain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import com.xcz.member.feign.dto.user.MobiUserFeign;

/**
 * 小程序用户服务
 */
public interface MobiUserService extends IService<MobiUser> {

    /**
     * 管理后台分页查询用户
     *
     * @param userId   用户 ID（可选）
     * @param nickname 昵称关键字（可选）
     * @param phone    手机号关键字（可选）
     * @param openid   OpenID 关键字（可选）
     * @param status   状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 用户分页结果
     */
    Page<MobiUser> pageForSys(Long userId, String nickname, String phone, String openid, Integer status,
                              int pageNum, int pageSize);

    /**
     * 管理后台更新用户
     *
     * @param body 用户 ID、昵称、手机号、状态等可更新字段
     */
    void updateForSys(MobiUserFeign body);
}
