# app-customer

微信小程序核心业务服务，承载店铺、商品、积分、优惠券、活动、工单、二维码等全部 C 端与管理端 Feign 入口。采用 DDD 分层架构，高并发库存与券领取通过 Redis 原子操作实现。

| 属性 | 值 |
|------|-----|
| Maven 模块名 | `app-customer` |
| Nacos 服务名 | `app-miniApp` |
| 端口 | 30002 |
| 启动类 | `com.xcz.member.AppCustomerApplication` |
| 网关前缀 | `/mobi` |

[← 返回总览](../README.md)

## 职责

- 微信登录、用户资料、支付密码
- 店铺开店（证照 OCR + OSS 上传）、审核、成员管理
- 商品目录与 Redis 高并发库存扣减
- 积分充值/消费/购商品/核销/退款
- 满赠活动管理
- 优惠券 Redis 原子领取
- 客服工单 + Redis 实时事件广播
- 店铺/付款/订单/店员邀请二维码
- 管理端 Feign 内部 API（`security.internal-service.token` 保护）

## 包结构

```
com.xcz.member.customer
├── controller/           # C 端 REST API
├── controller/feign/     # 管理端 Feign 入口
├── api/dto/              # request / response DTO
├── application/
│   ├── service/          # 应用服务（读写分离）
│   ├── command/          # 命令对象
│   └── assemblers/       # DTO 转换
├── domain/               # 领域模型、枚举、领域服务接口
├── infrastructure/
│   ├── entity/ + mapper/ # MyBatis 持久化
│   ├── service/          # 领域服务实现
│   ├── cache/            # Redis 缓存（库存、券、活动等）
│   ├── adapter/          # 微信、OCR 适配
│   ├── notify/           # Redis 工单广播
│   └── task/             # 定时同步任务
└── utils/                # 店铺权限、邀请码、付款码等
```

## 配置

### 本地 `application.yml`

```yaml
server:
  port: 30002

spring:
  application:
    name: app-miniApp
  config:
    import:
      - optional:nacos:${spring.application.name}.yaml

wx:
  env-version: develop    # develop | trial | release
  check-path: false       # 本地/体验环境建议 false
```

### Nacos `app-miniApp.yaml` 主要配置项

| 前缀 | 说明 |
|------|------|
| `spring.datasource.*` | MySQL + Druid |
| `redis.enabled` + `redis.single.*` | Redis |
| `security.jwt.*` | JWT 配置 |
| `security.internal-service.token` | Feign 内部调用令牌 |
| `aliyun.oss.*` | 店铺证照、商品图片 |
| `wx.miniapp.*` | 微信小程序配置 |
| `wx.pay.*` | 微信支付配置 |

## 数据库表

| 表 | 说明 |
|----|------|
| `mobi_shop` | 店铺 |
| `mobi_user` | 小程序用户 |
| `mobi_shop_user` | 店铺成员关系 |
| `mobi_role` | 小程序角色定义 |
| `mobi_shop_product` | 商品 |
| `mobi_shop_product_category` | 商品分类 |
| `mobi_points_account` | 积分账户 |
| `mobi_points_log` | 积分/消费流水 |
| `mobi_coupon_template` | 优惠券模板 |
| `mobi_user_coupons` | 用户持券 |
| `mobi_activity` | 活动 |
| `mobi_activity_rule` | 满赠规则 |
| `mobi_ticket` | 客服工单 |
| `mobi_ticket_message` | 工单消息 |
| `mobi_receipt` | 收据 |

## Redis

主要使用 DATABASE_1（业务缓存）：

| 键模式 | 说明 |
|--------|------|
| `user:session:{openId}` | 微信 session_key |
| `user:info` | 用户资料 MapCache |
| `shop:info` / `shop:code` | 店铺简要信息/代码索引 |
| `shop:enter:{userId}` | 用户进店时间 ZSet |
| `shop:role:{userId}` | 用户在店铺的角色缓存 |
| `product:catalog:{shopId}` | 商品目录 |
| `product:stock:{productId}` | 实时库存 |
| `product:sold:pending:{productId}` | Redis 侧累计销量 |
| `product:sold:dirty` | 待回写 DB 的商品 ID 集合 |
| `coupon:stock:` / `coupon:info:` / `coupon:claimed:` | 券库存与领取状态 |
| `activity:shop-ids:{shopId}` / `activity:detail` | 活动缓存 |
| `points:idempotent:{requestId}` | 积分购买幂等 |
| `pay:token:` / `order:token:` / `shop:invite:token:` / `staff:invite:token:` | 短时二维码 token |

DATABASE_0：

| 键/Topic | 说明 |
|----------|------|
| `ticket:message:topic` | 工单消息 Pub/Sub（→ app-notify） |

## 定时任务

| 任务 | 间隔 | 功能 |
|------|------|------|
| `CouponStockSyncTask` | 5s | Redis 券库存 → DB |
| `ProductSoldSyncTask` | 5min | Redis 销量/库存 → DB |
| `ShopEnterTimeSyncTask` | 5min | Redis 进店时间 → DB |

## C 端 API

网关完整路径 = `/mobi` + 下表路径。多数接口在 Service 层通过 `ShopAccessUtils` 校验店长/店员/顾客权限。

