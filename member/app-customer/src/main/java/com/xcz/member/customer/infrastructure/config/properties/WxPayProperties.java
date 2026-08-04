package com.xcz.member.customer.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付配置，绑定 Nacos / application.yml 中 {@code wx.pay.*} 前缀。
 * <p>
 * 仅当配置了 {@code wx.pay.app-id} 时生效，与 {@link com.xcz.member.customer.infrastructure.config.WxMaConfiguration}
 * 中的 {@code WxPayService} Bean 联动。
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "wx.pay")
@ConditionalOnProperty(prefix = "wx.pay", name = "app-id")
public class WxPayProperties {

    /** 小程序 / 公众号 AppID（V3 必填，须与发起支付的应用一致） */
    private String appId;

    /** 微信支付商户号（V3 必填） */
    private String mchId;

    /** V2 API 密钥（仅使用 V2 接口时需要） */
    private String mchKey;

    /** 服务商模式子商户 AppID（普通商户勿配置） */
    private String subAppId;

    /** 服务商模式子商户号（普通商户勿配置） */
    private String subMchId;

    /** V2 证书路径 apiclient_cert.p12 */
    private String keyPath;

    /** V3 API 密钥（V3 必填） */
    private String apiV3Key;

    /** V3 商户 API 证书序列号（V3 必填） */
    private String certSerialNo;

    /** V3 商户 API 证书 apiclient_cert.pem */
    private String privateCertPath;

    /** V3 商户 API 私钥 apiclient_key.pem（V3 必填） */
    private String privateKeyPath;

    /** V3 微信支付公钥 pub_key.pem（2024.08 后新商户验签需要） */
    private String publicKeyPath;

    /** V3 微信支付公钥 ID（与 publicKeyPath 配套） */
    private String publicKeyId;

    /** 支付结果回调地址，如 https://域名/app/pay/notify/order */
    private String notifyUrl;

    /** 退款结果回调地址，如 https://域名/app/pay/notify/refund */
    private String refundNotifyUrl;
}
