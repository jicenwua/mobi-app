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
@ConfigurationProperties(prefix = "gateway.captcha")
public class CaptchaProperties {
    /***是否开启**/
    private boolean enabled = false;
    /***验证码方式**/
    private String type = "char";
    /***有效时间**/
    private Long expireTime = 60 * 1000L;
    /***匹配路径**/
    private List<String> paths = new ArrayList<>();
}
