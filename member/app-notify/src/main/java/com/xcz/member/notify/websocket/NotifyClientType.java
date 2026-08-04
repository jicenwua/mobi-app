package com.xcz.member.notify.websocket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 客户端类型，用于区分后台客服与 C 端用户。
 * <p>
 * 同一 userId 在后台与小程序可能数值相同，因此会话注册时使用 {@code clientType:userId} 作为键。
 * 前端握手时需传 query 参数 {@code client=staff} 或 {@code client=customer}。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum NotifyClientType {

    /** 后台管理端（Admin） */
    STAFF("staff"),

    /** 小程序 C 端用户 */
    CUSTOMER("customer");

    /** 与前端约定的 client 参数值 */
    private final String code;

    /**
     * 根据 query 参数解析客户端类型
     *
     * @param code 前端传入的 client 值
     * @return 匹配的枚举；无效时返回 null
     */
    public static NotifyClientType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (NotifyClientType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
