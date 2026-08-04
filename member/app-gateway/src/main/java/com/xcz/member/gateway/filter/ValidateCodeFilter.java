package com.xcz.member.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.gateway.config.properties.CaptchaProperties;
import com.xcz.member.gateway.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 登录等接口的图形验证码校验（须在 {@link CryptoGlobalFilter}、{@link CacheRequestFilter} 之后执行）。
 */
@Component
@Order(-500)
@RequiredArgsConstructor
public class ValidateCodeFilter implements GlobalFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CaptchaService captchaService;
    private final CaptchaProperties captchaProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!shouldValidate(exchange)) {
            return chain.filter(exchange);
        }

        return readBodyAsString(exchange)
                .flatMap(body -> {
                    try {
                        JsonNode jsonNode = OBJECT_MAPPER.readTree(body);
                        String uuid = jsonNode.path("uuid").asText(null);
                        String code = jsonNode.path("code").asText(null);
                        captchaService.validate(uuid, code);
                        return chain.filter(exchange);
                    } catch (Exception e) {
                        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                        return exchange.getResponse().setComplete();
                    }
                });
    }

    /**
     * 是否命中需验证码的路径
     */
    private boolean shouldValidate(ServerWebExchange exchange) {
        if (!captchaProperties.isEnabled()) {
            return false;
        }
        String path = exchange.getRequest().getURI().getPath();
        return StringUtils.matches(path, captchaProperties.getPaths());
    }

    private Mono<String> readBodyAsString(ServerWebExchange exchange) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .defaultIfEmpty("");
    }
}
