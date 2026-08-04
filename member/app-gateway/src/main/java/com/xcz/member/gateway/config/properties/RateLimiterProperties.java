package com.xcz.member.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "gateway.ratelimit")
public class RateLimiterProperties {
    /***是否开启**/
    private boolean enabled = false;
    /***每秒最大请求数**/
    private int permitsPerSecond = 10;
    /***最多限流次数**/
    private int burstLimit = 10;
    /***黑名单计算时间**/
    private int timeout = 24 * 60 * 60 * 1000;
}
