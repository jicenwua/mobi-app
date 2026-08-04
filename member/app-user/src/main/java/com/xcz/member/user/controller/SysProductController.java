package com.xcz.member.user.controller;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.product.MobiProductFeign;
import com.xcz.member.feign.service.MobiProductFeignService;
import com.xcz.member.user.annotation.OperateLog;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台店铺商品管理（代理 app-customer）
 */
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class SysProductController {

    private final MobiProductFeignService mobiProductFeignService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:product:list')")
    public ResponseEntity<List<MobiProductFeign>> list(MobiProductFeign query) {
        return mobiProductFeignService.list(query);
    }

    @PutMapping("/status")
    @PreAuthorize("@ss.hasPermi('system:product:list')")
    @OperateLog(module = "商品管理", operation = "修改商品状态")
    public ResponseEntity<Void> updateStatus(@RequestBody MobiProductFeign body) {
        return mobiProductFeignService.updateStatus(body);
    }
}
