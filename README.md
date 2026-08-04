# Mobi App

Mobi 会员/店铺管理系统，前后端分离、微服务架构。包含后端微服务集群、管理后台 Web 端与微信小程序客户端。

## 项目结构

```
mobi-app/
├── member/     # 后端微服务（Spring Boot）
├── admin/      # 管理后台（Vue 3）
└── app/        # 微信小程序（uni-app）
```

## 后端微服务

| 模块 | 端口 | 说明 | 文档 |
|------|------|------|------|
| app-gateway | 30000 | API 网关 | [README](./member/app-gateway/README.md) |
| app-user | 30001 | 管理后台 API（Nacos: `app-admin`） | [README](./member/app-user/README.md) |
| app-customer | 30002 | 小程序业务（Nacos: `app-miniApp`） | [README](./member/app-customer/README.md) |
| app-notify | 30003 | WebSocket 推送 | [README](./member/app-notify/README.md) |

详细架构与 API 见 [member/README.md](./member/README.md)。

## 前端

| 项目 | 技术 | 端口/平台 | 网关前缀 | 文档 |
|------|------|-----------|----------|------|
| [admin](./admin/README.md) | Vue 3 + Element Plus | 5700 | `/mobi/dashboard` | 管理后台 |
| [app](./app/README.md) | uni-app 微信小程序 | 微信开发者工具 | `/mobi` | 小程序 C 端 |

## 架构简图

```
  admin (Vue)  ──► /mobi/dashboard ──► app-admin (30001)
  app (小程序)  ──► /mobi          ──► app-miniApp (30002)
                    /mobi/notify/ws ──► app-notify (30003)
                           │
                    app-gateway (30000)
```

## 快速启动

```bash
# 1. 后端（在 member/ 下，需 Nacos / MySQL / Redis 等前置）
cd member
mvn -pl app-gateway spring-boot:run
mvn -pl app-user spring-boot:run
mvn -pl app-customer spring-boot:run
mvn -pl app-notify spring-boot:run

# 2. 管理后台
cd admin && npm install && npm run dev

# 3. 小程序：编译后用微信开发者工具打开 unpackage/dist/dev/mp-weixin
```

## 相关项目

- [commons](../commons/README.md) — 公共基础模块
- [blog](../blog/README.md) — 技术博客
