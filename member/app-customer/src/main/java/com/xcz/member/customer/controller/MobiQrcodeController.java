package com.xcz.member.customer.controller;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.api.dto.response.shop.ShopDetailRes;
import com.xcz.member.customer.domain.service.MobiQrcodeService;
import com.xcz.member.customer.domain.vo.*;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序端二维码接口：店铺邀请码、会员付款码、店员校验付款码。
 */
@RestController
@RequestMapping("/qrcode")
public class MobiQrcodeController {

    @Resource
    private MobiQrcodeService mobiQrcodeService;

    /**
     * 生成店铺邀请小程序码（短时有效，扫码加入店铺）。
     *
     * @param shopId 店铺 ID
     * @return 小程序码与邀请 token
     */
    @GetMapping("/shop/{shopId}")
    @PreAuthorize("@ss.hasPermi('wx:user:query')")
    public ResponseEntity<ShopQrcodeVO> shopQrcode(@PathVariable Long shopId) {
        return ResponseEntityUtils.ok(mobiQrcodeService.generateShopQrcode(shopId), "生成成功");
    }

    /**
     * 解析店铺邀请 token（加入前预览）。
     *
     * @param token 邀请 token 或 scene
     * @return 店铺详情
     */
    @GetMapping("/shop/invite")
    @PreAuthorize("@ss.hasPermi('wx:user:query')")
    public ResponseEntity<ShopDetailRes> resolveShopInvite(@RequestParam String token) {
        return ResponseEntityUtils.ok(mobiQrcodeService.resolveShopInvite(token), "查询成功");
    }

    /**
     * 会员生成统一付款码（与用户绑定，不区分店铺）。
     *
     * @return 付款码 token 与二维码内容
     */
    @GetMapping("/pay/generate")
    public ResponseEntity<PayQrcodeVO> generatePay() {
        return ResponseEntityUtils.ok(mobiQrcodeService.generatePay(), "生成成功");
    }

    /**
     * 店员校验付款码并返回会员摘要（扣款前预览）。3
     *
     * @param shopId 店铺 ID
     * @param token  付款码 token
     * @return 会员摘要（含剩余积分）
     */
    @GetMapping("/pay/verify")
    public ResponseEntity<PayQrcodeVerifyVO> verifyPay(@RequestParam Long shopId, @RequestParam String token) {
        return ResponseEntityUtils.ok(mobiQrcodeService.verifyPay(shopId, token), "校验成功");
    }

    /**
     * 会员生成订单核销码（仅待使用订单）。
     */
    @GetMapping("/order/generate")
    public ResponseEntity<OrderQrcodeVO> generateOrder(@RequestParam Long logId, @RequestParam Long shopId) {
        return ResponseEntityUtils.ok(mobiQrcodeService.generateOrder(logId, shopId), "生成成功");
    }

    /**
     * 用户生成店员邀请码（店长扫码添加为店员）。
     */
    @GetMapping("/staff/invite/generate")
    public ResponseEntity<StaffInviteQrcodeVO> generateStaffInvite() {
        return ResponseEntityUtils.ok(mobiQrcodeService.generateStaffInvite(), "生成成功");
    }
}

