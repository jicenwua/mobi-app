"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("./common/vendor.js");
const utils_wxPrivacy = require("./utils/wx-privacy.js");
require("./utils/crypto-gateway.js");
const api_modules_authToken = require("./api/modules/auth-token.js");
const services_notifySocket = require("./services/notify-socket.js");
const utils_qrcodeScan = require("./utils/qrcode-scan.js");
const services_authRelogin = require("./services/auth-relogin.js");
const services_loginFlow = require("./services/login-flow.js");
const services_appSession = require("./services/app-session.js");
const api_modules_auth = require("./api/modules/auth.js");
if (!Math) {
  "./pages/login/login.js";
  "./pages/login/profile-setup.js";
  "./pages/main/main.js";
  "./pages/mine/profile.js";
  "./pages/mine/set-password.js";
  "./pages/mine/coupons.js";
  "./pages/mine/tickets.js";
  "./pages/mine/ticket-create.js";
  "./pages/mine/ticket-detail.js";
  "./pages/shop/add.js";
  "./pages/shop/detail.js";
  "./pages/shop/manage.js";
  "./pages/shop/product-detail.js";
  "./pages/shop/activity-edit.js";
  "./pages/shop/records.js";
  "./pages/shop/staff.js";
  "./pages/shop/statistics.js";
  "./pages/shop/scan-checkout.js";
  "./pages/member/shop-detail.js";
  "./pages/member/purchase-confirm.js";
  "./pages/member/purchase-success.js";
  "./pages/member/order-detail.js";
  "./pages/member/pay-qrcode.js";
}
function saveLaunchShopInvite(options) {
  const parsed = utils_qrcodeScan.parseLaunchShopInvite(options);
  if (parsed == null ? void 0 : parsed.token) {
    utils_qrcodeScan.savePendingShopInvite(parsed.token);
  } else if (parsed == null ? void 0 : parsed.shopCode) {
    utils_qrcodeScan.savePendingShopCode(parsed.shopCode);
  }
}
const _sfc_main = {
  onLaunch(options) {
    var _a, _b;
    utils_wxPrivacy.setupPrivacyAuthorization();
    saveLaunchShopInvite((options == null ? void 0 : options.query) || options);
    try {
      const launch = (_b = (_a = common_vendor.index).getLaunchOptionsSync) == null ? void 0 : _b.call(_a);
      saveLaunchShopInvite((launch == null ? void 0 : launch.query) || launch);
    } catch {
    }
  },
  onShow() {
    if (api_modules_authToken.getToken()) {
      services_notifySocket.connectNotifySocket();
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {};
}
const App = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
services_authRelogin.configureAuthRelogin({ performQuickLogin: services_loginFlow.performQuickLogin, resetAppSession: services_appSession.resetAppSession, fetchCurrentUserInfo: api_modules_auth.fetchCurrentUserInfo });
function createApp() {
  const app = common_vendor.createSSRApp(App);
  return { app };
}
createApp().app.mount("#app");
exports.createApp = createApp;
