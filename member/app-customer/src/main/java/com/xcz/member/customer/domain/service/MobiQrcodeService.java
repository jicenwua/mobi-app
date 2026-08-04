package com.xcz.member.customer.domain.service;

import com.xcz.member.customer.api.dto.response.shop.ShopDetailRes;
import com.xcz.member.customer.domain.vo.*;

/**
 * 小程序二维码服务：店铺邀请码、会员付款码。
 */
public interface MobiQrcodeService {

    /**
     * 生成店铺邀请小程序码（scene 携带短时 invite token，默认 2 分钟有效）
     *
     * @param shopId 店铺 ID
     * @return 小程序码
     */
    ShopQrcodeVO generateShopQrcode(Long shopId);

    /**
     * 解析邀请 token 并返回店铺预览信息
     *
     * @param token 邀请 token 或 scene
     * @return 店铺详情
     */
    ShopDetailRes resolveShopInvite(String token);

    /**
     * 会员生成统一付款码（与用户绑定，扫码时由店员所在店铺扣款）
     *
     * @return 付款码信息
     */
    PayQrcodeVO generatePay();

    /**
     * 店员校验付款码并返回会员摘要
     *
     * @param shopId 店铺 ID
     * @param token  付款码 token
     * @return 会员信息
     */
    PayQrcodeVerifyVO verifyPay(Long shopId, String token);

    /**
     * 会员生成订单核销码（仅待使用订单）
     */
    OrderQrcodeVO generateOrder(Long logId, Long shopId);

    /**
     * 用户生成店员邀请码（店长扫码添加为店员）
     *
     * @return 邀请码信息
     */
    StaffInviteQrcodeVO generateStaffInvite();
}
