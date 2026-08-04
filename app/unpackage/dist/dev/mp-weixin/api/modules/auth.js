"use strict";
const common_vendor = require("../../common/vendor.js");
const api_http_client = require("../http/client.js");
const api_constants_customer = require("../constants/customer.js");
const utils_apiResponse = require("../../utils/api-response.js");
const utils_avatar = require("../../utils/avatar.js");
const api_modules_authToken = require("./auth-token.js");
const utils_cryptoGateway = require("../../utils/crypto-gateway.js");
const services_notifySocket = require("../../services/notify-socket.js");
const utils_permissions = require("../../utils/permissions.js");
const services_userSecurity = require("../../services/user-security.js");
const WX_LOGIN_TIMEOUT_MS = 15e3;
function applyAuthFromLoginData(login) {
  if (login.token) {
    api_modules_authToken.setToken(login.token);
  }
  utils_permissions.setSessionPermissions(login.permission);
  services_userSecurity.applyServerSetPasswordFlag(login.setPassword);
}
function buildLoginResult(ok, login, body) {
  const avatarUrl = utils_avatar.resolveAvatarUrl(login.avatar);
  return {
    ok,
    token: login.token || "",
    register: !!login.register,
    nickname: login.nickName,
    avatarUrl,
    setPassword: login.setPassword,
    usesDefaultAvatar: utils_avatar.isEmptyAvatar(login.avatar) || utils_avatar.isDefaultAvatarUrl(avatarUrl),
    msg: (body == null ? void 0 : body.msg) || (ok ? "登录成功" : "登录失败")
  };
}
function wxLoginCode() {
  return new Promise((resolve, reject) => {
    let settled = false;
    const finish = (fn, arg) => {
      if (settled)
        return;
      settled = true;
      clearTimeout(timer);
      fn(arg);
    };
    const timer = setTimeout(() => {
      finish(reject, new Error("uni.login 超时，请检查 AppID 与网络"));
    }, WX_LOGIN_TIMEOUT_MS);
    const handlers = {
      success: (res) => {
        if (res.code)
          finish(resolve, res.code);
        else
          finish(reject, new Error("未获取到微信 code"));
      },
      fail: (err) => finish(reject, new Error(err.errMsg || "uni.login 失败"))
    };
    common_vendor.index.login(handlers);
  });
}
function fetchCurrentUserInfo() {
  return new Promise((resolve) => {
    if (!api_modules_authToken.getToken()) {
      resolve({ ok: false, msg: "未登录" });
      return;
    }
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.USER_INFO,
      method: "GET",
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        if (utils_apiResponse.isUnauthorizedResponse(res, body)) {
          api_modules_authToken.clearAuthSession();
          services_notifySocket.disconnectNotifySocket();
          resolve({ ok: false, unauthorized: true, msg: body.msg || "登录已失效" });
          return;
        }
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        const login = utils_apiResponse.parseLoginData(utils_apiResponse.pickApiData(body));
        const avatarUrl = utils_avatar.resolveAvatarUrl(login.avatar);
        if (ok) {
          applyAuthFromLoginData(login);
        }
        resolve({
          ok,
          nickname: login.nickName,
          avatarUrl,
          setPassword: login.setPassword,
          usesDefaultAvatar: utils_avatar.isEmptyAvatar(login.avatar) || utils_avatar.isDefaultAvatarUrl(avatarUrl),
          msg: body.msg || (ok ? "获取成功" : "获取用户信息失败")
        });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function wxLogin({ phoneCode } = {}) {
  return new Promise(async (resolve) => {
    try {
      const code = await wxLoginCode();
      const data = { code };
      const phone = (phoneCode || "").trim();
      if (phone)
        data.phoneCode = phone;
      api_http_client.request({
        service: "customer",
        path: api_constants_customer.CUSTOMER_API.LOGIN,
        method: "POST",
        data,
        timeout: 6e4,
        success: (res) => {
          utils_cryptoGateway.maybeDecryptResponse(res);
          const body = utils_apiResponse.unwrapResponseBody(res.data);
          const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
          const login = utils_apiResponse.parseLoginData(utils_apiResponse.pickApiData(body));
          if (ok) {
            applyAuthFromLoginData(login);
          }
          resolve(buildLoginResult(ok, login, body));
        },
        fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
      });
    } catch (e) {
      resolve({ ok: false, msg: e.message || "登录失败" });
    }
  });
}
exports.fetchCurrentUserInfo = fetchCurrentUserInfo;
exports.wxLogin = wxLogin;
