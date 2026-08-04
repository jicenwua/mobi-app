package com.xcz.member.user.controller;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.shop.MobiShopFeign;
import com.xcz.member.feign.dto.shop.ShopAuditFeign;
import com.xcz.member.feign.dto.shop.ShopCreateFeign;
import com.xcz.member.feign.dto.shop.ShopStatisticsFeign;
import com.xcz.member.feign.service.MobiShopFeignService;
import com.xcz.member.user.annotation.OperateLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 后台店铺管理（代理层）：权限校验在本服务，业务由 app-miniApp 通过 Feign 完成。
 */
@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
public class SysShopController {

    private final MobiShopFeignService MobiShopFeignService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:shop:list')")
    public ResponseEntity<List<MobiShopFeign>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String shopName,
            @RequestParam(required = false) String legalPerson,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Integer isEnabled,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String minCreateTime,
            @RequestParam(required = false) String maxCreateTime) {
        return MobiShopFeignService.list(pageNum, pageSize, shopName, legalPerson, auditStatus, isEnabled, categoryId, minCreateTime, maxCreateTime);
    }

    @GetMapping("/audit/list")
    @PreAuthorize("@ss.hasPermi('system:shop:audit:list')")
    public ResponseEntity<List<MobiShopFeign>> auditList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String shopName,
            @RequestParam(required = false) String legalPerson,
            @RequestParam(required = false) String minCreateTime,
            @RequestParam(required = false) String maxCreateTime) {
        return MobiShopFeignService.auditList(pageNum, pageSize, shopName, legalPerson, minCreateTime, maxCreateTime);
    }

    @GetMapping("/info/{id}")
    @PreAuthorize("@ss.hasPermi('system:shop:query')")
    public ResponseEntity<MobiShopFeign> get(@PathVariable Long id) {
        return MobiShopFeignService.get(id);
    }

    @PostMapping(value = "/add-full", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ss.hasPermi('system:shop:add')")
    @OperateLog(module = "店铺管理", operation = "新增店铺", saveParams = false)
    public ResponseEntity<Long> addFull(
            @RequestPart("meta") ShopCreateFeign meta,
            @RequestPart("idCardFront") MultipartFile idCardFront,
            @RequestPart("idCardBack") MultipartFile idCardBack,
            @RequestPart("businessLicensePic") MultipartFile businessLicensePic,
            @RequestPart("shopExterior") MultipartFile shopExterior,
            @RequestPart("shopInterior") MultipartFile shopInterior) {
        return MobiShopFeignService.addFull(meta, idCardFront, idCardBack, businessLicensePic, shopExterior, shopInterior);
    }

    @PutMapping("/audit")
    @PreAuthorize("@ss.hasPermi('system:shop:audit')")
    @OperateLog(module = "店铺管理", operation = "审核店铺")
    public ResponseEntity<Void> audit(@RequestBody ShopAuditFeign dto) {
        return MobiShopFeignService.audit(dto);
    }

    @GetMapping("/statistics")
    public ResponseEntity<ShopStatisticsFeign> statistics(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Long filterShopId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return MobiShopFeignService.statistics(shopId, filterShopId, startDate, endDate);
    }
}
