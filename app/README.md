# app

Mobi 微信小程序前端，基于 uni-app 构建。通过网关 `/mobi` 前缀访问 [app-customer](../member/app-customer/README.md) 服务，支持请求 RSA+AES 加解密与 WebSocket 工单实时通知。

| 属性 | 值 |
|------|-----|
| 框架 | uni-app（Vue 3） |
| 目标平台 | 微信小程序 |
| API 前缀 | `/mobi` |
| 网关地址 | 见 `config/env.js` |

[← 返回项目总览](../README.md)

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | uni-app + Vue 3 |
| 加密 | crypto-js + jsencrypt（网关 RSA+AES） |
| 二维码 | @uqrcode/js |
| UI 组件 | uni-ui 模块 |

## 目录结构

```
app/
├── api/
│   ├── http/client.js       # 统一 HTTP 客户端（加密、鉴权、去重）
│   └── modules/             # 业务 API 模块
│       ├── auth.js          # 微信登录
│       ├── user.js          # 用户资料
│       ├── shop.js          # 店铺浏览
│       ├── shop-manage.js   # 店铺管理
│       ├── points.js        # 积分/订单
│       ├── coupon.js        # 优惠券
│       ├── qrcode.js        # 二维码
│       └── ticket.js        # 工单
├── components/              # 业务组件（店铺、支付、商品等）
├── composables/             # 组合式逻辑
├── config/env.js            # 网关地址与加密配置
├── pages/                   # 页面
├── services/                # 认证重登、权限同步
├── static/                  # 静态资源
└── utils/                   # 加密、通知 Socket、扫码等
```

## 页面路由

路由定义在 `pages.json`，分主包与分包：

### 主包

| 路径 | 页面 | 说明 |
|------|------|------|
| `pages/login/login` | 登录 | 微信授权登录 |
| `pages/login/profile-setup` | 完善资料 | 新用户引导 |
| `pages/main/main` | 主页 | Tab 容器（会员/店铺） |
| `pages/mine/profile` | 个人资料 | — |
| `pages/mine/set-password` | 设置支付密码 | — |
| `pages/mine/coupons` | 我的折扣券 | — |
| `pages/mine/tickets` | 客服工单列表 | — |
| `pages/mine/ticket-create` | 新建工单 | — |
| `pages/mine/ticket-detail` | 工单详情 | — |

### 分包 `pages/shop`（店铺管理）

| 路径 | 页面 | 说明 |
|------|------|------|
| `pages/shop/add` | 添加店铺 | 开店申请 |
| `pages/shop/detail` | 店铺详情 | — |
| `pages/shop/manage` | 店铺管理 | 商品/活动/券入口 |
| `pages/shop/product-detail` | 商品详情 | — |
| `pages/shop/activity-edit` | 编辑活动 | 满赠活动 |
| `pages/shop/records` | 顾客信息 | — |
| `pages/shop/staff` | 店员管理 | — |
| `pages/shop/statistics` | 店铺统计 | — |
| `pages/shop/scan-checkout` | 扫码收银 | 扣积分/核销 |

### 分包 `pages/member`（会员消费）

| 路径 | 页面 | 说明 |
|------|------|------|
| `pages/member/shop-detail` | 店铺详情 | 顾客视角 |
| `pages/member/purchase-confirm` | 确认订单 | — |
| `pages/member/purchase-success` | 购买成功 | — |
| `pages/member/order-detail` | 订单详情 | — |
| `pages/member/pay-qrcode` | 付款码 | — |

主包 `pages/main/main` 预加载 `pages/member` 与 `pages/shop` 分包。

## API 请求

统一通过 `api/http/client.js` 发起请求：

```javascript
// 拼 URL：GATEWAY_BASE_URL + GW_SERVICE_PREFIX.customer + path
// 例：http://127.0.0.1:30000/mobi/app/login
```

### 网关配置（`config/env.js`）

| 变量 | 说明 |
|------|------|
| `GATEWAY_BASE_URL` | 网关根地址（上线需配 HTTPS 合法域名） |
| `GW_SERVICE_PREFIX.customer` | `/mobi` |
| `GW_FORBIDDEN_ADMIN_PATH_PREFIX` | `/mobi/dashboard`（小程序禁止访问） |
| `REQUEST_CRYPTO_ENABLED` | 是否加密 POST/PUT 请求体 |
| `GATEWAY_CRYPTO_MODE` | `rsa`（混合）或 `aes`（纯对称） |
| `GATEWAY_RSA_PUBLIC_KEY_BASE64` | RSA 公钥 |

客户端会拦截对 `/mobi/dashboard` 的请求，防止误调管理端接口。

### API 模块与后端对应

| 模块文件 | 后端 Controller |
|----------|----------------|
| `auth.js` / `user.js` | `MobiUserController`（`/app`） |
| `shop.js` | `MobiShopController`（`/shop`） |
| `shop-manage.js` | 店铺管理相关接口 |
| `points.js` | `MobiPointsController`（`/points`） |
| `coupon.js` | `MobiCouponController`（`/coupon`） |
| `qrcode.js` | `MobiQrcodeController`（`/qrcode`） |
| `ticket.js` | `MobiTicketController`（`/ticket`） |

## 认证与会话

- 微信 `wx.login` 获取 code → 调用 `/mobi/app/login`
- Token 由 `api/modules/auth-token.js` 管理
- 401 响应触发 `services/auth-relogin.js` 重新登录
- 权限变更后通过响应头同步（`syncPermissionsAfterTokenRefresh`）

## WebSocket 工单通知

`utils/notify-socket.js` 连接：

```
ws://{GATEWAY_BASE_URL}/mobi/notify/ws?token=...&client=customer
```

配合 `composables/use-ticket-notify.js` 在工单详情页接收实时消息。

## 开发

### 依赖安装

```bash
npm install
```

### 微信开发者工具

1. 用 HBuilderX 或 CLI 编译到 `unpackage/dist/dev/mp-weixin`
2. 微信开发者工具导入该目录
3. 确保 [app-gateway](../member/app-gateway/README.md)（30000）和 [app-customer](../member/app-customer/README.md)（30002）已启动
4. 修改 `config/env.js` 中的 `GATEWAY_BASE_URL` 为可访问的网关地址
5. 微信公众平台配置 request / socket 合法域名

### 辅助脚本

```bash
npm run fix:wechat        # 同步微信项目配置
npm run compress:static   # 压缩静态图片
```

## 相关文档

- [member/app-customer](../member/app-customer/README.md) — 后端 API 服务
- [member/app-notify](../member/app-notify/README.md) — WebSocket 推送服务
- [member/app-gateway](../member/app-gateway/README.md) — 网关（加解密）
