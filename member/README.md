# Mobi App 微服务

Mobi 会员/店铺管理系统的后端微服务集群，基于 Spring Cloud Alibaba 构建。小程序 C 端、管理后台与实时通知分别由独立服务承载，统一经 API 网关对外暴露。

| 属性 | 值 |
|------|-----|
| GroupId | `com.xcz.member` |
| ArtifactId | `mobi` |
| Version | `0.0.1-SNAPSHOT` |
| Java | 21 |
| Spring Boot | 3.2.0 |
| Nacos Group | `MINI_APP_MOBI` |

## 启动模块

| 模块 | Nacos 服务名 | 端口 | 说明 | 文档 |
|------|-------------|------|------|------|
| [app-gateway](./app-gateway/README.md) | `app-gateway` | 30000 | API 网关、加解密、限流、验证码 | 统一入口 |
| [app-user](./app-user/README.md) | `app-admin` | 30001 | 管理后台 RBAC + 业务 Feign 代理 | 目录名 app-user |
| [app-customer](./app-customer/README.md) | `app-miniApp` | 30002 | 微信小程序全业务 | 目录名 app-customer |
| [app-notify](./app-notify/README.md) | `app-notify` | 30003 | WebSocket 实时推送 | 工单消息 |

> `app-feign` 为内部 Feign 客户端库，非启动模块，无独立文档。

## 架构

```
                    ┌─────────────┐
  管理后台 ─────────►│ app-gateway │ :30000
  微信小程序 ───────►│             │
                    └──────┬──────┘
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    /mobi/dashboard   /mobi/**      /mobi/notify
           │               │               │
           ▼               ▼               ▼
     app-admin        app-miniApp     app-notify
      :30001            :30002          :30003
     RBAC+Feign        全业务实现       WebSocket
           │               ▲
           └───── Feign ───┘
                    │
              Redis Pub/Sub
         ticket:message:topic
```

## 网关路由约定

路由定义在 Nacos `app-gateway.yaml`（仓库内无静态路由文件）：

| 网关路径 | 目标服务 | 说明 |
|----------|----------|------|
| `/mobi/dashboard/**` | `lb://app-admin` | 管理后台 API |
| `/mobi/notify/**` | `lb://app-notify` | WebSocket 推送 |
| `/mobi/**` | `lb://app-miniApp` | 小程序业务 API |

网关本地路由：

| 路径 | 说明 |
|------|------|
| `GET /mobi/dashboard/code` | 图形验证码 |

## 配置加载

各服务本地 `application.yml` 仅保留端口、应用名与 Nacos 连接信息，业务配置从 Nacos 远程加载：

```yaml
spring:
  config:
    import:
      - optional:nacos:${spring.application.name}.yaml
  cloud:
    nacos:
      config:
        group: MINI_APP_MOBI
        namespace: public
        file-extension: yaml
```

Nacos 配置文件名与服务名对应：`app-gateway.yaml`、`app-admin.yaml`、`app-miniApp.yaml`、`app-notify.yaml`。

## 公共依赖

基于 [commons](../../commons/README.md) 公共模块：

| 模块 | 使用方 |
|------|--------|
| commons-log | gateway、user、customer |
| commons-redis | gateway、customer、notify |
| commons-security | user、customer、notify |
| commons-database | user、customer |
| commons-oss | customer |

## 快速启动

### 前置依赖

- JDK 21、Maven 3.9+
- Nacos（配置中心 + 服务注册）
- MySQL、Redis
- 阿里云 OSS（店铺证照、商品图片）
- 微信小程序 / 微信支付配置（customer 服务）

### 启动顺序

```bash
cd member

# 1. 网关
mvn -pl app-gateway spring-boot:run

# 2. 管理后台
mvn -pl app-user spring-boot:run

# 3. 小程序业务
mvn -pl app-customer spring-boot:run

# 4. 消息推送
mvn -pl app-notify spring-boot:run
```

或分别在各模块目录执行 `mvn spring-boot:run`。

### 构建

```bash
mvn clean package -DskipTests
```

## 前端客户端

| 项目 | 说明 | 文档 |
|------|------|------|
| [admin](../admin/README.md) | 管理后台 Vue 3 | 网关前缀 `/mobi/dashboard` |
| [app](../app/README.md) | 微信小程序 uni-app | 网关前缀 `/mobi` |

## 子模块文档

- [app-gateway](./app-gateway/README.md)
- [app-user](./app-user/README.md)
- [app-customer](./app-customer/README.md)
- [app-notify](./app-notify/README.md)
