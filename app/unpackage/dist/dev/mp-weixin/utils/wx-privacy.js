"use strict";
const common_vendor = require("../common/vendor.js");
const privacyPopupVisible = common_vendor.ref(false);
const wxPrivacyAuthorized = common_vendor.ref(false);
function formatPrivacyContractLabel(name) {
  const inner = String(name || "").trim().replace(/^《+/, "").replace(/》+$/, "");
  return inner ? `《${inner}》` : "《用户隐私保护指引》";
}
function showPrivacyPopupManually() {
  privacyPopupVisible.value = true;
}
let pendingPrivacyResolve = null;
function openPrivacyContract() {
  if (typeof common_vendor.index.openPrivacyContract !== "function") {
    common_vendor.index.showToast({ title: "请在微信小程序中查看", icon: "none" });
    return;
  }
  common_vendor.index.openPrivacyContract({
    fail: () => common_vendor.index.showToast({ title: "暂无法打开隐私指引", icon: "none" })
  });
}
function showPrivacyPopup(resolve) {
  if (resolve) {
    pendingPrivacyResolve = resolve;
  }
  privacyPopupVisible.value = true;
}
function finishPrivacyResolve(payload) {
  if (pendingPrivacyResolve) {
    pendingPrivacyResolve(payload);
    pendingPrivacyResolve = null;
  }
  {
    privacyPopupVisible.value = false;
  }
}
function onPrivacyAgree() {
  finishPrivacyResolve({ event: "agree", buttonId: "privacy-agree-btn" });
  wxPrivacyAuthorized.value = true;
  privacyPopupVisible.value = false;
}
function onPrivacyDisagree() {
  finishPrivacyResolve({ event: "disagree", buttonId: "privacy-disagree-btn" });
  privacyPopupVisible.value = false;
}
function setupPrivacyAuthorization() {
  if (typeof common_vendor.index.onNeedPrivacyAuthorization !== "function") {
    return;
  }
  common_vendor.index.onNeedPrivacyAuthorization((resolve) => {
    showPrivacyPopup(resolve);
  });
}
function getPrivacySettingState() {
  return new Promise((resolve) => {
    if (typeof common_vendor.index.getPrivacySetting !== "function") {
      resolve({
        needAuthorization: false,
        privacyContractName: "用户隐私保护指引"
      });
      return;
    }
    common_vendor.index.getPrivacySetting({
      success: (res) => {
        resolve({
          needAuthorization: !!res.needAuthorization,
          privacyContractName: res.privacyContractName || "用户隐私保护指引"
        });
      },
      fail: () => resolve({
        needAuthorization: false,
        privacyContractName: "用户隐私保护指引"
      })
    });
  });
}
exports.formatPrivacyContractLabel = formatPrivacyContractLabel;
exports.getPrivacySettingState = getPrivacySettingState;
exports.onPrivacyAgree = onPrivacyAgree;
exports.onPrivacyDisagree = onPrivacyDisagree;
exports.openPrivacyContract = openPrivacyContract;
exports.privacyPopupVisible = privacyPopupVisible;
exports.setupPrivacyAuthorization = setupPrivacyAuthorization;
exports.showPrivacyPopupManually = showPrivacyPopupManually;
exports.wxPrivacyAuthorized = wxPrivacyAuthorized;
