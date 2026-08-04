package com.xcz.member.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.commons.core.constant.SecurityHeaderConstants;
import com.xcz.commons.core.utils.CryptoUtils;
import com.xcz.commons.core.utils.RequestSignatureUtils;
import com.xcz.member.gateway.config.properties.EncryptionProperties;
import jakarta.annotation.Resource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求解密过滤器：对带 {@code X-Encrypted-Body} 的 POST/PUT 解密后转发。
 * 响应加密由 {@link CryptoResponseEncryptFilter} 在写回客户端前处理。
 */
@Component
@Order(-800)
public class CryptoGlobalFilter implements GlobalFilter {
    @Resource
    private EncryptionProperties encryptionProperties;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long TIMESTAMP_THRESHOLD_MS = 60_000; // 60秒

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        //未开启加密直接放行
        if (!encryptionProperties.isEnabled()) {
            return chain.filter(exchange);
        }

        // 放行文件上传和下载请求（multipart/form-data 或 application/octet-stream）
        if (isFileOperation(request)) {
            return chain.filter(exchange);
        }

        // 仅对 POST/PUT 请求且包含加密头的请求进行拦截
        String encryptedBodyHeader = request.getHeaders().getFirst(SecurityHeaderConstants.ENCRYPTED_BODY);
//        if (encryptedBodyHeader == null || (!HttpMethod.POST.equals(request.getMethod()) && !HttpMethod.PUT.equals(request.getMethod()))) {
        if (encryptedBodyHeader == null) {
            return chain.filter(exchange); // 不加密的请求直接放行
        }

