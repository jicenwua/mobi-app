package com.xcz.member.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "gateway.xss")
public class XssProperties {
    /***是否开启**/
    private boolean enabled = false;
    /***排除过滤路径**/
    private List<String> excludeUrls = null;
}
