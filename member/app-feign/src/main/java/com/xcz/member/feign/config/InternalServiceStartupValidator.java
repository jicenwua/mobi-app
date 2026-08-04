package com.xcz.member.feign.config;

import com.xcz.commons.core.constant.SecurityConstants;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.feign.config.properties.InternalServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 启动时校验内部服务令牌强度，避免生产环境使用默认值。
 */
@Slf4j
@RequiredArgsConstructor
public class InternalServiceStartupValidator implements ApplicationRunner {

    private final InternalServiceProperties internalServiceProperties;

    @Override
    public void run(ApplicationArguments args) {
        String token = internalServiceProperties.getToken();
        if (StringUtils.isEmpty(token)) {
            throw new IllegalStateException(
                    "security.internal-service.token 未配置，请在 Nacos 或 application.yml 中设置强随机令牌");
        }
        if (SecurityConstants.INTERNAL_SERVICE_VALUE.equals(token)) {
            String message = "内部服务令牌仍使用默认值 mobi-feign，请在 Nacos 配置 security.internal-service.token";
            throw new IllegalStateException(message);
        } else if (token.length() < 16) {
            log.warn("内部服务令牌长度过短（<16），建议使用随机字符串");
        }
    }

}
