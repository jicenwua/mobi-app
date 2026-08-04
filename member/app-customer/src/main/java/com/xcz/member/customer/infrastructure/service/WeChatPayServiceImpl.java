package com.xcz.member.customer.infrastructure.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyV3Result;
import com.github.binarywang.wxpay.bean.notify.WxPayRefundNotifyV3Result;
import com.github.binarywang.wxpay.bean.request.WxPayRefundV3Request;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayRefundV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.xcz.member.customer.infrastructure.config.properties.WxPayProperties;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付 V3 联调测试服务。
 * <p>
 * 提供下单、关单、查单、退款及回调处理能力；联调通过后可将业务逻辑迁移至正式订单模块。
 * 仅在配置了 {@code wx.pay.app-id} 时生效（与 {@link WxPayProperties}、{@link WxPayService} 联动）。
 */
@Slf4j
@Service
@ConditionalOnBean(WxPayProperties.class)
public class WeChatPayServiceImpl {

    /** 东八区，用于构造订单过期时间 */
    private static final ZoneOffset CHINA_OFFSET = ZoneOffset.ofHours(8);

    /** 微信 V3 要求的 RFC3339 时间格式 */
    private static final DateTimeFormatter RFC3339 = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /** 测试默认金额：1 元（100 分） */
    private static final int TEST_AMOUNT_FEN = 100;

    /** 微信 V3 回调成功时 HTTP 响应体 */
    public static final String NOTIFY_SUCCESS_BODY = "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";

    @Resource
    private WxPayService wxPayService;

    @Resource
    private WxPayProperties wxPayProperties;

    // ======================== 下单测试 ========================

    /**
     * 小程序 JSAPI 测试下单。
     * <p>
     * 调用微信 V3 统一下单接口，返回前端 {@code wx.requestPayment} 所需参数。
     *
     * @param openid 小程序用户 openid（登录后获取）
     * @return 支付参数，含 outTradeNo、timeStamp、nonceStr、package、signType、paySign
     * @throws WxPayException 微信接口调用失败
     */
    public Map<String, String> testMiniProgramPay(String openid) throws WxPayException {
        String outTradeNo = IdUtil.getSnowflakeNextIdStr();
        WxPayUnifiedOrderV3Request request = buildBaseRequest(outTradeNo, TEST_AMOUNT_FEN, "测试商品-小程序");

        WxPayUnifiedOrderV3Request.Payer payer = new WxPayUnifiedOrderV3Request.Payer();
        payer.setOpenid(openid);
        request.setPayer(payer);

        WxPayUnifiedOrderV3Result.JsapiResult result =
                wxPayService.createOrderV3(TradeTypeEnum.JSAPI, request);

        Map<String, String> payParams = new HashMap<>();
        payParams.put("outTradeNo", outTradeNo);
        payParams.put("timeStamp", result.getTimeStamp());
        payParams.put("nonceStr", result.getNonceStr());
        payParams.put("package", result.getPackageValue());
        payParams.put("signType", result.getSignType());
        payParams.put("paySign", result.getPaySign());
        return payParams;
    }

    /**
     * 多支付场景 V3 测试下单。
     * <p>
     * 按 {@link TradeTypeEnum} 区分返回类型：
     * <ul>
     *   <li>JSAPI → {@link WxPayUnifiedOrderV3Result.JsapiResult}</li>
     *   <li>APP → {@link WxPayUnifiedOrderV3Result.AppResult}</li>
     *   <li>H5 / NATIVE → 支付链接字符串</li>
     * </ul>
     *
     * @param openid    JSAPI 必填，用户 openid
     * @param tradeType 支付场景：JSAPI / H5 / NATIVE / APP
     * @param clientIp  H5 必填，用户客户端 IP
     * @return 对应场景的支付调起参数
     * @throws WxPayException           微信接口调用失败
     * @throws IllegalArgumentException 必填参数缺失
     */
    public Object testV3Pay(String openid, TradeTypeEnum tradeType, String clientIp) throws WxPayException {
        String outTradeNo = IdUtil.getSnowflakeNextIdStr();
        WxPayUnifiedOrderV3Request request = buildBaseRequest(outTradeNo, TEST_AMOUNT_FEN, "测试商品-" + tradeType.name());

        return switch (tradeType) {
            case JSAPI -> {
                if (StrUtil.isBlank(openid)) {
                    throw new IllegalArgumentException("JSAPI 支付必须提供 openid");
                }
                WxPayUnifiedOrderV3Request.Payer payer = new WxPayUnifiedOrderV3Request.Payer();
                payer.setOpenid(openid);
                request.setPayer(payer);
                yield wxPayService.createOrderV3(TradeTypeEnum.JSAPI, request);
            }
            case H5 -> {
                if (StrUtil.isBlank(clientIp)) {
                    throw new IllegalArgumentException("H5 支付必须提供 clientIp");
                }
                WxPayUnifiedOrderV3Request.SceneInfo sceneInfo = new WxPayUnifiedOrderV3Request.SceneInfo();
                sceneInfo.setPayerClientIp(clientIp);
                WxPayUnifiedOrderV3Request.H5Info h5Info = new WxPayUnifiedOrderV3Request.H5Info();
                h5Info.setType("Wap");
                sceneInfo.setH5Info(h5Info);
                request.setSceneInfo(sceneInfo);
                yield wxPayService.createOrderV3(TradeTypeEnum.H5, request);
            }
            case NATIVE -> wxPayService.createOrderV3(TradeTypeEnum.NATIVE, request);
            case APP -> wxPayService.createOrderV3(TradeTypeEnum.APP, request);
        };
    }

