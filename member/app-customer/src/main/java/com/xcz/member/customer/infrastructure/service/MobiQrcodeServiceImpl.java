package com.xcz.member.customer.infrastructure.service;

import cn.binarywang.wx.miniapp.api.WxMaService;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.api.dto.response.shop.ShopDetailRes;
import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import com.xcz.member.customer.domain.enums.PointsLogStatus;
import com.xcz.member.customer.domain.service.*;
import com.xcz.member.customer.domain.vo.*;
import com.xcz.member.customer.infrastructure.cache.ShopCache;
import com.xcz.member.customer.infrastructure.entity.MobiPointsAccount;
import com.xcz.member.customer.infrastructure.entity.MobiPointsLog;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import com.xcz.member.customer.infrastructure.mapper.MobiShopMapper;
import com.xcz.member.customer.utils.*;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Set;

/**
 * 小程序二维码服务实现
 */
@Slf4j
@Service
public class MobiQrcodeServiceImpl implements MobiQrcodeService {

    private static final String PAY_QR_PREFIX = "MOBI:PAY:";
    private static final String ORDER_QR_PREFIX = "MOBI:ORDER:";
    private static final String SHOP_INVITE_QR_PREFIX = "MOBI:SHOP:INV:";
    private static final String STAFF_INVITE_QR_PREFIX = "MOBI:STAFF:INV:";
    /** 扫码落地页：使用 app.json 首页，避免 check_path 校验失败 */
    private static final String SHOP_INVITE_PAGE = "pages/login/login";
    private static final Set<String> ALLOWED_ENV_VERSIONS = Set.of("release", "trial", "develop");

    @Resource
    private WxMaService wxMaService;
    @Resource
    private MobiShopMapper mobiShopMapper;
    @Resource
    private MobiShopUserService mobiShopUserService;
    @Resource
    private MobiPointsAccountService mobiPointsAccountService;
    @Resource
    private MobiUserService mobiUserService;
    /** 店铺读服务，用于解析邀请码后加载店铺详情 */
    @Resource
    private MobiShopService mobiShopService;
    @Resource
    private MobiPointsLogService mobiPointsLogService;

    @Value("${wx.env-version:develop}")
    private String envVersion;

    @Value("${wx.check-path:false}")
    private boolean checkPath;

    /**
     * 生成店铺邀请小程序码（当前用户须已加入该店铺）。
     *
     * @param shopId 店铺 ID
     * @return 含 base64 图片与邀请 token 的 VO
     */
    @Override
    public ShopQrcodeVO generateShopQrcode(Long shopId) {
        if (shopId == null) {
            throw new ServiceException("店铺ID不能为空");
        }
        ShopAccessUtils.assertShopMember(mobiShopUserService, shopId);
        ShopBriefDTO shop = ShopCache.getById(shopId, mobiShopMapper);
        if (shop == null) {
            throw new ServiceException("店铺不存在");
        }
        String shopCode = shop.shopCode();
        if (shopCode == null || shopCode.isBlank()) {
            throw new ServiceException("店铺代码无效");
        }
        //生成二维码token
        String scene = ShopInviteUtils.issueScene(shopId);
        long expireAt = System.currentTimeMillis() + ShopInviteUtils.TOKEN_TTL_MS;
        String inviteToken = scene.substring(1);
        String resolvedEnvVersion = resolveEnvVersion();
        try {
            log.debug("生成店铺邀请码: shopId={}, page={}, sceneLen={}, checkPath={}, envVersion={}",
                    shopId, SHOP_INVITE_PAGE, scene.length(), checkPath, resolvedEnvVersion);
            byte[] bytes = wxMaService.getQrcodeService().createWxaCodeUnlimitBytes(
                    scene,
                    SHOP_INVITE_PAGE,
                    checkPath,
                    resolvedEnvVersion,
                    430,
                    true,
                    null,
                    false
            );
            String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            return ShopQrcodeVO.builder()
                    .shopCode(shopCode)
                    .imageBase64(base64)
                    .expireAt(expireAt)
                    .inviteContent(SHOP_INVITE_QR_PREFIX + inviteToken)
                    .build();
        } catch (WxErrorException e) {
            int errCode = e.getError().getErrorCode();
            String errMsg = e.getError().getErrorMsg();
            throw new ServiceException(
                    "生成小程序码失败[" + errCode + "]: " + errMsg
                            + "（page=" + SHOP_INVITE_PAGE
                            + ", envVersion=" + resolvedEnvVersion
                            + ", checkPath=" + checkPath + "）"
            );
        }
    }

