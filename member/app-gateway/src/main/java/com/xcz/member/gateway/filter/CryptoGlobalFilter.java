package com.xcz.member.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.commons.core.constant.SecurityHeaderConstants;
import com.xcz.commons.core.utils.CryptoUtils;
import com.xcz.commons.core.utils.RequestSignatureUtils;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.gateway.config.properties.EncryptionProperties;
import jakarta.annotation.Resource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import java.util.List;
import java.util.Map;

/**
 * 请求解密过滤器：对带 {@code X-Encrypted-Body} 的请求解密或协商会话密钥后转发。
 * 混合加密的时候，是
 * 响应加密由 {@link CryptoResponseEncryptFilter} 复用同一 AES 会话密钥处理。
 */
@Component
@Order(-800)
public class CryptoGlobalFilter implements GlobalFilter {
    @Resource
    private EncryptionProperties encryptionProperties;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /** AES 纯对称模式下，请求时间戳允许的最大偏差（毫秒） */
    private static final long TIMESTAMP_THRESHOLD_MS = 60_000; // 60秒

    /**
     * 网关入口：按加密开关、请求方法与请求头决定是否解密或协商会话密钥。
     */
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

        //判断是否跳过的验证
        List<String> urls = encryptionProperties.getUrls();
        String path = request.getURI().getPath();
        if(!urls.isEmpty() && StringUtils.matches(path, urls)) {
            return chain.filter(exchange);
        }


        //获取前端是否进行加密
        String encryptedBodyHeader = request.getHeaders().getFirst(SecurityHeaderConstants.ENCRYPTED_BODY);
        if (encryptedBodyHeader == null) {
            // 未携带加密标识，按普通请求转发
            return chain.filter(exchange);
        }
        //如果是GET请求，获取前端的加密秘钥
        HttpMethod method = request.getMethod();
        if (HttpMethod.GET.equals(method)) {
            return handleGetSessionKey(exchange, request, chain);
        }
        // 仅对 POST/PUT 请求且包含加密头的请求进行拦截
        if (!HttpMethod.POST.equals(method) && !HttpMethod.PUT.equals(method)) {
            return chain.filter(exchange);
        }

