package com.xcz.member.customer.controller;

import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayRefundV3Result;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.commons.security.annotation.Release;
import com.xcz.member.customer.infrastructure.config.properties.WxPayProperties;
import com.xcz.member.customer.infrastructure.service.WeChatPayServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 微信支付联调测试接口。
 * <p>
 * {@code /app/pay/test/**} 需登录后调用；{@code /app/pay/notify/**} 供微信服务器回调，已标注 {@link Release} 免认证。
 * 生产环境请移除测试接口或增加权限控制。
 */
@RestController
@RequestMapping("/app/pay")
@ConditionalOnBean(WxPayProperties.class)
public class WxPayTestController {

    @Resource
    private WeChatPayServiceImpl weChatPayService;

    // ======================== 测试接口（需登录） ========================

    /**
     * 小程序 JSAPI 测试下单。
     *
     * @param openid 小程序用户 openid
     * @return wx.requestPayment 所需参数及 outTradeNo
     */
    @PostMapping("/test/jsapi")
    public ResponseEntity<Map<String, String>> testJsapi(@RequestParam String openid) throws WxPayException {
        return ResponseEntityUtils.ok(weChatPayService.testMiniProgramPay(openid), "下单成功");
    }

    /**
     * 多场景 V3 测试下单。
     *
     * @param openid    JSAPI 场景必填
     * @param tradeType 支付场景，默认 JSAPI
     * @param request   用于获取 H5 场景所需的客户端 IP
     * @return 对应场景的支付调起参数
     */
    @PostMapping("/test/v3")
    public ResponseEntity<Object> testV3(@RequestParam(required = false) String openid,
                                         @RequestParam(defaultValue = "JSAPI") TradeTypeEnum tradeType,
                                         HttpServletRequest request) throws WxPayException {
        return ResponseEntityUtils.ok(
                weChatPayService.testV3Pay(openid, tradeType, resolveClientIp(request)),
                "下单成功"
        );
    }

    /**
     * 关闭未支付订单。
     *
     * @param outTradeNo 商户订单号
     */
    @PostMapping("/test/close")
    public ResponseEntity<Void> testClose(@RequestParam String outTradeNo) throws WxPayException {
        weChatPayService.testCloseOrder(outTradeNo);
        return ResponseEntityUtils.ok(null, "关单成功");
    }

    /**
     * 查询订单支付状态。
     *
     * @param outTradeNo 商户订单号
     * @return 微信订单详情（含 tradeState）
     */
    @GetMapping("/test/query")
    public ResponseEntity<WxPayOrderQueryV3Result> testQuery(@RequestParam String outTradeNo) throws WxPayException {
        return ResponseEntityUtils.ok(weChatPayService.testQueryOrder(outTradeNo), "查询成功");
    }

    /**
     * 申请退款。
     *
     * @param outTradeNo 原商户订单号
     * @param refundFen  退款金额（分），可选，默认全额退
     * @param totalFen   原订单金额（分），可选，默认 100 分
     * @return 退款受理结果
     */
    @PostMapping("/test/refund")
    public ResponseEntity<WxPayRefundV3Result> testRefund(@RequestParam String outTradeNo,
                                                          @RequestParam(required = false) Integer refundFen,
                                                          @RequestParam(required = false) Integer totalFen)
            throws WxPayException {
        return ResponseEntityUtils.ok(
                weChatPayService.testRefund(outTradeNo, refundFen, totalFen),
                "退款申请已提交"
        );
    }

    // ======================== 微信服务器回调（免登录） ========================

    /**
     * 支付成功回调入口。
     * <p>
     * 配置到 {@code wx.pay.notify-url}，须为公网 HTTPS 地址。
     *
     * @param request  微信 POST 请求（含 Wechatpay-* 验签头）
     * @param response 返回 {@code {"code":"SUCCESS"}} 告知微信已收到
     */
    @Release
    @PostMapping("/notify/order")
    public void payNotify(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleNotify(request, response, true);
    }

    /**
     * 退款结果回调入口。
     * <p>
     * 配置到 {@code wx.pay.refund-notify-url}，须为公网 HTTPS 地址。
     *
     * @param request  微信 POST 请求
     * @param response 返回 {@code {"code":"SUCCESS"}} 告知微信已收到
     */
    @Release
    @PostMapping("/notify/refund")
    public void refundNotify(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleNotify(request, response, false);
    }

    /**
     * 统一处理微信回调：验签、业务处理、写 HTTP 应答。
     *
     * @param request   微信回调请求
     * @param response  HTTP 响应
     * @param payNotify true=支付回调，false=退款回调
     */
    private void handleNotify(HttpServletRequest request, HttpServletResponse response, boolean payNotify)
            throws IOException {
        String body = WeChatPayServiceImpl.readNotifyBody(request);
        SignatureHeader header = WeChatPayServiceImpl.buildSignatureHeader(request);
        try {
            if (payNotify) {
                weChatPayService.handlePayNotify(body, header);
            } else {
                weChatPayService.handleRefundNotify(body, header);
            }
            writeNotifyResponse(response, 200, WeChatPayServiceImpl.NOTIFY_SUCCESS_BODY);
        } catch (WxPayException e) {
            writeNotifyResponse(response, 500,
                    "{\"code\":\"FAIL\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 向微信写入回调应答 JSON。
     *
     * @param response HTTP 响应
     * @param status   HTTP 状态码（成功 200，失败 500）
     * @param body     JSON 应答体
     */
    private static void writeNotifyResponse(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body);
    }

    /**
     * 解析客户端真实 IP（兼容反向代理 X-Forwarded-For）。
     *
     * @param request HTTP 请求
     * @return 客户端 IP，H5 支付场景使用
     */
    private static String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
