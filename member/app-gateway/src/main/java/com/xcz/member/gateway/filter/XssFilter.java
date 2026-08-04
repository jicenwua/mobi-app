package com.xcz.member.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.commons.core.utils.html.EscapeUtil;
import com.xcz.member.gateway.config.properties.XssProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * XSS防护过滤器 - 对请求体中的内容进行XSS清理
 */
@Slf4j
@Component
@Order(-600)
@RequiredArgsConstructor
public class XssFilter implements GlobalFilter {
    private final XssProperties xssProperties;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!xssProperties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();

        // GET 和 DELETE 请求通常没有请求体，直接放行
        if (HttpMethod.GET.equals(method) || HttpMethod.DELETE.equals(method)) {
            return chain.filter(exchange);
        }

        // 只处理 JSON 格式的请求
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType == null || !contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE)) {
            return chain.filter(exchange);
        }

        // 检查是否在排除列表中
        String path = request.getURI().getPath();
        if (xssProperties.getExcludeUrls() != null &&
            StringUtils.matches(path, xssProperties.getExcludeUrls())) {
            return chain.filter(exchange);
        }

        try {
            // 包装 Request 进行 XSS 清理
            ServerHttpRequest decoratedRequest = cleanXssRequest(exchange, request);
            return chain.filter(exchange.mutate().request(decoratedRequest).build());
        } catch (Exception e) {
            log.error("XSS过滤失败", e);
            return chain.filter(exchange);
        }
    }

    /**
     * 对请求体进行XSS清理
     */
    private ServerHttpRequest cleanXssRequest(ServerWebExchange exchange, ServerHttpRequest request) {
        return new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<DataBuffer> getBody() {
                return super.getBody().handle((dataBuffer, sink) -> {
                    byte[] content = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(content);
                    DataBufferUtils.release(dataBuffer);

                    try {
                        String body = new String(content, StandardCharsets.UTF_8);

                        // 如果请求体为空，直接放行
                        if (StringUtils.isEmpty(body)) {
                            sink.next(exchange.getResponse().bufferFactory().wrap(content));
                            return;
                        }

                        // 解析JSON并进行XSS清理
                        String cleanedBody = cleanJsonXss(body);
                        byte[] cleanedBytes = cleanedBody.getBytes(StandardCharsets.UTF_8);

                        sink.next(exchange.getResponse().bufferFactory().wrap(cleanedBytes));
                    } catch (Exception e) {
                        log.error("XSS清理失败，使用原始数据", e);
                        // 如果清理失败，使用原始数据
                        sink.next(exchange.getResponse().bufferFactory().wrap(content));
                    }
                });
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                // 因为内容长度可能变化，移除Content-Length让下游重新计算
                headers.remove(HttpHeaders.CONTENT_LENGTH);
                return headers;
            }
        };
    }

    /**
     * 清理JSON中的XSS内容
     */
    private String cleanJsonXss(String jsonBody) throws Exception {
        JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonBody);

        // 递归清理JSON中的所有字符串字段
        JsonNode cleanedNode = cleanNode(jsonNode);

        return OBJECT_MAPPER.writeValueAsString(cleanedNode);
    }

    /**
     * 递归清理JSON节点
     */
    private JsonNode cleanNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<String> fieldNames = objectNode.fieldNames();

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = objectNode.get(fieldName);
                JsonNode cleanedValue = cleanNode(fieldValue);
                objectNode.set(fieldName, cleanedValue);
            }
            return objectNode;
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                JsonNode arrayElement = node.get(i);
                JsonNode cleanedElement = cleanNode(arrayElement);
                ((com.fasterxml.jackson.databind.node.ArrayNode) node).set(i, cleanedElement);
            }
            return node;
        } else if (node.isTextual()) {
            // 对字符串值进行XSS清理
            String textValue = node.asText();
            String cleanedValue = EscapeUtil.clean(textValue);
            return OBJECT_MAPPER.valueToTree(cleanedValue);
        } else {
            // 其他类型（数字、布尔值等）直接返回
            return node;
        }
    }
}
