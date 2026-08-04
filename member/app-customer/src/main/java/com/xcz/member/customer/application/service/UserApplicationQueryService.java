package com.xcz.member.customer.application.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.member.customer.application.assemblers.UserAssembler;
import com.xcz.member.customer.domain.service.MobiUserService;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import com.xcz.member.feign.dto.user.MobiUserFeign;
import com.xcz.commons.oss.service.UploadService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 管理后台小程序用户读侧应用服务（Feign 入口编排）。
 */
@Service
public class UserApplicationQueryService {

    @Resource
    private MobiUserService mobiUserService;
    @Resource
    private UploadService uploadService;

    public Page<MobiUserFeign> pageForFeign(Integer pageNum, Integer pageSize, Long userId,
                                              String nickname, String phone, String openid, Integer status) {
        Page<MobiUser> page = mobiUserService.pageForSys(
                userId, nickname, phone, openid, status, pageNum, pageSize);
        Page<MobiUserFeign> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(user -> UserAssembler.toFeign(user, uploadService::getUrl))
                .toList());
        return result;
    }

    public MobiUserFeign getForFeign(Long userId) {
        MobiUser user = mobiUserService.getById(userId);
        return UserAssembler.toFeign(user, uploadService::getUrl);
    }
}
