package com.xcz.member.notify.websocket;

import com.xcz.commons.core.utils.StringUtils;
import com.xcz.commons.security.extend.LoginUser;
import com.xcz.commons.security.service.TokenService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手鉴权拦截器。
 * <p>
 * 浏览器与小程序 WebSocket API 不便携带 Authorization 头，因此从 query 读取 token：
 * {@code /notify/ws?token=xxx&client=staff|customer}
 * </p>
 */
@Slf4j
@Component
public class NotifyHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private TokenService tokenService;

    /**
     * 握手前校验 token 并写入会话属性，供 {@link NotifyWebSocketHandler} 注册连接
     *
     * @param request    HTTP 升级请求
     * @param response   HTTP 响应
     * @param wsHandler  WebSocket 处理器
     * @param attributes 握手成功后传递给 WebSocketSession 的属性 Map
     * @return true 允许升级；false 拒绝连接
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = servletRequest.getServletRequest().getParameter("token");
        String client = servletRequest.getServletRequest().getParameter("client");
        if (StringUtils.isEmpty(token)) {
            log.debug("WebSocket 握手失败：缺少 token");
            return false;
        }
        NotifyClientType clientType = NotifyClientType.fromCode(client);
        if (clientType == null) {
            log.debug("WebSocket 握手失败：client 无效 {}", client);
            return false;
        }
        try {
            // 与 HTTP 接口共用 TokenService，支持 JWT 过期但 Redis 会话仍有效时的续签
            TokenService.AuthSession session = tokenService.resolveSession(token);
            LoginUser loginUser = session.loginUser();
            NotifyClientType expectedClientType = NotifyClientTypeResolver.resolveExpectedClientType(loginUser);
            if (expectedClientType == null) {
                log.debug("WebSocket 握手失败：无法判定客户端类型 userId={}", loginUser.getUserId());
                return false;
            }
            if (clientType != expectedClientType) {
                log.debug("WebSocket 握手失败：client={} 与权限不匹配，期望 {}", client, expectedClientType.getCode());
                return false;
            }
            attributes.put("userId", loginUser.getUserId());
            attributes.put("clientType", clientType);
            attributes.put("connectedAt", System.currentTimeMillis());
            return true;
        } catch (Exception e) {
            log.debug("WebSocket 握手失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 握手完成后的回调（当前无额外处理）
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 鉴权结果已在 beforeHandshake 决定，此处无需逻辑
    }
}
