package com.xcz.member.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.commons.core.constant.SecurityHeaderConstants;
import com.xcz.commons.core.utils.CryptoUtils;
import com.xcz.member.gateway.config.properties.EncryptionProperties;
import jakarta.annotation.Resource;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 响应体加密过滤器：须在 {@link NettyWriteResponseFilter} 之前执行。
 * <p>
 * 是否加密响应与 {@link CryptoGlobalFilter} 对齐：根据请求头 {@code X-Encrypted-Body}
 * 及 POST/PUT 判断（exchange attribute 在 Gateway 写回阶段可能丢失，不能依赖 attribute）。
 * </p>
 */
@Component
public class CryptoResponseEncryptFilter implements GlobalFilter, Ordered {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private EncryptionProperties encryptionProperties;

    @Override
    public int getOrder() {
        return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!shouldEncryptResponse(exchange)) {
            return chain.filter(exchange);
        }

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return DataBufferUtils.join(Flux.from(body))
                        .flatMap(this::writeEncrypted)
                        .switchIfEmpty(super.writeWith(Flux.empty()));
            }

            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return writeWith(Flux.from(body).flatMapSequential(Flux::from));
            }

            private Mono<Void> writeEncrypted(DataBuffer joined) {
                try {
                    byte[] content = new byte[joined.readableByteCount()];
                    joined.read(content);
                    DataBufferUtils.release(joined);

                    String plainResponse = new String(content, StandardCharsets.UTF_8);
                    Map<String, String> responseMap = buildEncryptedResponseMap(plainResponse);
                    String iv = responseMap.get("iv");
                    byte[] finalBytes = OBJECT_MAPPER.writeValueAsString(responseMap)
                            .getBytes(StandardCharsets.UTF_8);

                    HttpHeaders headers = getHeaders();
                    headers.set(SecurityHeaderConstants.ENCRYPTED_RESPONSE, "true");
                    if (iv != null) {
                        headers.set(SecurityHeaderConstants.INITIALIZATION_VECTOR, iv);
                    }
                    headers.remove(HttpHeaders.TRANSFER_ENCODING);
                    headers.setContentLength(finalBytes.length);
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    return super.writeWith(Mono.just(bufferFactory().wrap(finalBytes)));
                } catch (Exception e) {
                    return Mono.error(new RuntimeException("响应加密失败", e));
                }
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    /**
     * 与 {@link CryptoGlobalFilter} 使用相同判定：加密开关 + POST/PUT + X-Encrypted-Body。
     */
    private boolean shouldEncryptResponse(ServerWebExchange exchange) {
        if (!encryptionProperties.isEnabled()) {
            return false;
        }
        ServerHttpRequest request = exchange.getRequest();
        if (isFileOperation(request)) {
            return false;
        }
        String encryptedBodyHeader = request.getHeaders().getFirst(SecurityHeaderConstants.ENCRYPTED_BODY);
        if (encryptedBodyHeader == null) {
            return false;
        }
        HttpMethod method = request.getMethod();
        return HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method);
    }

    private static boolean isFileOperation(ServerHttpRequest request) {
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)
                || contentType.startsWith(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private Map<String, String> buildEncryptedResponseMap(String plainResponse) throws Exception {
        Map<String, String> responseMap = new HashMap<>();
        if (encryptionProperties.isRsaMode()) {
            String aesKey = CryptoUtils.generateAesKey();
            String iv = CryptoUtils.generateIv();
            responseMap.put("encryptedKey", aesKey);
            responseMap.put("payload", CryptoUtils.aesEncrypt(plainResponse, aesKey, iv));
            responseMap.put("iv", iv);
        } else {
            String secret = encryptionProperties.getSecretKey();
            String iv = CryptoUtils.generateIv();
            responseMap.put("payload", CryptoUtils.aesEncrypt(plainResponse, secret, iv));
            responseMap.put("iv", iv);
        }
        return responseMap;
    }
}
