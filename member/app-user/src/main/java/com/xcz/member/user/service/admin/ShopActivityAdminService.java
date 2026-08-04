package com.xcz.member.user.service.admin;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.activity.MobiActivitySaveDTO;
import com.xcz.member.feign.service.MobiActivityFeignService;
import com.xcz.member.user.annotation.OperateLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 店铺活动管理后台写操作（独立 Service 以便 @OperateLog 切面生效）。
 */
@Service
@RequiredArgsConstructor
public class ShopActivityAdminService {

    private final MobiActivityFeignService mobiActivityFeignService;

    @OperateLog(module = "店铺活动", operation = "新增活动")
    public ResponseEntity<Long> create(MobiActivitySaveDTO body) {
        return mobiActivityFeignService.save(body);
    }

    @OperateLog(module = "店铺活动", operation = "修改活动")
    public ResponseEntity<Long> update(MobiActivitySaveDTO body) {
        return mobiActivityFeignService.save(body);
    }

    @OperateLog(module = "店铺活动", operation = "删除活动")
    public ResponseEntity<Void> remove(Long id) {
        return mobiActivityFeignService.remove(id);
    }

    @OperateLog(module = "店铺活动", operation = "手动停止活动")
    public ResponseEntity<Void> stop(Long id) {
        return mobiActivityFeignService.stop(id);
    }

    @OperateLog(module = "店铺活动", operation = "启用活动")
    public ResponseEntity<Void> enable(Long id) {
        return mobiActivityFeignService.enable(id);
    }
}
