package com.xcz.member.user.service.admin;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.role.MobiRoleFeign;
import com.xcz.member.feign.service.MobiRoleFeignService;
import com.xcz.member.user.annotation.OperateLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 小程序权限（mobi_role）管理后台写操作。
 */
@Service
@RequiredArgsConstructor
public class MobiRoleAdminService {

    private final MobiRoleFeignService mobiRoleFeignService;

    @OperateLog(module = "小程序权限", operation = "新增权限项")
    public ResponseEntity<Void> add(MobiRoleFeign body) {
        return mobiRoleFeignService.add(body);
    }

    @OperateLog(module = "小程序权限", operation = "修改权限项")
    public ResponseEntity<Void> edit(MobiRoleFeign body) {
        return mobiRoleFeignService.edit(body);
    }

    @OperateLog(module = "小程序权限", operation = "删除权限项")
    public ResponseEntity<Void> remove(List<Long> ids) {
        return mobiRoleFeignService.remove(ids);
    }

    @OperateLog(module = "小程序权限", operation = "刷新权限缓存")
    public ResponseEntity<Void> reloadCache() {
        return mobiRoleFeignService.reloadCache();
    }
}
