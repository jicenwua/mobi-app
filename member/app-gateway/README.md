# app-gateway

Mobi 微服务 API 网关，基于 Spring Cloud Gateway（WebFlux）。统一对外入口，负责路由转发、请求加解密、限流熔断、验证码与 XSS 防护。

| 属性 | 值 |
|------|-----|
| Nacos 服务名 | `app-gateway` |
| 端口 | 30000 |
| 启动类 | `com.xcz.member.AppGatewayApplication` |

[← 返回总览](../README.md)

## 职责

- 将 `/mobi/dashboard/**`、`/mobi/**`、`/mobi/notify/**` 路由到下游微服务
- RSA + AES 请求体加解密（可配置开关与忽略路径）
- IP/路径限流、黑名单拦截
- 登录图形验证码（`GET /mobi/dashboard/code`）
- Sentinel 熔断降级
- CORS 跨域

## 网关路由

路由在 Nacos `app-gateway.yaml` 中配置，约定如下：

| 网关 Path | 目标服务 | StripPrefix | 说明 |
|-----------|----------|-------------|------|
| `/mobi/dashboard/**` | `lb://app-admin` | 2 | 管理后台 |
| `/mobi/notify/**` | `lb://app-notify` | 1 | WebSocket |
| `/mobi/**` | `lb://app-miniApp` | 1 | 小程序 API |

网关内置路由（非 Nacos）：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/mobi/dashboard/code` | 图形验证码，免登录 |

## 全局过滤器

| 过滤器 | Order | 功能 |
|--------|-------|------|
| `SentinelFallbackHandler` | 最高 | Sentinel 熔断降级响应 |
| `RateLimiterFilter` | -900 | IP/路径限流、黑名单 |
| `CryptoGlobalFilter` | -800 | POST/PUT 请求体 RSA+AES 解密 |
| `CacheRequestFilter` | — | 缓存请求体供后续过滤器读取 |
| `ValidateCodeFilter` | -500 | 登录验证码校验 |
| `CryptoResponseEncryptFilter` | — | 响应体加密 |
| `XssFilter` | — | XSS 防护 |
| `HeaderSanitizeFilter` | — | 请求头清洗 |

## 包结构

```
com.xcz.member.gateway
├── config/              # CORS、验证码、Sentinel 配置
├── config/properties/   # 加密、限流、XSS、黑名单、验证码属性
├── filter/              # 全局过滤器
├── handler/             # 验证码、Sentinel 降级处理器
├── service/             # CaptchaService
└── util/                # SensitiveUriUtils
```

## 配置

### 本地 `application.yml`

```yaml
server:
  port: 30000

spring:
  application:
    name: app-gateway
  config:
    import:
      - optional:nacos:${spring.application.name}.yaml
```

### Nacos `app-gateway.yaml` 主要配置项

| 前缀 | 说明 |
|------|------|
| `spring.cloud.gateway.routes` | 下游服务路由规则 |
| `gateway.encryption.*` | 加解密开关、RSA/AES 密钥、忽略路径 |
| `gateway.captcha.*` | 验证码开关、匹配路径 |
| `gateway.rate-limiter.*` | 限流规则 |
| `gateway.black-request.*` | IP 黑名单 |
| `gateway.xss.*` | XSS 过滤规则 |
| `redis.enabled` + `redis.single.*` | Redis（验证码、限流） |

## Redis

使用 DATABASE_0：

| 键 | 用途 |
|----|------|
| `captcha:code` | 图形验证码 |
| 限流相关键 | `RateLimiterFilter` 计数 |

## 依赖

| 依赖 | 用途 |
|------|------|
| commons-redis | Redisson 客户端 |
| commons-log | 请求日志、traceId |
| spring-cloud-starter-gateway | 网关核心 |
| Nacos Discovery + Config | 服务发现与配置 |
| Sentinel + sentinel-gateway | 熔断限流 |
| spring-boot-starter-actuator | 健康检查 |
| kaptcha | 图形验证码 |

## 构建与运行

```bash
mvn spring-boot:run
# 或
java -jar target/app-gateway-0.0.1-SNAPSHOT.jar
```

## 相关模块

- [app-user](../app-user/README.md) — 管理后台（`/mobi/dashboard`）
- [app-customer](../app-customer/README.md) — 小程序业务（`/mobi`）
- [app-notify](../app-notify/README.md) — WebSocket（`/mobi/notify`）
