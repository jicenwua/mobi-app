package com.xcz.member.user.service.admin;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.coupon.MobiCouponFeign;
import com.xcz.member.feign.service.MobiShopCouponFeignService;
import com.xcz.member.user.annotation.OperateLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 店铺折扣券模板管理后台写操作。
 */
@Service
@RequiredArgsConstructor
public class ShopCouponAdminService {

    private final MobiShopCouponFeignService mobiShopCouponFeignService;

    @OperateLog(module = "店铺折扣券", operation = "新增折扣券")
    public ResponseEntity<Boolean> add(MobiCouponFeign body) {
        return mobiShopCouponFeignService.add(body);
    }

    @OperateLog(module = "店铺折扣券", operation = "修改折扣券")
    public ResponseEntity<Boolean> edit(MobiCouponFeign body) {
        return mobiShopCouponFeignService.edit(body);
    }

    @OperateLog(module = "店铺折扣券", operation = "删除折扣券")
    public ResponseEntity<Void> remove(Long templateId) {
        return mobiShopCouponFeignService.remove(templateId);
    }

    @OperateLog(module = "店铺折扣券", operation = "手动停止")
    public ResponseEntity<Void> stop(Long templateId) {
        return mobiShopCouponFeignService.stop(templateId);
    }

    @OperateLog(module = "店铺折扣券", operation = "恢复发放")
    public ResponseEntity<Void> resume(Long templateId) {
        return mobiShopCouponFeignService.resume(templateId);
    }
}
