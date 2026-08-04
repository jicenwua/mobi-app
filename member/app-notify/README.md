# app-notify

实时消息推送服务，基于 WebSocket 长连接。订阅 Redis 工单事件并推送给在线的小程序用户与后台客服。

| 属性 | 值 |
|------|-----|
| Nacos 服务名 | `app-notify` |
| 端口 | 30003 |
| 启动类 | `com.xcz.member.AppNotifyApplication` |
| 网关前缀 | `/mobi/notify` |

[← 返回总览](../README.md)

## 职责

- 维护 WebSocket 长连接会话
- 订阅 Redis `ticket:message:topic` 工单事件
- 按用户类型（客服 / 顾客）精准推送消息
- JWT 握手鉴权，限制每用户最大连接数

本服务无 REST Controller，仅提供 WebSocket 端点。

## WebSocket 端点

| 网关地址 | 服务内路径 |
|----------|-----------|
| `ws://host:30000/mobi/notify/ws` | `/notify/ws` |

### 连接参数

| 参数 | 必填 | 说明 |
|------|------|------|
| `token` | 是 | JWT Token（`TokenService` 校验 Redis 会话） |
| `client` | 是 | `staff`（后台客服）或 `customer`（小程序用户） |

### 握手规则

1. `TokenService.resolveSession(token)` 校验 JWT 与 Redis 会话
2. `client` 必须与用户类型匹配
3. 每用户最多 **3** 个并发连接（`notify.ws.max-connections-per-user`）

## 推送事件

| 事件类型 | 说明 |
|----------|------|
| `ticket_message` | 工单聊天新消息 |
| `unread_changed` | 未读角标刷新 |
| `status_changed` | 工单状态变更 |

### 推送目标策略

| 场景 | 推送对象 |
|------|----------|
| 客服回复工单 | 对应 C 端用户 |
| 用户回复已认领工单 | 认领该工单的客服 |
| 新建 / 未认领工单 | 全部在线客服 |

事件由 [app-customer](../app-customer/README.md) 通过 Redis Pub/Sub 发布。

## 包结构

```
com.xcz.member.notify
├── config/           # WebSocketConfig, NotifyWebSocketProperties
├── websocket/        # Handler, Interceptor, SessionRegistry, ClientType
├── listener/         # TicketMessageNotifyListener
└── service/          # NotifyPushService
```

## 配置

### 本地 `application.yml`

```yaml
server:
  port: 30003

spring:
  application:
    name: app-notify
  config:
    import:
      - optional:nacos:${spring.application.name}.yaml

security:
  ignore:
    urls:
      - /notify/ws
  internal-service:
    token: mobi-feign

notify:
  ws:
    allowed-origins:
      - http://localhost:*
      - https://localhost:*
    max-connections-per-user: 3
```

### Nacos `app-notify.yaml` 主要配置项

| 前缀 | 说明 |
|------|------|
| `redis.enabled` + `redis.single.*` | Redis（订阅工单 Topic） |
| `security.jwt.*` | JWT 校验 |
| `notify.ws.*` | WebSocket 跨域、连接数限制 |

## Redis

| 键/Topic | DB | 说明 |
|----------|-----|------|
| `ticket:message:topic` | DATABASE_0 | 订阅工单事件（app-customer 发布） |

## 依赖

| 依赖 | 用途 |
|------|------|
| commons-redis | Redisson 订阅 Pub/Sub |
| commons-security | JWT 握手鉴权 |
| spring-boot-starter-web | Web 容器 |
| spring-boot-starter-websocket | WebSocket 支持 |
| Nacos Discovery + Config | 服务注册与配置 |

## 数据库

无。本服务不直接访问数据库。

## 构建与运行

```bash
mvn spring-boot:run
```

## 相关模块

- [app-customer](../app-customer/README.md) — 工单事件发布方
- [app-gateway](../app-gateway/README.md) — WebSocket 网关转发