    /**
     * 解析店铺邀请 token 并返回店铺详情。
     *
     * @param token 邀请 token
     * @return 店铺详情
     */
    @Override
    public ShopDetailRes resolveShopInvite(String token) {
        Long shopId = ShopInviteUtils.getShopId(token);
        if (shopId == null) {
            throw new ServiceException("邀请码已过期或无效");
        }
        return mobiShopService.getShopDetail(shopId);
    }

    /**
     * 为当前用户生成付款码 token（店员扫码扣款用）。
     */
    @Override
    public PayQrcodeVO generatePay() {
        Long userId = SecurityUtils.getUserId();
        String token = PayQrcodeUtils.issueToken(userId);
        long expireAt = System.currentTimeMillis() + PayQrcodeUtils.TOKEN_TTL_MS;
        return PayQrcodeVO.builder()
                .token(token)
                .expireAt(expireAt)
                .qrContent(PAY_QR_PREFIX + token)
                .build();
    }

    /**
     * 店员校验会员付款码并返回会员摘要（须为店铺员工且会员已加入本店）。
     *
     * @param shopId 店铺 ID
     * @param token  付款码 token
     * @return 会员 ID、昵称、手机号与剩余积分
     */
    @Override
    public PayQrcodeVerifyVO verifyPay(Long shopId, String token) {
        if (shopId == null) {
            throw new ServiceException("店铺ID不能为空");
        }
        ShopAccessUtils.assertShopStaff(mobiShopUserService, shopId);
        Long userId = PayQrcodeUtils.getUserId(token);
        if (userId == null) {
            throw new ServiceException("付款码已过期或无效");
        }
        ShopAccessUtils.assertUserIsShopCustomer(mobiShopUserService, userId, shopId);
        MobiUser user = mobiUserService.getById(userId);
        if (user == null) {
            throw new ServiceException("会员不存在");
        }
        MobiPointsAccount account = mobiPointsAccountService.getOrCreateAccount(userId, shopId);
        int remaining = safeInt(account.getBasePoints().add(account.getBonusPoints()));
        return PayQrcodeVerifyVO.builder()
                .userId(userId)
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .remainingPoints(remaining)
                .build();
    }

    /**
     * 为待核销积分订单生成核销二维码。
     *
     * @param logId  积分流水 ID
     * @param shopId 店铺 ID
     * @return 核销码 VO
     */
    @Override
    public OrderQrcodeVO generateOrder(Long logId, Long shopId) {
        if (logId == null || shopId == null) {
            throw new ServiceException("订单信息无效");
        }
        Long userId = SecurityUtils.getUserId();
        MobiPointsLog log = mobiPointsLogService.getById(logId);
        if (log == null) {
            throw new ServiceException("订单不存在");
        }
        if (!userId.equals(log.getUserId())) {
            throw new ServiceException("无权查看该订单");
        }
        if (!shopId.equals(log.getShopId())) {
            throw new ServiceException("订单与店铺不匹配");
        }
        if (log.getStatus() == null || log.getStatus() != PointsLogStatus.PENDING_VERIFY.getCode()) {
            throw new ServiceException("订单不可生成核销码");
        }
        String token = OrderQrcodeUtils.issueToken(logId, shopId, userId);
        long expireAt = System.currentTimeMillis() + OrderQrcodeUtils.TOKEN_TTL_MS;
        return OrderQrcodeVO.builder()
                .token(token)
                .expireAt(expireAt)
                .qrContent(ORDER_QR_PREFIX + token)
                .build();
    }

    /**
     * 为当前用户生成店员邀请码（店长扫码添加店员用）。
     */
    @Override
    public StaffInviteQrcodeVO generateStaffInvite() {
        Long userId = SecurityUtils.getUserId();
        String token = StaffInviteUtils.issueToken(userId);
        long expireAt = System.currentTimeMillis() + StaffInviteUtils.TOKEN_TTL_MS;
        return StaffInviteQrcodeVO.builder()
                .token(token)
                .expireAt(expireAt)
                .qrContent(STAFF_INVITE_QR_PREFIX + token)
                .build();
    }

    /**
     * 将积分余额转为非负整数（用于付款码校验摘要展示）。
     *
     * @param value 积分余额
     * @return 非负整数
     */
    private static int safeInt(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0 ? 0 : value.intValue();
    }

    /**
     * 规范化 env_version，微信仅接受 release / trial / develop。
     */
    private String resolveEnvVersion() {
        String normalized = StringUtils.hasText(envVersion) ? envVersion.trim().toLowerCase() : "develop";
        if (!ALLOWED_ENV_VERSIONS.contains(normalized)) {
            throw new ServiceException(
                    "wx.env-version 配置无效: " + envVersion + "，仅支持 release / trial / develop"
            );
        }
        return normalized;
    }
}
