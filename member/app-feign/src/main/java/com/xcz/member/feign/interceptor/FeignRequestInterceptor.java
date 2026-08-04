package com.xcz.member.feign.interceptor;

import com.xcz.commons.core.constant.SecurityConstants;
import com.xcz.commons.core.utils.ServletUtils;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.commons.core.utils.ip.IpUtils;
import com.xcz.member.feign.config.properties.InternalServiceProperties;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Feign 请求拦截：转发用户上下文请求头，并附加内部服务认证令牌。
 */
@RequiredArgsConstructor
public class FeignRequestInterceptor implements RequestInterceptor {

    private final InternalServiceProperties internalServiceProperties;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        HttpServletRequest request = ServletUtils.getRequest();
        Map<String, String> headers = ServletUtils.getHeaders(request);
        String userId = headers.get(SecurityConstants.DETAILS_USER_ID);
        if (StringUtils.isNotEmpty(userId)) {
            requestTemplate.header(SecurityConstants.DETAILS_USER_ID, userId);
        }
        String userKey = headers.get(SecurityConstants.USER_KEY);
        if (StringUtils.isNotEmpty(userKey)) {
            requestTemplate.header(SecurityConstants.USER_KEY, userKey);
        }
        String userName = headers.get(SecurityConstants.DETAILS_USERNAME);
        if (StringUtils.isNotEmpty(userName)) {
            requestTemplate.header(SecurityConstants.DETAILS_USERNAME, userName);
        }
        String authentication = headers.get(SecurityConstants.AUTHORIZATION_HEADER);
        if (StringUtils.isNotEmpty(authentication)) {
            requestTemplate.header(SecurityConstants.AUTHORIZATION_HEADER, authentication);
        }

        requestTemplate.header("X-Forwarded-For", IpUtils.getIpAddr(ServletUtils.getRequest()));
        requestTemplate.header(SecurityConstants.INTERNAL_SERVICE_HEADER, internalServiceProperties.getToken());
    }
}
