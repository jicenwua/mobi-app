package com.xcz.member.feign.interceptor;

import com.xcz.commons.core.constant.Constants;
import com.xcz.commons.core.constant.SecurityConstants;
import com.xcz.member.feign.config.properties.InternalServiceProperties;
import com.xcz.member.feign.support.InternalServiceAuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Reactive 环境：校验 Feign 管理端入口的内部服务令牌。
 */
@RequiredArgsConstructor
public class InternalServiceAuthWebFilter implements WebFilter {

    private final InternalServiceProperties internalServiceProperties;
    private final Environment environment;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!InternalServiceAuthSupport.shouldApply(environment, internalServiceProperties)) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getPath().value();
        if (!InternalServiceAuthSupport.requiresInternalAuth(path, internalServiceProperties)) {
            return chain.filter(exchange);
        }
        String token = exchange.getRequest().getHeaders().getFirst(SecurityConstants.INTERNAL_SERVICE_HEADER);
        if (InternalServiceAuthSupport.isValidToken(token, internalServiceProperties)) {
            return chain.filter(exchange);
        }
        return forbidden(exchange.getResponse());
    }

    private Mono<Void> forbidden(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, Constants.CONTENT_TYPE);
        String body = "{\"code\":403,\"msg\":\"非法的内部服务请求\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
