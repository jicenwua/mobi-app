package com.xcz.member.notify.listener;

import com.alibaba.fastjson2.JSON;
import com.xcz.commons.core.constant.NotifyConstants;
import com.xcz.commons.core.event.TicketMessageNotifyEvent;
import com.xcz.member.notify.service.NotifyPushService;
import com.xcz.commons.redis.extend.DatabaseEnum;
import com.xcz.commons.redis.utils.RedisUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.springframework.stereotype.Component;

/**
 * 工单消息 Redis 事件监听器。
 * <p>
 * 订阅 {@link NotifyConstants#TICKET_MESSAGE_TOPIC}，接收 app-customer 广播的工单事件，
 * 并委托 {@link NotifyPushService} 推送到本节点在线 WebSocket 客户端。
 * 多实例部署时，每个 notify 节点各自订阅同一 Topic，仅推送给连在本机的用户。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketMessageNotifyListener {

    private final NotifyPushService notifyPushService;

    /**
     * 应用启动后注册 Redis Topic 监听
     */
    @PostConstruct
    public void subscribe() {
        RTopic topic = RedisUtil.getRedisson(DatabaseEnum.DATABASE_0)
                .getTopic(NotifyConstants.TICKET_MESSAGE_TOPIC);
        topic.addListener(String.class, (channel, message) -> {
            try {
                if (message == null || message.isBlank()) {
                    return;
                }
                TicketMessageNotifyEvent event = JSON.parseObject(message, TicketMessageNotifyEvent.class);
                notifyPushService.pushTicketEvent(event);
            } catch (Exception e) {
                log.error("处理工单通知事件失败", e);
            }
        });
        log.info("已订阅 Redis Topic: {}", NotifyConstants.TICKET_MESSAGE_TOPIC);
    }
}
