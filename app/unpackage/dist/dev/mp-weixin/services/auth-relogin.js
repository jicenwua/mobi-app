"use strict";
const common_vendor = require("../common/vendor.js");
const api_modules_authToken = require("../api/modules/auth-token.js");
const services_notifySocket = require("./notify-socket.js");
const utils_wxPrivacy = require("../utils/wx-privacy.js");
const LOGIN_PAGE = "/pages/login/login";
let autoLoginPromise = null;
let quickLoginHandler = null;
let resetSessionHandler = null;
let fetchUserInfoHandler = null;
let permissionSyncPromise = null;
function configureAuthRelogin({
  performQuickLogin,
  resetAppSession,
  fetchCurrentUserInfo
}) {
  quickLoginHandler = performQuickLogin;
  resetSessionHandler = resetAppSession;
  fetchUserInfoHandler = fetchCurrentUserInfo;
}
async function canAutoLogin() {
  const state = await utils_wxPrivacy.getPrivacySettingState();
  return !state.needAuthorization;
}
async function performAutoLogin() {
  if (autoLoginPromise) {
    return autoLoginPromise;
  }
  autoLoginPromise = (async () => {
    if (!quickLoginHandler) {
      return { ok: false, msg: "登录模块未就绪" };
    }
    const allowed = await canAutoLogin();
    if (!allowed) {
      return { ok: false, needPrivacy: true, msg: "请先同意隐私保护指引" };
    }
    const result = await quickLoginHandler();
    if (result.ok && resetSessionHandler) {
      resetSessionHandler();
    }
    return result;
  })().finally(() => {
    autoLoginPromise = null;
  });
  return autoLoginPromise;
}
async function handleSessionUnauthorized() {
  api_modules_authToken.clearAuthSession();
  services_notifySocket.disconnectNotifySocket();
  if (resetSessionHandler) {
    resetSessionHandler();
  }
  return performAutoLogin();
}
function isOnLoginPage() {
  try {
    const pages = getCurrentPages();
    const current = pages[pages.length - 1];
    const route = (current == null ? void 0 : current.route) || "";
    return route.includes("pages/login/login");
  } catch {
    return false;
  }
}
async function syncPermissionsAfterTokenRefresh() {
  if (!fetchUserInfoHandler)
    return;
  if (permissionSyncPromise) {
    return permissionSyncPromise;
  }
  permissionSyncPromise = fetchUserInfoHandler().finally(() => {
    permissionSyncPromise = null;
  });
  return permissionSyncPromise;
}
function redirectToLoginIfNeeded(loginResult) {
  if (loginResult == null ? void 0 : loginResult.ok)
    return;
  if (isOnLoginPage())
    return;
  common_vendor.index.reLaunch({ url: LOGIN_PAGE });
}
async function ensureAuthenticated() {
  if (api_modules_authToken.getToken()) {
    return { ok: true };
  }
  const result = await performAutoLogin();
  if (!result.ok) {
    redirectToLoginIfNeeded(result);
  }
  return result;
}
exports.configureAuthRelogin = configureAuthRelogin;
exports.ensureAuthenticated = ensureAuthenticated;
exports.handleSessionUnauthorized = handleSessionUnauthorized;
exports.performAutoLogin = performAutoLogin;
exports.redirectToLoginIfNeeded = redirectToLoginIfNeeded;
exports.syncPermissionsAfterTokenRefresh = syncPermissionsAfterTokenRefresh;
