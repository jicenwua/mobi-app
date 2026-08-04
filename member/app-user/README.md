# app-user

管理后台服务。负责后台账号 RBAC（用户/角色/菜单/权限）、操作日志，以及店铺、商品、积分、优惠券、工单等业务的 **Feign 权限代理层**——权限在本服务校验，实际业务逻辑在 [app-customer](../app-customer/README.md) 执行。

| 属性 | 值 |
|------|-----|
| Maven 模块名 | `app-user` |
| Nacos 服务名 | `app-admin` |
| 端口 | 30001 |
| 启动类 | `com.xcz.member.AppUserApplication` |
| 网关前缀 | `/mobi/dashboard` |

[← 返回总览](../README.md)

## 职责

- 后台用户登录/登出（JWT + Redis 会话）
- 用户、角色、菜单、权限 CRUD
- 操作日志记录（`@OperateLog` AOP）
- 通过 Feign 代理管理端对店铺、商品、活动、优惠券、积分、工单、小程序用户/角色的操作

## 包结构

```
com.xcz.member.user
├── controller/       # REST API（RBAC + Feign 代理）
├── service/          # 登录、用户、角色、菜单、日志
├── service/admin/    # 各业务 Feign 代理服务
├── service/impl/
├── mapper/ + xml/    # MyBatis Mapper
├── domain/           # SysUser, SysRole, SysMenu, SysLog 等
├── aspect/           # OperateLogAspect
└── annotation/       # @OperateLog
```

## 配置

### 本地 `application.yml`

```yaml
server:
  port: 30001

spring:
  application:
    name: app-admin
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 120MB
  config:
    import:
      - optional:nacos:${spring.application.name}.yaml
```

### Nacos `app-admin.yaml` 主要配置项

| 前缀 | 说明 |
|------|------|
| `spring.datasource.*` | MySQL + Druid |
| `redis.enabled` + `redis.single.*` | Redis |
| `security.jwt.*` | JWT 密钥与过期时间 |
| `security.ignore.urls` | 额外白名单 |

## 数据库表（本服务自有）

| 表 | 说明 |
|----|------|
| `sys_user` | 后台用户 |
| `sys_role` | 角色（含 version 字段） |
| `sys_menu` | 菜单权限 |
| `sys_user_role` | 用户-角色关联 |
| `sys_role_menu` | 角色-菜单关联 |
| `sys_log` | 操作/登录日志 |

## Redis（commons-security，DATABASE_0）

| 键/Topic | 用途 |
|----------|------|
| `login:body:{token}` | 登录会话 |
| `login:refresh` | Token 续期集合 |
| `sys:role:permission` | 角色权限集 |
| `sys:role:user:{userId}` | 用户角色变更标记 |
| `version:topic` | 权限版本 Pub/Sub |

## API 接口

网关完整路径 = `/mobi/dashboard` + 下表路径。权限通过 `@PreAuthorize("@ss.hasPermi('...')")` 控制。

### 认证 `/system/auth`

| 方法 | 路径 | 权限 |
|------|------|------|
| POST | `/login` | 公开（可配验证码） |
| POST | `/logout` | 需登录 |

### 用户 `/system/user`

| 方法 | 路径 | 权限 |
|------|------|------|
| POST | `/list` | `system:user:list` |
| GET | `/info/{userId}` | `system:user:query` |
| GET | `/info` | 已认证 |
| PUT | `/` | `system:user:edit` |
| POST | `/` | `system:user:add` |
| GET | `/` | `system:user:remove` |

### 角色 `/system/role`

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/list` | `system:role:list` |
| GET | `/{roleId}` | `system:role:query` |
| POST | `/` | `system:role:add` |
| PUT | `/` | `system:role:edit` |
| DELETE | `/{roleIds}` | `system:role:remove` |
| PUT | `/menus` | `system:role:assign` |

### 菜单 `/system/menu`

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/tree` | `system:menu:list` |
| GET | `/admin/list` | `system:menu:list` |
| GET | `/user/{userId}` | `system:menu:query` |
| GET | `/{menuId}` | `system:menu:query` |
| POST | `/` | `system:menu:add` |
| PUT | `/` | `system:menu:edit` |
| DELETE | `/{menuIds}` | `system:menu:remove` |

