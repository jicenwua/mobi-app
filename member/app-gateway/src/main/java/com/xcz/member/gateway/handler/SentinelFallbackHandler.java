package com.xcz.member.gateway.handler;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.xcz.commons.core.utils.ServletUtils;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * Sentinel 限流/熔断降级时的全局异常处理器。
 * <p>
 * 在 {@link com.xcz.member.gateway.config.GatewayConfig} 中注册为 Bean，
 * 并设置 {@code @Order(Ordered.HIGHEST_PRECEDENCE)} 以保证优先于其他异常处理器执行。
 */
public class SentinelFallbackHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 响应已提交（如已开始写出 body），无法再写入自定义内容，直接向上抛出异常
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        // 非 Sentinel 阻塞异常（如普通业务异常），交由其他 WebExceptionHandler 处理
        if (!BlockException.isBlockException(ex)) {
            return Mono.error(ex);
        }
        // Sentinel 触发限流或熔断后，走自定义降级响应
        return handleBlockedRequest(exchange, ex).flatMap(response -> writeResponse(response, exchange));
    }

    /**
     * 委托 Sentinel Gateway 适配器的默认 BlockHandler 处理被拦截的请求。
     */
    private Mono<ServerResponse> handleBlockedRequest(ServerWebExchange exchange, Throwable throwable) {
        return GatewayCallbackManager.getBlockHandler().handleRequest(exchange, throwable);
    }

    /**
     * 向客户端写入统一的降级提示文案（当前未使用 BlockHandler 返回的 ServerResponse 内容）。
     */
    private Mono<Void> writeResponse(ServerResponse response, ServerWebExchange exchange) {
        return ServletUtils.webFluxResponseWriter(exchange.getResponse(), "请求超过最大数，请稍候再试");
    }
}
