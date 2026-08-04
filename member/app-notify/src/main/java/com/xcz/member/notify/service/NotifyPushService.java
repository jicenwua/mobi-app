package com.xcz.member.notify.service;

import com.alibaba.fastjson2.JSON;
import com.xcz.commons.core.event.TicketMessageNotifyEvent;
import com.xcz.member.notify.websocket.NotifyClientType;
import com.xcz.member.notify.websocket.NotifySessionRegistry;
import com.xcz.member.notify.websocket.NotifyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 工单实时推送服务。
 * <p>
 * 根据 Redis 事件中的推送目标，在本节点查找在线 WebSocket 连接并下发消息。
 * 每次新消息会连续推送两条：{@code ticket_message}（聊天内容）与 {@code unread_changed}（红点刷新）。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class NotifyPushService {

    private final NotifySessionRegistry sessionRegistry;

    /**
     * 将工单 Redis 事件推送给本节点在线的目标用户
     *
     * @param event app-customer 发布的工单通知事件
     */
    public void pushTicketEvent(TicketMessageNotifyEvent event) {
        if (event == null) {
            return;
        }
        Set<WebSocketSession> targets = resolveTargets(event);
        if (targets.isEmpty()) {
            return;
        }

        String payload = JSON.toJSONString(event);
        for (WebSocketSession session : targets) {
            NotifyWebSocketHandler.sendQuietly(session, payload);
        }

        // 新消息事件额外推送未读变更信号，供侧边栏/角标刷新
        if (!TicketMessageNotifyEvent.TYPE_UNREAD_CHANGED.equals(event.getType())
                && !TicketMessageNotifyEvent.TYPE_STATUS_CHANGED.equals(event.getType())) {
            TicketMessageNotifyEvent unreadEvent = TicketMessageNotifyEvent.builder()
                    .type(TicketMessageNotifyEvent.TYPE_UNREAD_CHANGED)
                    .ticketId(event.getTicketId())
                    .build();
            String unreadPayload = JSON.toJSONString(unreadEvent);
            for (WebSocketSession session : targets) {
                NotifyWebSocketHandler.sendQuietly(session, unreadPayload);
            }
        }
    }

    /**
     * 根据事件中的 target 字段解析本节点应推送的 WebSocket 连接集合
     *
     * @param event 工单通知事件
     * @return 去重后的目标会话；可能为空（用户不在线）
     */
    private Set<WebSocketSession> resolveTargets(TicketMessageNotifyEvent event) {
        Set<WebSocketSession> targets = new LinkedHashSet<>();
        // 推送给 C 端用户（客服回复时）
        if (event.getTargetCustomerUserId() != null) {
            targets.addAll(sessionRegistry.sessionsFor(
                    NotifyClientType.CUSTOMER, event.getTargetCustomerUserId()));
        }
        // 推送给指定认领客服（用户回复已认领工单时）
        if (event.getTargetStaffId() != null) {
            targets.addAll(sessionRegistry.sessionsFor(
                    NotifyClientType.STAFF, event.getTargetStaffId()));
        }
        // 推送给全部在线客服（用户新建/回复未认领工单时）
        if (event.isNotifyAllStaff()) {
            targets.addAll(sessionRegistry.allStaffSessions());
        }
        return targets;
    }
}