        try {
            // RSA+AES 混合模式：RSA 解密 AES 密钥后再解密 body
            if (encryptionProperties.isRsaMode()) {
                return handleRsaAesMode(exchange, request, chain);
            }
            // 纯 AES 模式：验签后解密 body
            return handleAesMode(exchange, request, chain);
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * GET 请求：从请求头协商 AES 会话密钥（RSA 模式），供响应加密复用。
     */
    private Mono<Void> handleGetSessionKey(ServerWebExchange exchange, ServerHttpRequest request, GatewayFilterChain chain) {
        try {
            String secretKey = encryptionProperties.getDecryptionKey();
            if (secretKey == null || secretKey.isEmpty()) {
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                return exchange.getResponse().setComplete();
            }
            //判断使用的是什么模式
            if (encryptionProperties.isRsaMode()) {
                //获取前端加密的秘钥（RSA 密文，放在请求头 X-Encrypted-Key）
                String encryptedKey = request.getHeaders().getFirst(SecurityHeaderConstants.ENCRYPTED_KEY_HEADER);
                if (encryptedKey == null || encryptedKey.isEmpty()) {
                    exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                    return exchange.getResponse().setComplete();
                }
                //解密秘钥并重新保存回请求头
                String aesKey = CryptoUtils.rsaDecrypt(encryptedKey, secretKey);
                // 实际存入 exchange 属性（非请求头），供 CryptoResponseEncryptFilter 加密响应
                exchange.getAttributes().put(SecurityHeaderConstants.SESSION_AES_KEY_ATTR, aesKey);
                ServerHttpRequest stripped = stripCryptoHeaders(request);
                return chain.filter(exchange.mutate().request(stripped).build());
            }

            // AES 模式：验签通过后使用配置中的固定密钥作为会话密钥
            if (validateAesModeSecureHeaders(exchange, request)) {
                return exchange.getResponse().setComplete();
            }
            exchange.getAttributes().put(SecurityHeaderConstants.SESSION_AES_KEY_ATTR, secretKey);
            ServerHttpRequest stripped = stripCryptoHeaders(request);
            return chain.filter(exchange.mutate().request(stripped).build());
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * RSA+AES 模式处理 POST/PUT：从请求头取密钥与 IV，body 仅为密文。
     */
    private Mono<Void> handleRsaAesMode(ServerWebExchange exchange, ServerHttpRequest request, GatewayFilterChain chain) {
        try {
            String rsaPrivateKey = encryptionProperties.getDecryptionKey();
            if (rsaPrivateKey == null || rsaPrivateKey.isEmpty()) {
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                return exchange.getResponse().setComplete();
            }

            ServerHttpRequest decoratedRequest = decryptRequestWithRsaAes(exchange, request, rsaPrivateKey);
            return chain.filter(exchange.mutate().request(decoratedRequest).build());
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * 纯 AES 模式处理 POST/PUT：验签通过后解密请求体并转发。
     */
    private Mono<Void> handleAesMode(ServerWebExchange exchange, ServerHttpRequest request, GatewayFilterChain chain) {
        try {
            // 验签或防重放失败时，validate 已设置响应状态码
            if (validateAesModeSecureHeaders(exchange, request)) {
                return exchange.getResponse().setComplete();
            }

            String secret = encryptionProperties.getSecretKey();
            // 固定密钥同时作为本次请求的会话密钥，供响应加密复用
            exchange.getAttributes().put(SecurityHeaderConstants.SESSION_AES_KEY_ATTR, secret);

            ServerHttpRequest decoratedRequest = decryptRequest(exchange, request, secret);
            return chain.filter(exchange.mutate().request(decoratedRequest).build());
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * 纯 AES 模式安全校验：校验时间戳、防重放与请求签名。
     *
     * @param exchange 当前交换上下文（校验失败时写入 HTTP 状态码）
     * @param request  当前请求
     * @return true 表示校验失败（应终止请求）；false 表示校验通过
     */
    private boolean validateAesModeSecureHeaders(ServerWebExchange exchange, ServerHttpRequest request) {
        //获取验证参数
        String timestampStr = request.getHeaders().getFirst(SecurityHeaderConstants.TIMESTAMP);
        String nonce = request.getHeaders().getFirst(SecurityHeaderConstants.NONCE);
        String signature = request.getHeaders().getFirst(SecurityHeaderConstants.SIGNATURE);
        if (timestampStr == null || nonce == null || signature == null) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return true;
        }
        //验证请求是否过期
        long timestamp = Long.parseLong(timestampStr);
        if (System.currentTimeMillis() - timestamp > TIMESTAMP_THRESHOLD_MS) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return true;
        }

        // 使用配置中的 AES 密钥参与签名校验（与前端约定同一 secret）
        String secret = encryptionProperties.getSecretKey();
        // 提取 URL query 参数，与前端签名时使用的参数集保持一致
        Map<String, String> params = extractRequestParams(request);
        // 按 query + timestamp + nonce + secret 重新计算 SHA-256，与客户端 X-Signature 比对
        boolean isSignatureValid = RequestSignatureUtils.verifySignature(params, timestampStr, nonce, signature, secret);
        if (!isSignatureValid) {
            // 签名不一致，拒绝请求（防篡改）
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return true;
        }
        return false;
    }

    /**
     * RSA+AES 模式：请求头携带 RSA 密文密钥与 IV，body 仅为 AES 密文。
     *
     * @param rsaPrivateKey 网关 RSA 私钥（PKCS8 Base64）
     */
    private ServerHttpRequest decryptRequestWithRsaAes(ServerWebExchange exchange, ServerHttpRequest request, String rsaPrivateKey) {
        String encryptedAesKey = request.getHeaders().getFirst(SecurityHeaderConstants.ENCRYPTED_KEY_HEADER);
        String iv = request.getHeaders().getFirst(SecurityHeaderConstants.INITIALIZATION_VECTOR);
        if (encryptedAesKey == null || encryptedAesKey.isEmpty()) {
            throw new IllegalArgumentException("请求头缺少 X-Encrypted-Key");
        }
        if (iv == null || iv.isEmpty()) {
            throw new IllegalArgumentException("请求头缺少 X-IV");
        }

        return new ServerHttpRequestDecorator(stripCryptoHeaders(request)) {
            @Override
            public Flux<DataBuffer> getBody() {
                // 合并分片 body，避免 JSON 被截断
                return DataBufferUtils.join(super.getBody())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("请求体为空")))
                        .flatMapMany((DataBuffer joined) -> {
                            try {
                                byte[] content = new byte[joined.readableByteCount()];
                                joined.read(content);
                                DataBufferUtils.release(joined);

                                String requestBody = new String(content, StandardCharsets.UTF_8);
                                // 1. RSA 解密请求头中的 AES 会话密钥
                                String aesKey = CryptoUtils.rsaDecrypt(encryptedAesKey, rsaPrivateKey);
                                exchange.getAttributes().put(SecurityHeaderConstants.SESSION_AES_KEY_ATTR, aesKey);

                                // 2. body 仅为密文（兼容旧版 {"payload":"..."} 包装）
                                String encryptedPayload = extractEncryptedPayload(requestBody);
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
                // 解密后长度变化，移除原 Content-Length
                headers.remove(HttpHeaders.CONTENT_LENGTH);
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
            }
        };
    }

    /**
     * 纯 AES 模式：请求头携带 IV，body 仅为密文，密钥取自配置。
     *
     * @param secret 配置的 AES 密钥（Base64）
     */
    private ServerHttpRequest decryptRequest(ServerWebExchange exchange, ServerHttpRequest request, String secret) {
        String iv = request.getHeaders().getFirst(SecurityHeaderConstants.INITIALIZATION_VECTOR);
        if (iv == null || iv.isEmpty()) {
            throw new IllegalArgumentException("请求头缺少 X-IV");
        }

        return new ServerHttpRequestDecorator(stripCryptoHeaders(request)) {
            @Override
            public Flux<DataBuffer> getBody() {
                return DataBufferUtils.join(super.getBody())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("请求体为空")))
                        .flatMapMany((DataBuffer joined) -> {
                            try {
                                byte[] content = new byte[joined.readableByteCount()];
                                joined.read(content);
                                DataBufferUtils.release(joined);

                                String requestBody = new String(content, StandardCharsets.UTF_8);
                                String encryptedPayload = extractEncryptedPayload(requestBody);
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
                headers.remove(HttpHeaders.CONTENT_LENGTH);
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
            }
        };
    }

    /**
     * 从请求体提取 AES 密文：支持纯 Base64 字符串或兼容旧版 {@code {"payload":"..."}}。
     */
    private String extractEncryptedPayload(String requestBody) throws Exception {
        String trimmed = requestBody.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("请求体为空");
        }
        if (trimmed.startsWith("{")) {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(trimmed);
            if (jsonNode.has("payload")) {
                return jsonNode.get("payload").asText();
            }
        }
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return OBJECT_MAPPER.readValue(trimmed, String.class);
        }
        return trimmed;
    }

    /**
     * 转发下游前移除加解密相关请求头，避免密文密钥泄漏到业务服务。
     */
    private ServerHttpRequest stripCryptoHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(SecurityHeaderConstants.ENCRYPTED_BODY);
                    headers.remove(SecurityHeaderConstants.ENCRYPTED_KEY_HEADER);
                    headers.remove(SecurityHeaderConstants.INITIALIZATION_VECTOR);
                    headers.remove(SecurityHeaderConstants.TIMESTAMP);
                    headers.remove(SecurityHeaderConstants.NONCE);
                    headers.remove(SecurityHeaderConstants.SIGNATURE);
                })
                .build();
    }

    /**
     * 获取请求地址参数数据
     * @param request   请求
     * @return  参数对应map
     */
    private Map<String, String> extractRequestParams(ServerHttpRequest request) {
        Map<String, String> params = new HashMap<>();
        // 将 query 参数转为 Map，供签名校验使用
        request.getQueryParams().forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                params.put(key, values.getFirst());
            }
        });
        return params;
    }

    /**
     * 判断是否为文件类请求（上传/下载），此类请求跳过加解密。
     */
    private boolean isFileOperation(ServerHttpRequest request) {
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType == null) {
            return false;
        }

        // 表单文件上传
        if (contentType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
            return true;
        }

        // 二进制流下载
        if (contentType.startsWith(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
            return true;
        }

        // 常见文件 MIME 类型
        return contentType.startsWith("application/pdf") ||
                contentType.startsWith("image/") ||
                contentType.startsWith("video/") ||
                contentType.startsWith("audio/");
    }
}