### 用户 `/app`

| 方法 | 路径 | 权限 |
|------|------|------|
| POST | `/login` | 公开（微信 code 登录） |
| POST | `/update` | 需登录 |
| POST | `/password` | 需登录 |
| POST | `/password/verify` | 需登录 |
| GET | `/info` | 需登录 |

### 店铺 `/shop`

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/` | 需登录 |
| POST | `/` | `wx:shop:add` |
| POST | `/enter` | `wx:user:enter` |
| POST | `/staff` | `wx:shop:list` |
| GET | `/staff/list` | `wx:shop:list` |
| DELETE | `/staff` | `wx:shop:list` |
| GET | `/enter/{shopCode}` | `wx:user:query` |
| GET | `/enter/time` | 需登录 |
| GET | `/statistics` | `wx:shop:list` |

### 商品 `/product`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/catalog` | 商品目录（需店铺成员） |
| GET | `/category` | 分类列表 |
| POST | `/` | 新增商品（店长/店员） |
| PUT | `/` | 更新商品 |
| PUT | `/stock` | 调整库存 |
| DELETE | `/{productId}` | 删除商品 |
| PUT | `/{productId}/status` | 上下架 |
| POST/PUT/DELETE | `/category/**` | 分类管理 |

### 积分 `/points`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/shop/account/list` | 店员查会员积分 |
| GET | `/shop/log/list` | 店员查店铺流水 |
| GET | `/my/log/list` | 顾客查本人流水 |
| GET | `/log/{logId}` | 流水详情 |
| POST | `/consume` | 店员扫码扣积分 |
| POST | `/recharge` | 店员/店长充值 |
| POST | `/purchase` | 会员自助购商品 |
| GET | `/my/order/list` | 顾客订单列表 |
| POST | `/order/verify` | 店员核销订单 |
| POST | `/order/refund` | 会员退款 |

### 优惠券 `/coupon`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/distributing` | 顾客可领券列表 |
| GET | `/template/list` | 店员券模板列表 |
| GET | `/my` | 我的优惠券 |
| POST | `/claim` | 领券 |
| POST/PUT/DELETE | `/template/**` | 店长券管理 |

### 活动 `/activity`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ongoing` | 进行中满赠活动 |
| GET | `/announcement/latest` | 最新公告 |
| GET | `/list` | 店员活动列表 |
| GET | `/{activityId}` | 活动详情 |
| POST/PUT/DELETE | `/**` | 店长活动管理 |

### 工单 `/ticket`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/unread-count` | 未读数 |
| GET | `/my` | 我的工单 |
| POST | `/` | 创建工单 |
| GET | `/{ticketId}` | 详情 |
| POST | `/{ticketId}/message` | 用户回复 |

### 二维码 `/qrcode`

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/shop/{shopId}` | `wx:user:query` |
| GET | `/shop/invite` | `wx:user:query` |
| GET | `/pay/generate` | 需登录 |
| GET | `/pay/verify` | 店员校验付款码 |
| GET | `/order/generate` | 订单核销码 |
| GET | `/staff/invite/generate` | 店员邀请码 |

### 微信支付测试 `/app/pay`（条件装配）

| 方法 | 路径 | 权限 |
|------|------|------|
| POST | `/test/**` | 需登录 |
| POST | `/notify/order` | `@Release` 公开回调 |
| POST | `/notify/refund` | `@Release` 公开回调 |

## Feign 内部 API

由 `security.internal-service.token` 保护，供 [app-user](../app-user/README.md) 通过 Feign 调用：

| Controller | 前缀 | 对应 app-user 代理 |
|-----------|------|-------------------|
| `SysMobiShopController` | `/shop/sys` | `SysShopController` |
| `SysMobiActivityController` | `/shop/sys/activity` | `SysShopActivityController` |
| `SysMobiShopCouponController` | `/shop/sys/coupon` | `SysShopCouponController` |
| `SysMobiPointsController` | `/points/sys` | `SysPointsController` |
| `SysMobiUserCouponController` | `/user-coupon/sys` | `SysUserCouponController` |
| `SysMobiTicketController` | `/ticket/sys` | `SysTicketController` |
| `SysMobiUserController` | `/mobi/user/sys` | `SysMobiUserController` |
| `SysMobiRoleController` | `/mobi/role` | `SysMobiRoleController` |
| `SysMobiProductController` | `/product` | `SysProductController` |

## 依赖

| 依赖 | 用途 |
|------|------|
| commons-oss | 证照、商品图片上传 |
| commons-database | MyBatis-Plus |
| commons-redis | Redisson 缓存与 Pub/Sub |
| commons-security | JWT、权限 |
| commons-log | 请求日志 |
| app-feign | Feign 客户端定义 |
| weixin-java-miniapp / weixin-java-pay | 微信 SDK |
| ocr_api20210707 | 阿里云身份证 OCR |
| Nacos Discovery + Config | 服务注册与配置 |

## 构建与运行

```bash
mvn spring-boot:run
```

## 相关模块

- [app-user](../app-user/README.md) — 管理端 Feign 调用方
- [app-notify](../app-notify/README.md) — 工单消息 WebSocket 推送
- [app-gateway](../app-gateway/README.md) — 网关入口
