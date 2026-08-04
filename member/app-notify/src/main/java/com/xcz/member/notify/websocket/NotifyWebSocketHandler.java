package com.xcz.member.notify.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 连接生命周期与消息处理器。
 * <p>
 * 仅处理文本帧：客户端发送 {@code ping} 时回复 {@code pong} 保活；
 * 业务推送由 {@link com.xcz.member.notify.service.NotifyPushService} 主动下发 JSON。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyWebSocketHandler extends TextWebSocketHandler {

    private final NotifySessionRegistry sessionRegistry;

    /**
     * 连接建立成功后，将会话写入注册表
     *
     * @param session 已升级的 WebSocket 连接
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        NotifyClientType clientType = (NotifyClientType) session.getAttributes().get("clientType");
        // 握手拦截器未写入属性时拒绝连接（理论上不应出现）
        if (userId == null || clientType == null) {
            closeQuietly(session);
            return;
        }
        sessionRegistry.register(session, userId, clientType);
        log.info("WebSocket 已连接 client={} userId={}", clientType.getCode(), userId);
    }

    /**
     * 连接正常关闭时注销会话
     *
     * @param session 关闭的连接
     * @param status  关闭状态码
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
        log.debug("WebSocket 已断开 status={}", status);
    }

    /**
     * 处理客户端上行文本消息（当前仅支持心跳 ping）
     *
     * @param session 来源连接
     * @param message 文本帧
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            sendQuietly(session, "pong");
        }
    }

    /**
     * 传输层异常时清理会话，避免注册表残留
     *
     * @param session   异常连接
     * @param exception 异常信息
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessionRegistry.unregister(session);
        closeQuietly(session);
    }

    /**
     * 向指定连接发送文本消息，失败时静默忽略（避免推送线程因单连接异常中断）
     *
     * @param session WebSocket 连接
     * @param payload JSON 字符串或 pong
     */
    public static void sendQuietly(WebSocketSession session, String payload) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            // WebSocketSession 非线程安全，同一连接并发 send 需加锁
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (Exception e) {
            log.debug("WebSocket 发送失败", e);
        }
    }

    /**
     * 静默关闭连接
     */
    private static void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        } catch (Exception ignored) {
            // 连接可能已被对端关闭
        }
    }
}