        try {
            // RSA+AES 混合加密模式
            if (encryptionProperties.isRsaMode()) {
                return handleRsaAesMode(exchange, request, chain);
            }

            // 纯 AES 加密模式（旧版本兼容）
            return handleAesMode(exchange, request, chain);
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * RSA + AES 混合加密模式（推荐，更安全）。
     * 解密请求体并包装响应加密装饰器。
     */
    private Mono<Void> handleRsaAesMode(ServerWebExchange exchange, ServerHttpRequest request, GatewayFilterChain chain) {
        try {
            // 1. 获取 RSA 私钥
            String rsaPrivateKey = encryptionProperties.getRsaPrivateKey();
            if (rsaPrivateKey == null || rsaPrivateKey.isEmpty()) {
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                return exchange.getResponse().setComplete();
            }

            // 2. 包装 Request 进行解密（RSA 解密 AES 密钥 + AES 解密请求体）
            ServerHttpRequest decoratedRequest = decryptRequestWithRsaAes(exchange, request, rsaPrivateKey);

            // 3. 继续过滤器链（响应加密见 CryptoResponseEncryptFilter，按 X-Encrypted-Body 判定）
            return chain.filter(exchange.mutate().request(decoratedRequest).build());
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * 纯 AES 加密模式（旧版本兼容）。
     * 校验时间戳与签名后解密请求，并用固定密钥加密响应。
     */
    private Mono<Void> handleAesMode(ServerWebExchange exchange, ServerHttpRequest request, GatewayFilterChain chain) {
        try {
            // 1. 防重放：时间戳校验
            String timestampStr = request.getHeaders().getFirst(SecurityHeaderConstants.TIMESTAMP);
            String nonce = request.getHeaders().getFirst(SecurityHeaderConstants.NONCE);
            String signature = request.getHeaders().getFirst(SecurityHeaderConstants.SIGNATURE);
            //需要三个加密参数都传递
            if (timestampStr == null || nonce == null || signature == null) {
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                return exchange.getResponse().setComplete();
            }
            //请求超过一分钟不处理
            long timestamp = Long.parseLong(timestampStr);
            if (System.currentTimeMillis() - timestamp > TIMESTAMP_THRESHOLD_MS) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // 2. 获取密钥
            String secret = encryptionProperties.getSecretKey();

            // 3. 验证签名
            Map<String, String> params = extractRequestParams(request);
            boolean isSignatureValid = RequestSignatureUtils.verifySignature(params, timestampStr, nonce, signature, secret);
            if (!isSignatureValid) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // 4. 包装 Request 进行解密
            ServerHttpRequest decoratedRequest = decryptRequest(exchange, request, secret);

            // 5. 继续过滤器链
            return chain.filter(exchange.mutate().request(decoratedRequest).build());
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * 使用 RSA + AES 混合解密请求体。
     * <p>
     * 前端请求格式：{@code {encryptedKey, payload, iv}}，先用 RSA 私钥解密 AES 密钥，再 AES 解密 payload。
     * </p>
     *
     * @param exchange      当前交换上下文
     * @param request       原始请求
     * @param rsaPrivateKey RSA 私钥（PKCS8 Base64）
     * @return 解密后的请求装饰器
     */
    private ServerHttpRequest decryptRequestWithRsaAes(ServerWebExchange exchange, ServerHttpRequest request, String rsaPrivateKey) {
        return new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<DataBuffer> getBody() {
                // 必须合并全部 DataBuffer 再解析 JSON；否则大 body 分片时会在字符串中间截断，触发 JsonEOFException
                return DataBufferUtils.join(super.getBody())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("请求体为空")))
                        .flatMapMany((DataBuffer joined) -> {
                            try {
                                byte[] content = new byte[joined.readableByteCount()];
                                joined.read(content);
                                DataBufferUtils.release(joined);

                                String requestBody = new String(content, StandardCharsets.UTF_8);
                                JsonNode jsonNode = OBJECT_MAPPER.readTree(requestBody);

                                // 1. 提取 RSA 加密的 AES 密钥
                                String encryptedAesKey = jsonNode.get("encryptedKey").asText();

                                // 2. 用 RSA 私钥解密得到 AES 密钥（Base64 格式）
                                String aesKey = CryptoUtils.rsaDecrypt(encryptedAesKey, rsaPrivateKey);

                                // 3. 提取加密载荷和 IV
                                String encryptedPayload = jsonNode.get("payload").asText();
                                String iv = jsonNode.has("iv") ? jsonNode.get("iv").asText() : null;

                                if (iv == null || iv.isEmpty()) {
                                    return Flux.error(new IllegalArgumentException("请求中缺少 IV 参数"));
                                }

                                // 4. AES 解密出真实明文
                                String plainText = CryptoUtils.aesDecrypt(encryptedPayload, aesKey, iv);

                                byte[] decryptedBytes = plainText.getBytes(StandardCharsets.UTF_8);
                                return Flux.just(exchange.getResponse().bufferFactory().wrap(decryptedBytes));
                            } catch (Exception e) {
                                return Flux.error(new RuntimeException("请求解密失败", e));
                            }
                        });
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                // 因为解密后 Content-Length 会变，所以必须移除它，让微服务重新计算
                headers.remove(HttpHeaders.CONTENT_LENGTH);
                // 设置内容类型为JSON
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
            }
        };
    }

    /**
     * 纯 AES 模式解密请求体。
     * <p>
     * 前端请求格式：{@code {payload, iv}}，使用配置的固定 secretKey 解密。
     * </p>
     *
     * @param exchange 当前交换上下文
     * @param request  原始请求
     * @param secret   AES 密钥（Base64）
     * @return 解密后的请求装饰器
     */
    private ServerHttpRequest decryptRequest(ServerWebExchange exchange, ServerHttpRequest request, String secret) {
        return new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<DataBuffer> getBody() {
                return DataBufferUtils.join(super.getBody())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("请求体为空")))
                        .flatMapMany((DataBuffer joined) -> {
                            try {
                                byte[] content = new byte[joined.readableByteCount()];
                                joined.read(content);
                                DataBufferUtils.release(joined);

                                String encryptedBodyJson = new String(content, StandardCharsets.UTF_8);
                                JsonNode jsonNode = OBJECT_MAPPER.readTree(encryptedBodyJson);

                                String encryptedPayload = jsonNode.get("payload").asText();
                                String iv = jsonNode.has("iv") ? jsonNode.get("iv").asText() : null;

                                if (iv == null || iv.isEmpty()) {
                                    return Flux.error(new IllegalArgumentException("请求中缺少IV参数"));
                                }

                                String plainText = CryptoUtils.aesDecrypt(encryptedPayload, secret, iv);
                                byte[] decryptedBytes = plainText.getBytes(StandardCharsets.UTF_8);
                                return Flux.just(exchange.getResponse().bufferFactory().wrap(decryptedBytes));
                            } catch (Exception e) {
                                return Flux.error(new RuntimeException("请求解密失败", e));
                            }
                        });
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                // 因为解密后 Content-Length 会变，所以必须移除它，让微服务重新计算，或者配置分块传输
                headers.remove(HttpHeaders.CONTENT_LENGTH);
                // 设置内容类型为JSON
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
            }
        };
    }

    /**
     * 提取请求参数用于签名验证
     */
    private Map<String, String> extractRequestParams(ServerHttpRequest request) {
        Map<String, String> params = new HashMap<>();
        // 这里可以根据实际情况提取查询参数或其他参数
        request.getQueryParams().forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                params.put(key, values.getFirst());
            }
        });
        return params;
    }

    /**
     * 判断是否为文件操作请求（上传或下载）
     *
     * @param request 请求对象
     * @return true-是文件操作，false-不是文件操作
     */
    private boolean isFileOperation(ServerHttpRequest request) {
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType == null) {
            return false;
        }

        // 文件上传：multipart/form-data
        if (contentType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
            return true;
        }

        // 文件下载或二进制流：application/octet-stream
        if (contentType.startsWith(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
            return true;
        }

        // 其他常见的文件类型（可根据需要扩展）
        return contentType.startsWith("application/pdf") ||
                contentType.startsWith("image/") ||
                contentType.startsWith("video/") ||
                contentType.startsWith("audio/");
    }

}
