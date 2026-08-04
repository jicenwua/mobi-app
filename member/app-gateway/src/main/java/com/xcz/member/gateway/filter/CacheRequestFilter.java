package com.xcz.member.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 请求体缓存过滤器 - 缓存请求体以便后续过滤器可以多次读取
 * 在 CryptoGlobalFilter 解密后执行，缓存解密后的明文数据
 */
@Component
@Order(-700)
public class CacheRequestFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        //检查请求方法，GET 和 DELETE 通常没有请求体，不需要缓存，直接放行
        HttpMethod method = exchange.getRequest().getMethod();
        if (method == HttpMethod.GET || method == HttpMethod.DELETE) {
            return chain.filter(exchange);
        }

        // ⭐ 添加：跳过 multipart/form-data 请求（文件上传）
        String contentType = exchange.getRequest().getHeaders().getFirst("Content-Type");
        if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
            // 文件上传请求不缓存，直接放行
            return chain.filter(exchange);
        }

        return ServerWebExchangeUtils.cacheRequestBodyAndRequest(exchange, (serverHttpRequest) -> {
            // 如果生成的请求对象没变，说明不需要特殊处理，继续执行后续过滤器
            if (serverHttpRequest == exchange.getRequest()) {
                return chain.filter(exchange);
            }
            // 使用 mutate() 重新构建 exchange，将包含"已缓存 Body"的新请求对象传递下去
            return chain.filter(exchange.mutate().request(serverHttpRequest).build());
        });
    }


}
