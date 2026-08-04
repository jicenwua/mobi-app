package com.xcz.member.feign.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.List;

@Data
@RefreshScope
@ConfigurationProperties(prefix = "security.internal-service")
public class InternalServiceProperties {

    /**
     * 服务间 Feign 内部认证令牌，各微服务 Nacos 配置需保持一致。
     */
    private String token;

    /**
     * 启用入站内部服务令牌校验的应用名（{@code spring.application.name}）。
     * 为空则不校验；扩展新服务时在此追加即可。
     */
    private List<String> inboundAuthApplications = List.of("app-miniApp");

    /**
     * 必须携带合法内部令牌的 Feign 管理端路径（Ant 风格）。
     */
    private List<String> protectedPaths = List.of(
            "/shop/sys/**",
            "/mobi/user/sys/**",
            "/mobi/role/**",
            "/ticket/sys/**",
            "/user-coupon/sys/**",
            "/points/sys/**",
            "/product/list",
            "/product/status"
    );
}
