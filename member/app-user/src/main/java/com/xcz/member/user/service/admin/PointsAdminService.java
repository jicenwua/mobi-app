package com.xcz.member.user.service.admin;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.points.PointsAdjustFeign;
import com.xcz.member.feign.service.MobiPointsFeignService;
import com.xcz.member.user.annotation.OperateLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户积分管理后台写操作。
 */
@Service
@RequiredArgsConstructor
public class PointsAdminService {

    private final MobiPointsFeignService mobiPointsFeignService;

    @OperateLog(module = "用户积分", operation = "调整积分", saveResult = true)
    public ResponseEntity<Void> adjustAccount(PointsAdjustFeign body) {
        return mobiPointsFeignService.adjustAccount(body);
    }
}
