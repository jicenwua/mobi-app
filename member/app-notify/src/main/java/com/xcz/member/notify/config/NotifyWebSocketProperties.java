package com.xcz.member.notify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket 端点配置（跨域白名单、单用户最大连接数）。
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "notify.ws")
public class NotifyWebSocketProperties {

    /**
     * 允许建立 WebSocket 连接的 Origin 模式列表。
     */
    private List<String> allowedOrigins = defaultAllowedOrigins();

    /**
     * 同一 clientType + userId 在本节点的最大并发连接数（手机/平板/电脑三端）。
     */
    private int maxConnectionsPerUser = 3;

    private static List<String> defaultAllowedOrigins() {
        List<String> origins = new ArrayList<>();
        origins.add("http://localhost:*");
        origins.add("https://localhost:*");
        origins.add("http://127.0.0.1:*");
        origins.add("https://127.0.0.1:*");
        return origins;
    }
}
