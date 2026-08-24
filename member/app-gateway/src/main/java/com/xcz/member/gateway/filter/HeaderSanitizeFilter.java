package com.xcz.member.gateway.filter;

import com.xcz.commons.core.constant.SecurityConstants;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 剥离外部请求伪造的内部服务头，防止经网关越权跳过店铺 ACL 或黑名单。
 */
@Component
@Order(-950)
public class HeaderSanitizeFilter implements GlobalFilter {

    /**
     * 接收请求的时候，删除请求的时候携带的危险请求头
     * @param exchange the current server exchange
     * @param chain provides a way to delegate to the next filter
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(SecurityConstants.FEIGN_INVOKE);
                    headers.remove(SecurityConstants.FROM_SOURCE);
                    headers.remove(SecurityConstants.INTERNAL_SERVICE_HEADER);
                })
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }
}
