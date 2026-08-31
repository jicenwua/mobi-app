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
    /**
     * RSA 私钥（PKCS8 Base64，不含 PEM 头尾）。
     * rsaMode=true 时优先使用；与 secretKey 二选一。
     */
    private String rsaPrivateKey = null;
    /**
     * 解密密钥：rsaMode=true 时为 RSA 私钥；rsaMode=false 时为 AES 密钥（Base64）。
     */
    private String secretKey = null;
    /***忽略路径**/
    private List<String> urls = new ArrayList<>();

    /**
     * 获取当前模式下的解密密钥。
     * RSA 模式优先 rsaPrivateKey，避免误将 AES 密钥填入 secretKey。
     */
    public String getDecryptionKey() {
        if (rsaMode) {
            if (rsaPrivateKey != null && !rsaPrivateKey.isBlank()) {
                return rsaPrivateKey.trim();
            }
            if (secretKey != null && !secretKey.isBlank()) {
                return secretKey.trim();
            }
            return null;
        }
        return secretKey != null && !secretKey.isBlank() ? secretKey.trim() : null;
    }
}

