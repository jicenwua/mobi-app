"use strict";
const CUSTOMER_API = {
  /** 微信登录 POST multipart：code + nickName + avatar 文件 */
  LOGIN: "/app/login",
  /** 当前登录用户信息 GET（需 Bearer token） */
  USER_INFO: "/app/info",
  /** 更新用户资料 POST */
  UPDATE: "/app/update",
  /** 设置/修改支付密码 POST */
  PASSWORD: "/app/password",
  /** 校验支付密码 POST */
  PASSWORD_VERIFY: "/app/password/verify",
  /** 店铺分页列表 GET：pageNum、pageSize（按最后进入时间倒序，含剩余积分） */
  SHOP_LIST: "/shop",
  /** 按店铺代码预览店铺 GET /shop/enter/{shopCode} */
  SHOP_ENTER_INFO: "/shop/enter",
  /** 加入店铺 POST：shopId */
  SHOP_ENTER: "/shop/enter",
  /** 店长扫码添加店员 POST：shopId + token */
  SHOP_STAFF_ADD: "/shop/staff",
  /** 店长查看店员列表 GET：shopId */
  SHOP_STAFF_LIST: "/shop/staff/list",
  /** 店长移除店员 DELETE：shopId + userId */
  SHOP_STAFF_REMOVE: "/shop/staff",
  /** 记录进入店铺时间 GET：shopId（用于刷新店铺列表排序） */
  SHOP_ENTER_TIME: "/shop/enter/time",
  /** 店铺统计数据 GET：shopId + filterShopId */
  SHOP_STATISTICS: "/shop/statistics",
  /** 新增店铺（multipart）POST */
  SHOP_ADD_FULL: "/shop/add-full",
  /** 店员：店铺会员积分列表 */
  POINTS_SHOP_ACCOUNT_LIST: "/points/shop/account/list",
  /** 店员：店铺消费记录列表（不含商品明细） */
  POINTS_SHOP_LOG_LIST: "/points/shop/log/list",
  /** 消费记录详情 GET：logId + shopId */
  POINTS_LOG_DETAIL: "/points/log",
  /** 当前会员在店铺的消费记录 GET：shopId + pageNum + pageSize */
  POINTS_MY_LOG_LIST: "/points/my/log/list",
  /** 顾客领取优惠券 POST：shopId + templateId */
  COUPON_CLAIM: "/coupon/claim",
  /** 店铺商品目录 GET：shopId */
  PRODUCT_CATALOG: "/product/catalog",
  /** 商品维护：新增 POST multipart；更新 PUT multipart；库存 PUT /product/stock；删除 DELETE /product/{productId} */
  PRODUCT: "/product",
  /** 商品分类 GET/POST/PUT /product/category；删除 DELETE /product/category/{categoryId} */
  PRODUCT_CATEGORY: "/product/category",
  /** 折扣券追加库存 PUT /coupon/template/{templateId}/stock */
  COUPON_TEMPLATE_STOCK: "/coupon/template",
  /** 活动列表 GET /activity/list；详情与维护 /activity/{activityId} */
  ACTIVITY_LIST: "/activity/list",
  ACTIVITY: "/activity",
  /** 顾客：进行中满赠活动 GET */
  ACTIVITY_ONGOING: "/activity/ongoing",
  /** 顾客：最新公告 GET */
  ACTIVITY_ANNOUNCEMENT_LATEST: "/activity/announcement/latest",
  /** 折扣券模板 GET /coupon/template/list；维护 /coupon/template/{templateId} */
  COUPON_TEMPLATE_LIST: "/coupon/template/list",
  COUPON_TEMPLATE: "/coupon/template",
  /** 顾客：可领取优惠券 GET */
  COUPON_DISTRIBUTING: "/coupon/distributing",
  /** 顾客：我的优惠券 GET */
  COUPON_MY: "/coupon/my",
  /** 店铺邀请小程序码 GET：/qrcode/shop/{shopId} */
  SHOP_QRCODE: "/qrcode/shop",
  /** 解析店铺邀请 token GET：token */
  SHOP_INVITE_RESOLVE: "/qrcode/shop/invite",
  /** 会员统一付款码 GET（不区分店铺） */
  PAY_QRCODE_GENERATE: "/qrcode/pay/generate",
  /** 店员校验付款码 GET：shopId + token */
  PAY_QRCODE_VERIFY: "/qrcode/pay/verify",
  /** 用户店员邀请码 GET */
  STAFF_INVITE_QRCODE_GENERATE: "/qrcode/staff/invite/generate",
  /** 会员订单核销码 GET：logId + shopId */
  ORDER_QRCODE_GENERATE: "/qrcode/order/generate",
  /** 店员扫码核销订单 POST */
  ORDER_VERIFY: "/points/order/verify",
  /** 会员自助退款待使用订单 POST */
  ORDER_REFUND: "/points/order/refund",
  /** 店员扫码扣积分 POST */
  POINTS_CONSUME: "/points/consume",
  /** 店员/店长线下充值积分 POST */
  POINTS_RECHARGE: "/points/recharge",
  /** 会员自助购买 POST */
  POINTS_PURCHASE: "/points/purchase",
  /** 会员全部店铺订单 GET：status + pageNum + pageSize */
  POINTS_MY_ORDER_LIST: "/points/my/order/list",
  /** 客服工单 GET /ticket/my；创建 POST /ticket；详情 GET /ticket/{id}；回复 POST /ticket/{id}/message */
  TICKET_MY: "/ticket/my",
  TICKET_UNREAD_COUNT: "/ticket/unread-count",
  TICKET: "/ticket"
};
const WX_PERM = {
  /** 店主店铺 Tab/列表/详情/商品与积分管理 */
  SHOP_LIST: "wx:shop:list",
  /** 添加店铺、开店申请 */
  SHOP_ADD: "wx:shop:add",
  /** 会员 Tab、店内详情、顾客读接口 */
  USER: "wx:user",
  /** 搜索/预览店铺、店铺邀请码 */
  USER_QUERY: "wx:user:query",
  /** 加入店铺 */
  USER_ENTER: "wx:user:enter",
  /** 活动/优惠券模板查询（店长） */
  ACTIVITY_QUERY: "wx:activity:query",
  /** 活动/优惠券创建与修改（店长） */
  ACTIVITY_EDIT: "wx:activity:edit",
  /** 活动/优惠券删除（店长） */
  ACTIVITY_REMOVE: "wx:activity:remove",
  /** 扫码收银、校验付款码 */
  SHOP_CONSUME: "wx:shop:consume",
  /** 会员付款码、校验支付密码 */
  PAY_QRCODE: "wx:pay:qrcode"
};
exports.CUSTOMER_API = CUSTOMER_API;
exports.WX_PERM = WX_PERM;
