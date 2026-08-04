package com.xcz.member.notify.websocket;

import com.xcz.member.notify.config.NotifyWebSocketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 在线 WebSocket 会话注册表。
 * <p>
 * 按 {@code clientType:userId} 隔离 C 端与后台 ID 空间，支持同一用户多端同时在线。
 * 多实例部署时，各节点仅维护本机连接；跨节点推送依赖 Redis Pub/Sub 广播到所有 notify 实例。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifySessionRegistry {

    /** 会话属性：登录用户 ID */
    private static final String SESSION_USER_ID = "userId";

    /** 会话属性：客户端类型 */
    private static final String SESSION_CLIENT_TYPE = "clientType";

    /** 会话属性：连接建立时间戳（用于超出上限时淘汰最旧连接） */
    private static final String SESSION_CONNECTED_AT = "connectedAt";

    private final NotifyWebSocketProperties webSocketProperties;

    /** key = clientType:userId，value = 该用户在本节点的全部 WebSocket 连接 */
    private final ConcurrentMap<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    /**
     * 注册新建立的 WebSocket 会话
     *
     * @param session    已鉴权的 WebSocket 连接
     * @param userId     登录用户 ID
     * @param clientType 客户端类型（staff / customer）
     */
    public void register(WebSocketSession session, Long userId, NotifyClientType clientType) {
        if (session.getAttributes().get(SESSION_CONNECTED_AT) == null) {
            session.getAttributes().put(SESSION_CONNECTED_AT, System.currentTimeMillis());
        }
        Set<WebSocketSession> bucket = sessions.computeIfAbsent(
                key(clientType, userId), ignored -> new CopyOnWriteArraySet<>());
        int maxConnections = webSocketProperties.getMaxConnectionsPerUser();
        while (bucket.size() >= maxConnections) {
            evictOldest(bucket);
        }
        bucket.add(session);
    }

    /**
     * 连接关闭或异常时移除会话
     *
     * @param session 待注销的 WebSocket 连接
     */
    public void unregister(WebSocketSession session) {
        Long userId = getUserId(session);
        NotifyClientType clientType = getClientType(session);
        if (userId == null || clientType == null) {
            return;
        }
        Set<WebSocketSession> bucket = sessions.get(key(clientType, userId));
        if (bucket != null) {
            bucket.remove(session);
            // 该用户已无在线连接时清理 map 条目，避免内存泄漏
            if (bucket.isEmpty()) {
                sessions.remove(key(clientType, userId), bucket);
            }
        }
    }

    /**
     * 查询指定用户在当前节点的全部 WebSocket 连接
     *
     * @param clientType 客户端类型
     * @param userId     用户 ID
     * @return 在线会话集合；无连接时返回空集合
     */
    public Set<WebSocketSession> sessionsFor(NotifyClientType clientType, Long userId) {
        if (clientType == null || userId == null) {
            return Collections.emptySet();
        }
        Set<WebSocketSession> bucket = sessions.get(key(clientType, userId));
        return bucket == null ? Collections.emptySet() : bucket;
    }

    /**
     * 获取当前节点所有在线后台客服的 WebSocket 连接。
     * 用于「未认领工单」场景：用户发消息后需通知全部在线客服。
     *
     * @return 所有 staff 类型会话的并集
     */
    public Set<WebSocketSession> allStaffSessions() {
        Set<WebSocketSession> result = new CopyOnWriteArraySet<>();
        String prefix = NotifyClientType.STAFF.getCode() + ":";
        sessions.forEach((key, bucket) -> {
            if (key.startsWith(prefix)) {
                result.addAll(bucket);
            }
        });
        return result;
    }

    /**
     * 从会话属性读取用户 ID
     *
     * @param session WebSocket 连接
     * @return 用户 ID；未注册时返回 null
     */
    public static Long getUserId(WebSocketSession session) {
        Object value = session.getAttributes().get(SESSION_USER_ID);
        return value instanceof Long id ? id : null;
    }

    /**
     * 从会话属性读取客户端类型
     *
     * @param session WebSocket 连接
     * @return 客户端类型；未注册时返回 null
     */
    public static NotifyClientType getClientType(WebSocketSession session) {
        Object value = session.getAttributes().get(SESSION_CLIENT_TYPE);
        return value instanceof NotifyClientType type ? type : null;
    }

    private void evictOldest(Set<WebSocketSession> bucket) {
        WebSocketSession oldest = bucket.stream()
                .min(Comparator.comparingLong(this::connectedAt))
                .orElse(null);
        if (oldest == null) {
            return;
        }
        bucket.remove(oldest);
        closeQuietly(oldest);
        log.info("WebSocket 连接数超限，已关闭最旧连接 sessionId={}", oldest.getId());
    }

    private long connectedAt(WebSocketSession session) {
        Object value = session.getAttributes().get(SESSION_CONNECTED_AT);
        return value instanceof Long ts ? ts : 0L;
    }

    private static void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("连接数超出上限"));
        } catch (Exception ignored) {
            // 连接可能已被对端关闭
        }
    }

    /**
     * 生成会话注册表键：{@code staff:1001} 或 {@code customer:1001}
     */
    private static String key(NotifyClientType clientType, Long userId) {
        return clientType.getCode() + ":" + userId;
    }
}
