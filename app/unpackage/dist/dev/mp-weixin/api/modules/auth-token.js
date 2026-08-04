"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_permissions = require("../../utils/permissions.js");
const services_userSecurity = require("../../services/user-security.js");
const TOKEN_KEY = "auth_token";
function normalizeToken(raw) {
  if (raw == null || raw === "")
    return "";
  const value = String(raw).trim();
  if (/^bearer\s+/i.test(value)) {
    return value.replace(/^bearer\s+/i, "").trim();
  }
  return value;
}
function getToken() {
  try {
    return normalizeToken(common_vendor.index.getStorageSync(TOKEN_KEY));
  } catch {
    return "";
  }
}
function setToken(token) {
  const normalized = normalizeToken(token);
  if (normalized) {
    const previous = getToken();
    common_vendor.index.setStorageSync(TOKEN_KEY, normalized);
    return previous !== normalized;
  }
  common_vendor.index.removeStorageSync(TOKEN_KEY);
  utils_permissions.clearSessionPermissions();
  services_userSecurity.clearUserSecurity();
  return false;
}
function clearAuthSession() {
  setToken("");
}
function extractTokenFromResponse(res) {
  const headers = (res == null ? void 0 : res.header) || (res == null ? void 0 : res.headers) || {};
  const raw = headers.authorization || headers.Authorization || "";
  return normalizeToken(raw);
}
function applyResponseToken(res) {
  const newToken = extractTokenFromResponse(res);
  if (!newToken)
    return false;
  return setToken(newToken);
}
function extractPermissionsFromResponse(res) {
  const headers = (res == null ? void 0 : res.header) || (res == null ? void 0 : res.headers) || {};
  const raw = headers.role_permission || headers["role-permission"] || headers.Role_Permission || "";
  if (!raw)
    return null;
  if (typeof raw === "object")
    return raw;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}
function applyResponseSession(res) {
  const tokenChanged = applyResponseToken(res);
  const permissions = extractPermissionsFromResponse(res);
  let permChanged = false;
  if (permissions != null) {
    utils_permissions.setSessionPermissions(permissions);
    permChanged = true;
  }
  return { tokenChanged, permChanged };
}
exports.applyResponseSession = applyResponseSession;
exports.clearAuthSession = clearAuthSession;
exports.getToken = getToken;
exports.setToken = setToken;
