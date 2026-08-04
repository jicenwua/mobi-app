package com.xcz.member.notify.config;

import com.xcz.commons.core.constant.NotifyConstants;
import com.xcz.member.notify.websocket.NotifyHandshakeInterceptor;
import com.xcz.member.notify.websocket.NotifyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 端点注册配置。
 * <p>
 * 对外路径 {@link NotifyConstants#WS_PATH}，经 Gateway StripPrefix 后映射到本服务。
 * 握手阶段由 {@link NotifyHandshakeInterceptor} 完成 token 鉴权。
 * </p>
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotifyWebSocketHandler notifyWebSocketHandler;
    private final NotifyHandshakeInterceptor notifyHandshakeInterceptor;
    private final NotifyWebSocketProperties notifyWebSocketProperties;

    /**
     * 注册 WebSocket 处理器与拦截器
     *
     * @param registry Spring WebSocket 注册器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] allowedOrigins = notifyWebSocketProperties.getAllowedOrigins()
                .toArray(String[]::new);
        registry.addHandler(notifyWebSocketHandler, NotifyConstants.WS_PATH)
                .addInterceptors(notifyHandshakeInterceptor)
                .setAllowedOriginPatterns(allowedOrigins);
    }
}
