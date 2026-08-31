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
        registry.addHandler(notifyWebSocketHandler, NotifyConstants.WS_PATH)    //校验请求是否合法的请求地址，不合法则直接拒绝连接
                .addInterceptors(notifyHandshakeInterceptor)                //第一次连接首先进行三次握手拦截（实际就是连接请求校验）
                .setAllowedOriginPatterns(allowedOrigins);      //校验websocket请求是合法的来源（这里是只发送请求的网页等实际挂在的服务器的地址或者域名）
    }
}
