package com.xcz.member.feign.interceptor;

import com.xcz.commons.core.constant.Constants;
import com.xcz.commons.core.constant.SecurityConstants;
import com.xcz.member.feign.config.properties.InternalServiceProperties;
import com.xcz.member.feign.support.InternalServiceAuthSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet 环境：校验 Feign 管理端入口的内部服务令牌。
 */
@RequiredArgsConstructor
public class InternalServiceAuthFilter extends OncePerRequestFilter {

    private final InternalServiceProperties internalServiceProperties;
    private final Environment environment;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!InternalServiceAuthSupport.shouldApply(environment, internalServiceProperties)) {
            filterChain.doFilter(request, response);
            return;
        }
        String uri = request.getRequestURI();
        if (!InternalServiceAuthSupport.requiresInternalAuth(uri, internalServiceProperties)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = request.getHeader(SecurityConstants.INTERNAL_SERVICE_HEADER);
        if (!InternalServiceAuthSupport.isValidToken(token, internalServiceProperties)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(Constants.CONTENT_TYPE);
            response.getWriter().write("{\"code\":403,\"msg\":\"非法的内部服务请求\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
