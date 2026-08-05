package com.xcz.member.gateway.filter;

import com.xcz.commons.core.constant.SecurityHeaderConstants;
import com.xcz.commons.core.utils.CryptoUtils;
import com.xcz.member.gateway.config.properties.EncryptionProperties;
import jakarta.annotation.Resource;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 响应体加密过滤器：须在 {@link NettyWriteResponseFilter} 之前执行。
 * <p>
 * 复用 {@link CryptoGlobalFilter} 协商的 AES 会话密钥；IV 写入响应头 {@code X-IV}，body 仅为密文。
 * </p>
 */
@Order(NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1)
@Component
public class CryptoResponseEncryptFilter implements GlobalFilter {

    @Resource
    private EncryptionProperties encryptionProperties;

    /**
     * 包装响应写出逻辑：下游返回明文时，在写回客户端前加密并设置响应头。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 不满足加密条件时直接走原始响应
        if (!shouldEncryptResponse(exchange)) {
            return chain.filter(exchange);
        }

        // 装饰 Response，拦截 writeWith 对 body 做加密
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                // 合并分片响应体后再加密
                return DataBufferUtils.join(Flux.from(body))
                        .flatMap(joined -> writeEncrypted(exchange, joined))
                        .switchIfEmpty(super.writeWith(Flux.empty()));
            }

            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                // 统一走 writeWith，避免分块写出绕过加密
                return writeWith(Flux.from(body).flatMapSequential(Flux::from));
            }

            /**
             * 将下游明文响应加密为密文 body，IV 写入响应头。
             */
            private Mono<Void> writeEncrypted(ServerWebExchange exchange, DataBuffer joined) {
                try {
                    byte[] content = new byte[joined.readableByteCount()];
                    joined.read(content);
                    DataBufferUtils.release(joined);

                    // 下游返回的原始 JSON 明文
                    String plainResponse = new String(content, StandardCharsets.UTF_8);
                    EncryptedResponse encrypted = encryptResponse(exchange, plainResponse);
                    byte[] finalBytes = encrypted.payload.getBytes(StandardCharsets.UTF_8);

                    HttpHeaders headers = getHeaders();
                    // 告知客户端响应已加密
                    headers.set(SecurityHeaderConstants.ENCRYPTED_RESPONSE, "true");
                    // IV 仅放在响应头，body 不再重复返回 iv
                    headers.set(SecurityHeaderConstants.INITIALIZATION_VECTOR, encrypted.iv);
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
     * 判断是否应对本次响应加密。
     * <p>
     * 条件：加密已开启 + 非文件请求 + 请求带 X-Encrypted-Body + exchange 中已有会话密钥。
     * </p>
     */
    private boolean shouldEncryptResponse(ServerWebExchange exchange) {
        if (!encryptionProperties.isEnabled()) {
            return false;
        }
        ServerHttpRequest request = exchange.getRequest();
        if (isFileOperation(request)) {
            return false;
        }
        // 与 CryptoGlobalFilter 对齐：仅处理客户端声明需加解密的请求
        String encryptedBodyHeader = request.getHeaders().getFirst(SecurityHeaderConstants.ENCRYPTED_BODY);
        if (encryptedBodyHeader == null) {
            return false;
        }
        // 会话密钥由 CryptoGlobalFilter 在解密/协商时写入
        return exchange.getAttribute(SecurityHeaderConstants.SESSION_AES_KEY_ATTR) != null;
    }

    /**
     * 判断是否为文件类请求（上传/下载），此类响应不加密。
     */
    private static boolean isFileOperation(ServerHttpRequest request) {
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)
                || contentType.startsWith(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    /**
     * 使用会话 AES 密钥加密响应明文，返回密文与 IV（IV 由调用方写入响应头）。
     */
    private EncryptedResponse encryptResponse(ServerWebExchange exchange, String plainResponse) throws Exception {
        String sessionKey = exchange.getAttribute(SecurityHeaderConstants.SESSION_AES_KEY_ATTR);
        if (sessionKey == null || sessionKey.isEmpty()) {
            throw new IllegalStateException("缺少 AES 会话密钥，无法加密响应");
        }
        String iv = CryptoUtils.generateIv();
        String payload = CryptoUtils.aesEncrypt(plainResponse, sessionKey, iv);
        return new EncryptedResponse(payload, iv);
    }

    /** 响应加密结果：payload 写 body，iv 写响应头 */
    private record EncryptedResponse(String payload, String iv) {
    }
}
