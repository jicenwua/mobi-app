package com.xcz.member.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "gateway.encryption")
public class EncryptionProperties {
    /***是否开启加密**/
    private boolean enabled = false;
    /***RSA 加密模式（true: RSA+AES 混合加密, false: 仅 AES 加密）**/
    private boolean rsaMode = true;
    /*** 加密密钥 **/
    private String secretKey = null;
    /***忽略路径**/
    private List<String> urls = new ArrayList<>();
}

