package com.xcz.member.customer.infrastructure.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import com.xcz.member.customer.infrastructure.config.properties.WxPayProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信小程序与微信支付 SDK 配置。
 * <p>
 * {@link WxMaService} 始终注册；{@link WxPayService} 仅在存在 {@link WxPayProperties} 时注册。
 */
@RefreshScope
@Configuration
public class WxMaConfiguration {

    @Value("${wx.app-id}")
    private String appId;

    @Value("${wx.app-secret}")
    private String appSecret;

    /**
     * 微信小程序 SDK，用于登录、手机号、二维码等能力。
     *
     * @return WxMaService 实例
     */
    @Bean
    public WxMaService wxMaService() {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(appId);
        config.setSecret(appSecret);

        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }

    /**
     * 微信支付 SDK，用于 V3 下单、查单、退款及回调验签。
     * <p>
     * 依赖 {@link WxPayProperties}，未配置 {@code wx.pay.app-id} 时不创建。
     *
     * @param wxPayProperties 支付配置
     * @return WxPayService 实例
     */
    @Bean
    @ConditionalOnBean(WxPayProperties.class)
    public WxPayService wxPayService(WxPayProperties wxPayProperties) {
        WxPayConfig wxPayConfig = new WxPayConfig();
        wxPayConfig.setAppId(wxPayProperties.getAppId());
        wxPayConfig.setMchId(wxPayProperties.getMchId());
        wxPayConfig.setMchKey(wxPayProperties.getMchKey());
        wxPayConfig.setKeyPath(wxPayProperties.getKeyPath());
        wxPayConfig.setApiV3Key(wxPayProperties.getApiV3Key());
        wxPayConfig.setCertSerialNo(wxPayProperties.getCertSerialNo());
        wxPayConfig.setPrivateCertPath(wxPayProperties.getPrivateCertPath());
        wxPayConfig.setPrivateKeyPath(wxPayProperties.getPrivateKeyPath());
        wxPayConfig.setPublicKeyPath(wxPayProperties.getPublicKeyPath());
        wxPayConfig.setPublicKeyId(wxPayProperties.getPublicKeyId());
        wxPayConfig.setSubAppId(wxPayProperties.getSubAppId());
        wxPayConfig.setSubMchId(wxPayProperties.getSubMchId());
        wxPayConfig.setUseSandboxEnv(false);

        WxPayServiceImpl wxPayService = new WxPayServiceImpl();
        wxPayService.setConfig(wxPayConfig);
        return wxPayService;
    }
}
