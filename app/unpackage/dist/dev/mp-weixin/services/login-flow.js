"use strict";
const api_modules_auth = require("../api/modules/auth.js");
const services_userProfile = require("./user-profile.js");
async function performQuickLogin() {
  const loginResult = await api_modules_auth.wxLogin();
  if (!loginResult.ok) {
    return loginResult;
  }
  if (loginResult.register) {
    return loginResult;
  }
  services_userProfile.markProfileSetupDone();
  const info = await api_modules_auth.fetchCurrentUserInfo();
  if (info.ok) {
    services_userProfile.applyLoginProfile({
      nickname: info.nickname,
      avatarUrl: info.avatarUrl,
      usesDefaultAvatar: info.usesDefaultAvatar
    });
  }
  return loginResult;
}
function resolvePostLoginPath(register) {
  return register ? "/pages/login/profile-setup" : "/pages/main/main";
}
exports.performQuickLogin = performQuickLogin;
exports.resolvePostLoginPath = resolvePostLoginPath;
