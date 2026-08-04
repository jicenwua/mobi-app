package com.xcz.member.customer.controller.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.application.assemblers.ShopAssembler;
import com.xcz.member.customer.application.command.shop.AuditShopCommand;
import com.xcz.member.customer.application.command.shop.CreateShopCommand;
import com.xcz.member.customer.application.service.shop.ShopApplicationQueryService;
import com.xcz.member.customer.application.service.shop.ShopApplicationService;
import com.xcz.member.customer.application.service.shop.ShopStatisticsApplicationQueryService;
import com.xcz.member.feign.dto.shop.MobiShopFeign;
import com.xcz.member.feign.dto.shop.ShopAuditFeign;
import com.xcz.member.feign.dto.shop.ShopCreateFeign;
import com.xcz.member.feign.dto.shop.ShopStatisticsFeign;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 管理后台店铺（Feign 入口）
 */
@RestController
@RequestMapping("/shop/sys")
public class SysMobiShopController {

    @Resource
    private ShopApplicationService shopApplicationService;
    @Resource
    private ShopApplicationQueryService shopApplicationQueryService;
    @Resource
    private ShopStatisticsApplicationQueryService shopStatisticsApplicationQueryService;

    @GetMapping("/list")
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
        Page<MobiShopFeign> page = shopApplicationQueryService.pageForFeign(
                pageNum, pageSize, shopName, legalPerson, auditStatus, isEnabled, categoryId, minCreateTime, maxCreateTime);
        ResponseEntity<List<MobiShopFeign>> res = ResponseEntityUtils.ok(page.getRecords(), "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    @GetMapping("/audit/list")
    public ResponseEntity<List<MobiShopFeign>> auditList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String shopName,
            @RequestParam(required = false) String legalPerson,
            @RequestParam(required = false) String minCreateTime,
            @RequestParam(required = false) String maxCreateTime) {
        Page<MobiShopFeign> page = shopApplicationQueryService.auditListForFeign(
                pageNum, pageSize, shopName, legalPerson, minCreateTime, maxCreateTime);
        ResponseEntity<List<MobiShopFeign>> res = ResponseEntityUtils.ok(page.getRecords(), "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    @GetMapping("/info/{id}")
    public ResponseEntity<MobiShopFeign> get(@PathVariable("id") Long id) {
        return ResponseEntityUtils.ok(shopApplicationQueryService.getForFeign(id), "查询成功");
    }

    @PostMapping(value = "/add-full", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> addFull(
            @RequestPart("meta") ShopCreateFeign meta,
            @RequestPart("idCardFront") MultipartFile idCardFront,
            @RequestPart("idCardBack") MultipartFile idCardBack,
            @RequestPart("businessLicensePic") MultipartFile businessLicensePic,
            @RequestPart("shopExterior") MultipartFile shopExterior,
            @RequestPart("shopInterior") MultipartFile shopInterior) {
        MultipartFile[] carouselImages = new MultipartFile[]{shopExterior};
        Long id = shopApplicationService.addShop(new CreateShopCommand(
                ShopAssembler.toCreatePayload(meta),
                idCardFront,
                idCardBack,
                businessLicensePic,
                shopExterior,
                shopInterior,
                carouselImages
        ), meta.getManagerUserId());
        return ResponseEntityUtils.ok(id, "新增成功");
    }

    /**
     * 管理后台审核店铺（通过 / 驳回）。
     */
    @PutMapping("/audit")
    public ResponseEntity<Void> audit(@RequestBody ShopAuditFeign dto) {
        AuditShopCommand command = new AuditShopCommand(
                dto.getId(),
                dto.getAuditStatus(),
                dto.getAuditReason()
        );
        shopApplicationService.auditShop(command, SecurityUtils.getUserId());
        return ResponseEntityUtils.ok(null, "审核成功");
    }

    /**
     * 管理后台首页统计数据。
     */
    @GetMapping("/statistics")
    public ResponseEntity<ShopStatisticsFeign> statistics(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Long filterShopId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntityUtils.ok(
                ShopAssembler.toStatisticsFeign(
                        shopStatisticsApplicationQueryService.getForAdmin(shopId, filterShopId, startDate, endDate)),
                "查询成功");
    }
}
