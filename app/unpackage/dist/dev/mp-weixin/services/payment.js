"use strict";
const common_vendor = require("../common/vendor.js");
const services_userSecurity = require("./user-security.js");
const SET_PASSWORD_URL = "/pages/mine/set-password";
function guardPayPassword() {
  if (services_userSecurity.hasPayPasswordSet()) {
    return Promise.resolve({ ok: true });
  }
  return new Promise((resolve) => {
    common_vendor.index.showModal({
      title: "请先设置支付密码",
      content: "为保障资金安全，支付前需设置 6 位数字支付密码",
      confirmText: "去设置",
      cancelText: "取消",
      success: (res) => {
        if (res.confirm) {
          setTimeout(() => {
            common_vendor.index.navigateTo({
              url: `${SET_PASSWORD_URL}?from=pay`,
              animationType: "slide-in-right",
              animationDuration: 200,
              fail: () => {
                common_vendor.index.showToast({ title: "打开设密页失败，请从「我的」重试", icon: "none" });
              }
            });
          }, 80);
          resolve({ ok: false, navigatedToSet: true });
        } else {
          resolve({ ok: false, navigatedToSet: false });
        }
      },
      fail: () => resolve({ ok: false, navigatedToSet: false })
    });
  });
}
exports.guardPayPassword = guardPayPassword;