    // ======================== 关单 / 查单 / 退款测试 ========================

    /**
     * 关闭未支付订单（取消下单）。
     * <p>
     * 仅对状态为 NOTPAY 的订单有效；已支付订单请走退款流程。
     *
     * @param outTradeNo 商户订单号
     * @throws WxPayException           微信接口调用失败
     * @throws IllegalArgumentException outTradeNo 为空
     */
    public void testCloseOrder(String outTradeNo) throws WxPayException {
        if (StrUtil.isBlank(outTradeNo)) {
            throw new IllegalArgumentException("outTradeNo 不能为空");
        }
        wxPayService.closeOrderV3(outTradeNo);
        log.info("[微信支付测试] 关单成功 outTradeNo={}", outTradeNo);
    }

    /**
     * 查询订单支付状态。
     *
     * @param outTradeNo 商户订单号
     * @return 订单详情，{@code tradeState} 常见值：NOTPAY / SUCCESS / CLOSED / REFUND
     * @throws WxPayException           微信接口调用失败
     * @throws IllegalArgumentException outTradeNo 为空
     */
    public WxPayOrderQueryV3Result testQueryOrder(String outTradeNo) throws WxPayException {
        if (StrUtil.isBlank(outTradeNo)) {
            throw new IllegalArgumentException("outTradeNo 不能为空");
        }
        WxPayOrderQueryV3Result result = wxPayService.queryOrderV3(outTradeNo, wxPayProperties.getMchId());
        log.info("[微信支付测试] 查单 outTradeNo={} tradeState={}", outTradeNo, result.getTradeState());
        return result;
    }

    /**
     * 申请退款（测试环境默认全额退 1 元）。
     * <p>
     * 退款结果异步通过 {@link #handleRefundNotify} 回调通知。
     *
     * @param outTradeNo 原商户订单号（须已支付成功）
     * @param refundFen  退款金额（分），null 则退全额
     * @param totalFen   原订单金额（分），null 则使用测试默认 100 分
     * @return 退款受理结果，{@code status} 通常为 PROCESSING
     * @throws WxPayException           微信接口调用失败
     * @throws IllegalArgumentException 参数无效
     */
    public WxPayRefundV3Result testRefund(String outTradeNo, Integer refundFen, Integer totalFen) throws WxPayException {
        if (StrUtil.isBlank(outTradeNo)) {
            throw new IllegalArgumentException("outTradeNo 不能为空");
        }
        int total = totalFen != null ? totalFen : TEST_AMOUNT_FEN;
        int refund = refundFen != null ? refundFen : total;
        if (refund <= 0 || refund > total) {
            throw new IllegalArgumentException("退款金额无效");
        }

        String outRefundNo = "RF" + IdUtil.getSnowflakeNextIdStr();

        WxPayRefundV3Request request = new WxPayRefundV3Request();
        request.setOutTradeNo(outTradeNo);
        request.setOutRefundNo(outRefundNo);
        request.setReason("测试退款");
        request.setNotifyUrl(wxPayProperties.getRefundNotifyUrl());

        WxPayRefundV3Request.Amount amount = new WxPayRefundV3Request.Amount();
        amount.setRefund(refund);
        amount.setTotal(total);
        amount.setCurrency("CNY");
        request.setAmount(amount);

        WxPayRefundV3Result result = wxPayService.refundV3(request);
        log.info("[微信支付测试] 退款申请 outTradeNo={} outRefundNo={} status={}",
                outTradeNo, outRefundNo, result.getStatus());
        return result;
    }

    // ======================== 微信回调 ========================

    /**
     * 处理支付成功回调（V3）。
     * <p>
     * 验签并解密微信推送的支付通知；{@code tradeState=SUCCESS} 时触发 {@link #onPaySuccess}。
     *
     * @param notifyBody      微信 POST 的原始 JSON 报文
     * @param signatureHeader 请求头中的验签信息（Wechatpay-* 系列头）
     * @return 解密后的支付结果
     * @throws WxPayException 验签失败或报文解析失败
     */
    public WxPayNotifyV3Result.DecryptNotifyResult handlePayNotify(String notifyBody, SignatureHeader signatureHeader)
            throws WxPayException {
        WxPayNotifyV3Result notifyResult = wxPayService.parseOrderNotifyV3Result(notifyBody, signatureHeader);
        WxPayNotifyV3Result.DecryptNotifyResult data = notifyResult.getResult();

        log.info("[微信支付回调] outTradeNo={} transactionId={} tradeState={} amount={}",
                data.getOutTradeNo(),
                data.getTransactionId(),
                data.getTradeState(),
                data.getAmount() != null ? data.getAmount().getTotal() : null);

        if (!"SUCCESS".equals(data.getTradeState())) {
            log.warn("[微信支付回调] 非成功状态 outTradeNo={} tradeState={}",
                    data.getOutTradeNo(), data.getTradeState());
            return data;
        }

        onPaySuccess(data);
        return data;
    }

