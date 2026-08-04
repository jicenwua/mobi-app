package com.xcz.member.user.service.admin;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.coupon.MobiUserCouponGrantFeign;
import com.xcz.member.feign.service.MobiUserCouponFeignService;
import com.xcz.member.user.annotation.OperateLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户持券管理后台写操作。
 */
@Service
@RequiredArgsConstructor
public class UserCouponAdminService {

    private final MobiUserCouponFeignService mobiUserCouponFeignService;

    @OperateLog(module = "用户折扣券", operation = "发放折扣券", saveResult = true)
    public ResponseEntity<Long> grant(MobiUserCouponGrantFeign body) {
        return mobiUserCouponFeignService.grant(body);
    }

    @OperateLog(module = "用户折扣券", operation = "删除折扣券")
    public ResponseEntity<Void> remove(Long userCouponId) {
        return mobiUserCouponFeignService.remove(userCouponId);
    }
}
