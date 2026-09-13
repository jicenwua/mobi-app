package com.xcz.member.customer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.commons.security.annotation.Release;
import com.xcz.member.customer.api.dto.request.shop.ShopCreatePayloadReq;
import com.xcz.member.customer.api.dto.request.shop.ShopReq;
import com.xcz.member.customer.api.dto.response.shop.ShopDetailRes;
import com.xcz.member.customer.api.dto.response.shop.ShopStatisticsRes;
import com.xcz.member.customer.api.dto.response.shop.StaffAddRes;
import com.xcz.member.customer.api.dto.response.shop.StaffListRes;
import com.xcz.member.customer.application.assemblers.ShopAssembler;
import com.xcz.member.customer.application.command.shop.CreateShopCommand;
import com.xcz.member.customer.application.service.shop.ShopApplicationQueryService;
import com.xcz.member.customer.application.service.shop.ShopApplicationService;
import com.xcz.member.customer.application.service.shop.ShopStatisticsApplicationQueryService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 小程序端店铺接口。
 */
@RestController
@RequestMapping("/shop")
public class MobiShopController {

    @Resource
    private ShopApplicationService shopApplicationService;
    @Resource
    private ShopApplicationQueryService shopApplicationQueryService;
    @Resource
    private ShopStatisticsApplicationQueryService shopStatisticsApplicationQueryService;

    /**
     * 分页查询当前用户关联的店铺列表，按最后进入时间倒序，并返回各店剩余积分。
     *
     * @param shopReq 分页参数
     * @return 店铺列表
     */
    @GetMapping
    public ResponseEntity<List<ShopDetailRes>> getUserList(ShopReq shopReq) {
        Page<ShopDetailRes> shopList = shopApplicationQueryService.getShopList(ShopAssembler.toQueryShopCommand(shopReq));
        ResponseEntity<List<ShopDetailRes>> res = ResponseEntityUtils.ok();
        res.setTotal(shopList.getTotal());
        res.setMsg("查询成功");
        res.setData(shopList.getRecords());
        return res;
    }

    /**
     * 提交开店申请（含证照与门店图片上传）
     *
     * @param meta               店铺元数据
     * @param idCardFront        身份证正面
     * @param idCardBack         身份证反面
     * @param businessLicensePic 营业执照
     * @param shopExterior       门店外观
     * @param shopInterior       门店内景
     * @param carouselImages     轮播图（可选，1–3 张）
     * @return 新店铺 ID
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ss.hasPermi('wx:shop:add')")
    public ResponseEntity<Long> addFull(
            @RequestPart("meta") ShopCreatePayloadReq meta,
            @RequestPart("idCardFront") MultipartFile idCardFront,
            @RequestPart("idCardBack") MultipartFile idCardBack,
            @RequestPart("businessLicensePic") MultipartFile businessLicensePic,
            @RequestPart("shopExterior") MultipartFile shopExterior,
            @RequestPart("shopInterior") MultipartFile shopInterior,
            @RequestPart(value = "carouselImages", required = false) MultipartFile[] carouselImages) {
        Long id = shopApplicationService.addShop(new CreateShopCommand(meta, idCardFront, idCardBack, businessLicensePic, shopExterior, shopInterior, carouselImages));
        ResponseEntity<Long> res = new ResponseEntity<>();
        res.setCode(200);
        res.setMsg("新增成功");
        res.setData(id);
        return res;
    }
    /**
     * 用户加入店铺（以顾客身份）。
     */
    @PostMapping("/enter")
    @PreAuthorize("@ss.hasPermi('wx:user:enter')")
    public ResponseEntity<Void> enterShop(@RequestParam Long shopId) {
        shopApplicationService.enterShop(shopId);
        return ResponseEntityUtils.ok(null, "进入成功");
    }

    /**
     * 店长扫码添加店员。
     */
    @PostMapping("/staff")
    @PreAuthorize("@ss.hasPermi('wx:shop:list')")
    public ResponseEntity<StaffAddRes> addStaff(@RequestParam Long shopId, @RequestParam String token) {
        return ResponseEntityUtils.ok(shopApplicationService.addClerkByInviteToken(shopId, token), "添加成功");
    }

    /**
     * 店长查看本店店员列表。
     */
    @GetMapping("/staff/list")
    @PreAuthorize("@ss.hasPermi('wx:shop:list')")
    public ResponseEntity<List<StaffListRes>> listStaff(@RequestParam Long shopId) {
        return ResponseEntityUtils.ok(shopApplicationQueryService.listShopStaff(shopId), "查询成功");
    }

    /**
     * 店长移除店员（降为顾客）。
     */
    @DeleteMapping("/staff")
    @PreAuthorize("@ss.hasPermi('wx:shop:list')")
    public ResponseEntity<Void> removeStaff(@RequestParam Long shopId, @RequestParam Long userId) {
        shopApplicationService.removeClerk(shopId, userId);
        return ResponseEntityUtils.ok(null, "已移除店员");
    }

    /**
     * 按店铺代码预览店铺（加入前），返回含商品列表的详情读模型。
     *
     * @param shopCode 店铺唯一编码
     * @return 店铺详情（不含当前用户角色与积分）
     */
    @GetMapping("/enter/{shopCode}")
    @PreAuthorize("@ss.hasPermi('wx:user:query')")
    public ResponseEntity<ShopDetailRes> previewByShopCode(@PathVariable String shopCode) {
        ShopDetailRes detailByShopCode = shopApplicationQueryService.getDetailByShopCode(shopCode);
        return ResponseEntityUtils.ok(detailByShopCode, "查询成功");
    }

    /**
     * 未登录浏览店铺详情（按店铺 ID），返回店铺基础信息与商品列表，不含用户积分。
     *
     * @param shopId 店铺 ID
     * @return 店铺详情
     */
    @Release
    @GetMapping("/guest")
    public ResponseEntity<ShopDetailRes> previewByShopId(@RequestParam Long shopId) {
        return ResponseEntityUtils.ok(shopApplicationQueryService.getDetailByShopId(shopId), "查询成功");
    }

    /**
     * 内部缓存用户最后进入的店铺，以此来刷新给用户展示的顺序
     * @param shopId    店铺id
     * @return          是否成功
     */
    @GetMapping("/enter/time")
    public ResponseEntity<Void> enterShopTime(@RequestParam Long shopId){
        shopApplicationService.enterShopTime(shopId);
        return ResponseEntityUtils.ok(null,"更新成功");
    }

    /**
     * 店长/店员查看店铺统计数据。
     *
     * @param shopId       当前管理店铺 ID
     * @param filterShopId 可选，总店可传分店 ID 或本店 ID 筛选；不传时总店汇总全部门店
     */
    @GetMapping("/statistics")
    @PreAuthorize("@ss.hasPermi('wx:shop:list')")
    public ResponseEntity<ShopStatisticsRes> statistics(
            @RequestParam Long shopId,
            @RequestParam(required = false) Long filterShopId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntityUtils.ok(
                shopStatisticsApplicationQueryService.getForStaff(shopId, filterShopId, startDate, endDate),
                "查询成功");
    }

}
