package com.xcz.member.feign.support;

import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.feign.config.properties.InternalServiceProperties;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Feign 内部服务入站校验共用逻辑（Servlet / Reactive 一致）。
 */
public final class InternalServiceAuthSupport {

    private InternalServiceAuthSupport() {
    }

    /**
     * 当前应用是否启用入站内部服务令牌校验。
     */
    public static boolean shouldApply(Environment environment, InternalServiceProperties properties) {
        List<String> applications = properties.getInboundAuthApplications();
        if (applications == null || applications.isEmpty()) {
            return false;
        }
        String applicationName = environment.getProperty("spring.application.name");
        return applicationName != null && applications.contains(applicationName);
    }

    /**
     * 需要验证的地址
     */
    public static boolean requiresInternalAuth(String uri, InternalServiceProperties properties) {
        List<String> patterns = properties.getProtectedPaths();
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream().anyMatch(pattern -> StringUtils.isMatch(pattern, uri));
    }

    /**
     * 验证token是否相同
     */
    public static boolean isValidToken(String token, InternalServiceProperties properties) {
        String expected = properties.getToken();
        return StringUtils.isNotEmpty(expected)
                && StringUtils.isNotEmpty(token)
                && expected.equals(token.trim());
    }
}
