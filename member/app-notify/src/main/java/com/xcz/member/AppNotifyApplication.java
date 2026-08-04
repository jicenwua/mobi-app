package com.xcz.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 实时消息推送服务启动类。
 * <p>
 * 职责：维护 WebSocket 长连接，订阅 Redis 工单事件并推送给在线客户端。
 * 前端统一经 Gateway 连接 {@code /mobi/notify/ws}，由 Gateway 转发至本服务。
 * </p>
 */
@SpringBootApplication
public class AppNotifyApplication {

    /**
     * 应用入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AppNotifyApplication.class, args);
    }
}
