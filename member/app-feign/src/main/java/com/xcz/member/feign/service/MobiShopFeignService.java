package com.xcz.member.feign.service;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.config.MobiShopFeignConfig;
import com.xcz.member.feign.dto.shop.MobiShopFeign;
import com.xcz.member.feign.dto.shop.ShopAuditFeign;
import com.xcz.member.feign.dto.shop.ShopCreateFeign;
import com.xcz.member.feign.dto.shop.ShopStatisticsFeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 店铺服务 Feign 客户端 - 调用 app-miniApp（customer）店铺接口
 */
@FeignClient(
        value = "app-miniApp",
        path = "/shop/sys",
        contextId = "MobiShopFeignService",
        configuration = MobiShopFeignConfig.class
)
public interface MobiShopFeignService {

    @GetMapping("/list")
    ResponseEntity<List<MobiShopFeign>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String shopName,
            @RequestParam(required = false) String legalPerson,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Integer isEnabled,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String minCreateTime,
            @RequestParam(required = false) String maxCreateTime);

    @GetMapping("/audit/list")
    ResponseEntity<List<MobiShopFeign>> auditList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String shopName,
            @RequestParam(required = false) String legalPerson,
            @RequestParam(required = false) String minCreateTime,
            @RequestParam(required = false) String maxCreateTime);

    @GetMapping("/info/{id}")
    ResponseEntity<MobiShopFeign> get(@PathVariable("id") Long id);

    @PostMapping(value = "/add-full", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Long> addFull(
            @RequestPart("meta") ShopCreateFeign meta,
            @RequestPart("idCardFront") MultipartFile idCardFront,
            @RequestPart("idCardBack") MultipartFile idCardBack,
            @RequestPart("businessLicensePic") MultipartFile businessLicensePic,
            @RequestPart("shopExterior") MultipartFile shopExterior,
            @RequestPart("shopInterior") MultipartFile shopInterior);

    @PutMapping("/audit")
    ResponseEntity<Void> audit(@RequestBody ShopAuditFeign dto);

    @GetMapping("/statistics")
    ResponseEntity<ShopStatisticsFeign> statistics(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Long filterShopId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate);
}