### 日志 `/system/log`

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/list` | `system:log:list` |
| GET | `/{logId}` | `system:log:query` |
| DELETE | `/{logIds}` | `system:log:remove` |

### 店铺 `/shop`（Feign → app-miniApp）

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/list` | `system:shop:list` |
| GET | `/audit/list` | `system:shop:audit:list` |
| GET | `/info/{id}` | `system:shop:query` |
| POST | `/add-full` | `system:shop:add` |
| PUT | `/audit` | `system:shop:audit` |
| GET | `/statistics` | 无显式权限注解 |

### 活动 `/shop/activity`

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/list` | `system:shop:activity:list` |
| GET | `/{id}` | `system:shop:activity:query` |
| POST | `/` | `system:shop:activity:add` 或 `edit` |
| DELETE | `/{id}` | `system:shop:activity:remove` |
| PUT | `/{id}/stop` | `system:shop:activity:edit` |
| PUT | `/{id}/enable` | `system:shop:activity:edit` |

### 优惠券 `/shop/coupon`

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/list` | `system:shop:coupon:list` |
| GET | `/{templateId}` | `system:shop:coupon:query` |
| POST | `/` | `system:shop:coupon:add` |
| PUT | `/` | `system:shop:coupon:edit` |
| DELETE | `/{templateId}` | `system:shop:coupon:remove` |
| PUT | `/{templateId}/stop` | `system:shop:coupon:edit` |
| PUT | `/{templateId}/resume` | `system:shop:coupon:edit` |

### 商品 `/product`

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/list` | `system:product:list` |
| PUT | `/status` | `system:product:list` |

### 积分 `/points`

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/account/list` | `system:user:points:list` |
| POST | `/account/adjust` | `system:user:points:adjust` |
| GET | `/log/list` | `system:user:consume:list` |
| GET | `/log/{logId}` | `system:user:consume:query` |

### 用户优惠券 `/user/coupon`

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/list` | `system:user:coupon:list` |
| POST | `/grant` | `system:user:coupon:grant` |
| DELETE | `/{userCouponId}` | `system:user:coupon:remove` |

### 工单 `/ticket`

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/unread-summary` | `system:ticket:list` |
| GET | `/list` | `system:ticket:list` |
| GET | `/mine` | `system:ticket:mine` |
| GET | `/{ticketId}` | `system:ticket:query` |
| POST | `/{ticketId}/claim` | `system:ticket:claim` |
| POST | `/{ticketId}/reply` | `system:ticket:reply` |
| POST | `/{ticketId}/complete` | `system:ticket:complete` |

### 小程序用户 `/user/mobi-user`

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/list` | `system:mobi:user:list` |
| GET | `/{userId}` | `system:mobi:user:query` |
| PUT | `/` | `system:mobi:user:edit` |

### 小程序角色 `/system/mobi-role`

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/list` | `system:mobi:role:list` |
| GET | `/{id}` | `system:mobi:role:query` |
| POST | `/` | `system:mobi:role:add` |
| PUT | `/` | `system:mobi:role:edit` |
| DELETE | `/{ids}` | `system:mobi:role:remove` |
| POST | `/reload-cache` | `system:mobi:role:edit` |

## 依赖

| 依赖 | 用途 |
|------|------|
| commons-database | MyBatis-Plus + Druid |
| commons-security | JWT 认证、权限校验 |
| commons-log | 请求日志 |
| app-feign | OpenFeign 调用 app-miniApp |
| Nacos Discovery + Config | 服务注册与配置 |

## 构建与运行

```bash
mvn spring-boot:run
```

## 相关模块

- [app-customer](../app-customer/README.md) — Feign 调用的业务实现服务
- [app-gateway](../app-gateway/README.md) — 网关入口
