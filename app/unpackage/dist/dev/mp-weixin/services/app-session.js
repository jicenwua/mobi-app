"use strict";
const api_modules_auth = require("../api/modules/auth.js");
const services_authRelogin = require("./auth-relogin.js");
const services_notifySocket = require("./notify-socket.js");
const services_userProfile = require("./user-profile.js");
const services_userSecurity = require("./user-security.js");
const api_modules_authToken = require("../api/modules/auth-token.js");
let bootstrapPromise = null;
function resetAppSession() {
  bootstrapPromise = null;
}
function bootstrapAppSession() {
  if (!bootstrapPromise) {
    bootstrapPromise = runBootstrap().then((result) => {
      var _a;
      if (!((_a = result.loginResult) == null ? void 0 : _a.ok)) {
        bootstrapPromise = null;
      }
      return result;
    });
  }
  return bootstrapPromise;
}
function syncProfileFromInfo(info) {
  if (!info.ok)
    return;
  if (info.setPassword !== void 0) {
    services_userSecurity.applyServerSetPasswordFlag(info.setPassword);
  }
  if (info.nickname || info.avatarUrl) {
    services_userProfile.applyLoginProfile({
      nickname: info.nickname,
      avatarUrl: info.avatarUrl,
      usesDefaultAvatar: info.usesDefaultAvatar
    });
  }
}
async function restoreSessionFromToken() {
  const info = await api_modules_auth.fetchCurrentUserInfo();
  if (info.ok) {
    syncProfileFromInfo(info);
    return {
      loginResult: { ok: true, token: api_modules_authToken.getToken(), msg: "已登录" },
      needLogin: false
    };
  }
  api_modules_authToken.clearAuthSession();
  services_notifySocket.disconnectNotifySocket();
  if (info.serverError) {
    return {
      loginResult: { ok: false, msg: info.msg || "服务暂不可用，请稍后重试" },
      needLogin: true,
      serverError: true
    };
  }
  if (info.unauthorized) {
    return null;
  }
  return null;
}
async function runBootstrap() {
  var _a;
  const token = api_modules_authToken.getToken();
  if (token) {
    const restored = await restoreSessionFromToken();
    if ((_a = restored == null ? void 0 : restored.loginResult) == null ? void 0 : _a.ok) {
      return restored;
    }
    if (restored == null ? void 0 : restored.serverError) {
      return {
        loginResult: restored.loginResult,
        needLogin: true
      };
    }
  }
  const autoResult = await services_authRelogin.performAutoLogin();
  if (autoResult.ok) {
    const info = await api_modules_auth.fetchCurrentUserInfo();
    if (info.ok) {
      syncProfileFromInfo(info);
    }
    return {
      loginResult: { ok: true, token: api_modules_authToken.getToken(), msg: "已登录" },
      needLogin: false
    };
  }
  return {
    loginResult: {
      ok: false,
      msg: autoResult.msg || "请先登录",
      needPrivacy: !!autoResult.needPrivacy
    },
    needLogin: true
  };
}
exports.bootstrapAppSession = bootstrapAppSession;
exports.resetAppSession = resetAppSession;
