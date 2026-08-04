# admin

Mobi 管理后台前端，基于 Vue 3 + Vite + Element Plus。通过网关 `/mobi/dashboard` 前缀访问 [app-user](../member/app-user/README.md) 服务，支持动态菜单路由、RBAC 权限指令与请求加解密。

| 属性 | 值 |
|------|-----|
| 开发端口 | 5700 |
| 构建工具 | Vite 8 |
| UI 框架 | Element Plus |
| API 前缀 | `/mobi/dashboard` |

[← 返回项目总览](../README.md)

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Vue 3.5（Composition API） |
| 构建 | Vite 8 |
| 路由 | Vue Router 5（常量路由 + 动态菜单路由） |
| 状态 | Pinia 3 |
| UI | Element Plus + Icons |
| HTTP | Axios |
| 图表 | ECharts 6 |
| 加密 | crypto-js + jsencrypt（网关 RSA+AES） |
| 实时通知 | WebSocket（`notifySocket.js` → app-notify） |

## 目录结构

```
src/
├── api/                  # 后端 API 封装
│   ├── dashboard/        # 统计面板
│   ├── system/           # 用户、角色、菜单、日志、小程序角色
│   ├── shop/             # 店铺、活动、优惠券
│   ├── product/          # 商品
│   ├── user/             # 积分、消费、优惠券、小程序用户
│   └── ticket/           # 工单
├── components/           # 公共组件（分页、字典标签等）
├── directives/           # 权限指令 v-permission
├── hooks/                # 组合式函数（表格、确认、聊天滚动等）
├── layout/               # 布局（侧栏、导航栏、标签页）
├── router/               # 常量路由
├── store/modules/        # Pinia（user、permission、theme、ticket）
├── utils/                # 请求、加密、会话、WebSocket 等
└── views/                # 页面视图
    ├── dashboard/        # 首页统计
    ├── system/           # 系统管理
    ├── shop/             # 店铺管理
    ├── product/          # 商品管理
    ├── user/             # 用户运营
    └── ticket/           # 工单管理
```

## 路由

### 常量路由（无需后端菜单）

| 路径 | 页面 | 说明 |
|------|------|------|
| `/login` | 登录 | 图形验证码 + 账号密码 |
| `/dashboard` | 首页统计 | 默认首页 |
| `/shop/manage/detail/:id` | 店铺详情 | 隐藏路由 |
| `/shop/manage/add` | 添加店铺 | 隐藏路由 |
| `/shop/manage/edit/:id` | 编辑店铺 | 隐藏路由 |
| `/ticket/info/:id` | 工单详情（只读） | 隐藏路由 |
| `/ticket/handle/:id` | 工单处理（对话） | 隐藏路由 |
| `/404` | 404 页面 | — |

### 动态路由（后端菜单驱动）

登录后调用 `GET /system/user/info` 获取菜单树，由 `permissionStore.generateRoutes()` 动态注册。菜单 `component` 字段映射到 `views/` 下对应 `.vue` 文件。

主要业务页面模块：

| 模块 | 视图目录 | 功能 |
|------|----------|------|
| 系统管理 | `views/system/user` | 后台用户 |
| | `views/system/role` | 角色与菜单分配 |
| | `views/system/menu` | 菜单 CRUD |
| | `views/system/log` | 操作日志 |
| | `views/system/mobiRole` | 小程序角色 |
| 店铺 | `views/shop/management` | 店铺列表/表单 |
| | `views/shop/audit` | 店铺审核 |
| | `views/shop/activity` | 活动管理 |
| | `views/shop/coupon` | 优惠券模板 |
| 商品 | `views/product/management` | 商品上下架 |
| 用户运营 | `views/user/points` | 积分账户 |
| | `views/user/consume` | 消费流水 |
| | `views/user/coupon` | 用户优惠券 |
| | `views/user/mobiUser` | 小程序用户 |
| 工单 | `views/ticket/list` | 全部工单 |
| | `views/ticket/mine` | 我的工单 |

## 权限控制

### 路由守卫（`permission.js`）

1. 无 token → 跳转 `/login`
2. 有 token → 拉取用户信息与菜单，动态注册路由
3. 401 或业务 code 401 → 清除会话，跳转登录

### 按钮权限

`v-permission` 指令读取 `permissionStore.permissions`，匹配后端返回的权限标识（如 `system:user:add`）。

### 会话存储

| 存储 | 内容 |
|------|------|
| Token | `authToken.js` 管理 |
| 用户资料 | 昵称、头像、账号、菜单（白名单字段持久化） |
| 权限标识 | 仅内存，不落 localStorage |

## API 请求

```javascript
// utils/request.js
baseURL: import.meta.env.VITE_APP_BASE_API + '/mobi/dashboard'
```

请求自动携带 `authorization` 头，支持 RSA+AES 混合加密（与网关 `CryptoGlobalFilter` 对齐）。

### 环境变量

| 文件 | 关键变量 | 说明 |
|------|----------|------|
| `.env.development` | `VITE_APP_BASE_API=/dev-api` | Vite 代理到网关 `localhost:30000` |
| `.env.production` | `VITE_APP_BASE_API=/prod-api` | Nginx 反代 |
| | `VITE_APP_ENCRYPTION_ENABLED` | 是否开启请求加密 |
| | `VITE_APP_ENCRYPTION_RSA_MODE` | RSA+AES 混合 / 纯 AES |
| | `VITE_APP_ENCRYPTION_RSA_PUBLIC_KEY` | RSA 公钥 |

开发代理（`vite.config.js`）：

```javascript
proxy: {
  '/dev-api': {
    target: 'http://localhost:30000',
    changeOrigin: true,
    ws: true,  // WebSocket 工单通知
    rewrite: (path) => path.replace(/^\/dev-api/, '')
  }
}
```

## WebSocket 工单通知

`utils/notifySocket.js` 连接网关 WebSocket：

```
ws://host:30000/mobi/notify/ws?token=...&client=staff
```

收到 `ticket_message`、`unread_changed`、`status_changed` 事件后更新工单角标与聊天界面。

## 开发

```bash
npm install
npm run dev      # http://localhost:5700
npm run build
npm run preview
```

需确保 [app-gateway](../member/app-gateway/README.md)（30000）和 [app-user](../member/app-user/README.md)（30001）已启动。

## 相关文档

- [member/app-user](../member/app-user/README.md) — 后端 API 服务
- [member/app-notify](../member/app-notify/README.md) — WebSocket 推送服务