    /**
     * 处理退款结果回调（V3）。
     * <p>
     * 验签并解密微信推送的退款通知；{@code refundStatus=SUCCESS} 时触发 {@link #onRefundSuccess}。
     *
     * @param notifyBody      微信 POST 的原始 JSON 报文
     * @param signatureHeader 请求头中的验签信息
     * @return 解密后的退款结果
     * @throws WxPayException 验签失败或报文解析失败
     */
    public WxPayRefundNotifyV3Result.DecryptNotifyResult handleRefundNotify(String notifyBody,
                                                                             SignatureHeader signatureHeader)
            throws WxPayException {
        WxPayRefundNotifyV3Result notifyResult = wxPayService.parseRefundNotifyV3Result(notifyBody, signatureHeader);
        WxPayRefundNotifyV3Result.DecryptNotifyResult data = notifyResult.getResult();

        log.info("[微信退款回调] outTradeNo={} outRefundNo={} refundId={} refundStatus={}",
                data.getOutTradeNo(),
                data.getOutRefundNo(),
                data.getRefundId(),
                data.getRefundStatus());

        if (!"SUCCESS".equals(data.getRefundStatus())) {
            log.warn("[微信退款回调] 非成功状态 outRefundNo={} refundStatus={}",
                    data.getOutRefundNo(), data.getRefundStatus());
            return data;
        }

        onRefundSuccess(data);
        return data;
    }

    /**
     * 从 HTTP 请求头构建微信 V3 验签信息。
     *
     * @param request 微信回调请求
     * @return 包含 Timestamp、Nonce、Serial、Signature 的验签头
     */
    public static SignatureHeader buildSignatureHeader(HttpServletRequest request) {
        return SignatureHeader.builder()
                .timeStamp(request.getHeader("Wechatpay-Timestamp"))
                .nonce(request.getHeader("Wechatpay-Nonce"))
                .serial(request.getHeader("Wechatpay-Serial"))
                .signature(request.getHeader("Wechatpay-Signature"))
                .build();
    }

    /**
     * 读取微信回调原始报文。
     * <p>
     * 须从 {@link HttpServletRequest#getInputStream()} 读取，不可使用 {@code @RequestBody} 二次解析。
     *
     * @param request 微信回调请求
     * @return UTF-8 编码的 JSON 字符串
     * @throws java.io.IOException 读取请求体失败
     */
    public static String readNotifyBody(HttpServletRequest request) throws java.io.IOException {
        return new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    // ======================== 业务占位（测试阶段仅打日志） ========================

    /**
     * 支付成功后的业务处理（占位）。
     * <p>
     * 联调通过后在此实现：更新订单状态、写支付流水、发货等。
     *
     * @param data 解密后的支付通知
     */
    protected void onPaySuccess(WxPayNotifyV3Result.DecryptNotifyResult data) {
        log.info("[微信支付测试] 支付成功业务处理占位 outTradeNo={}", data.getOutTradeNo());
    }

    /**
     * 退款成功后的业务处理（占位）。
     * <p>
     * 联调通过后在此实现：更新订单状态、回滚库存/积分等。
     *
     * @param data 解密后的退款通知
     */
    protected void onRefundSuccess(WxPayRefundNotifyV3Result.DecryptNotifyResult data) {
        log.info("[微信支付测试] 退款成功业务处理占位 outRefundNo={}", data.getOutRefundNo());
    }

    /**
     * 构建 V3 统一下单公共请求体。
     *
     * @param outTradeNo  商户订单号
     * @param totalFen    订单金额（分）
     * @param description 商品描述
     * @return 已填充 appid、mchid、notifyUrl、过期时间等字段的请求对象
     */
    private WxPayUnifiedOrderV3Request buildBaseRequest(String outTradeNo, int totalFen, String description) {
        WxPayUnifiedOrderV3Request request = new WxPayUnifiedOrderV3Request();
        request.setAppid(wxPayProperties.getAppId());
        request.setMchid(wxPayProperties.getMchId());
        request.setDescription(description);
        request.setOutTradeNo(outTradeNo);
        request.setNotifyUrl(wxPayProperties.getNotifyUrl());
        request.setTimeExpire(OffsetDateTime.now(CHINA_OFFSET).plusMinutes(5).format(RFC3339));

        WxPayUnifiedOrderV3Request.Amount amount = new WxPayUnifiedOrderV3Request.Amount();
        amount.setTotal(totalFen);
        amount.setCurrency("CNY");
        request.setAmount(amount);
        return request;
    }
}
