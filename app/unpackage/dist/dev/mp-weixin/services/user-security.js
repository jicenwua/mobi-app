"use strict";
const common_vendor = require("../common/vendor.js");
const SECURITY_KEY = "user_security";
const defaultSecurity = () => ({
  hasSetPassword: false
});
function getUserSecurity() {
  try {
    const stored = common_vendor.index.getStorageSync(SECURITY_KEY);
    if (stored && typeof stored === "object") {
      return { ...defaultSecurity(), ...stored, hasSetPassword: !!stored.hasSetPassword };
    }
  } catch {
  }
  return defaultSecurity();
}
function hasPayPasswordSet() {
  return getUserSecurity().hasSetPassword;
}
function setUserSecurity(partial) {
  const next = {
    ...getUserSecurity(),
    ...partial,
    hasSetPassword: !!(partial == null ? void 0 : partial.hasSetPassword)
  };
  common_vendor.index.setStorageSync(SECURITY_KEY, next);
  return next;
}
function applyServerSetPasswordFlag(raw) {
  if (raw === void 0 || raw === null)
    return getUserSecurity();
  const hasSetPassword = !!raw;
  return setUserSecurity({ hasSetPassword });
}
function clearUserSecurity() {
  try {
    common_vendor.index.removeStorageSync(SECURITY_KEY);
  } catch {
  }
}
exports.applyServerSetPasswordFlag = applyServerSetPasswordFlag;
exports.clearUserSecurity = clearUserSecurity;
exports.hasPayPasswordSet = hasPayPasswordSet;
exports.setUserSecurity = setUserSecurity;
