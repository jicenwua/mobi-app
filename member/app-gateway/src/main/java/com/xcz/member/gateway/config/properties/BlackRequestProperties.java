package com.xcz.member.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "gateway.black")
public class BlackRequestProperties {
    /***黑名单请求地址**/
    private List<String> urls = null;
    /***黑名单ip**/
    private List<String> ips = null;
    /***白名单地址或者ip**/
    private List<String> whitelist = null;
}
