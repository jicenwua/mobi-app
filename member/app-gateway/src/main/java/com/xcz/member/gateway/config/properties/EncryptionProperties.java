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
    /***RSA 私钥（Base64 编码的 PKCS8 格式，仅在 rsaMode=true 时使用）**/
    private String rsaPrivateKey = null;
    /***AES 加密密钥（32位 Base64 编码，仅在 rsaMode=false 时使用）**/
    private String secretKey = null;
    /***忽略路径**/
    private List<String> urls = new ArrayList<>();
}

