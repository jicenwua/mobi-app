"use strict";
const common_vendor = require("../common/vendor.js");
const api_modules_qrcode = require("../api/modules/qrcode.js");
const api_modules_points = require("../api/modules/points.js");
const api_modules_shop = require("../api/modules/shop.js");
const utils_memberOrdersCache = require("./member-orders-cache.js");
const utils_qrcodeScan = require("./qrcode-scan.js");
function openManageCheckout(shopId) {
  common_vendor.index.navigateTo({
    url: `/pages/shop/manage?id=${shopId}`,
    animationType: "slide-in-right",
    animationDuration: 200
  });
}
function scanStaffPayCode({ shopId, onPayVerified }) {
  if (!shopId) {
    common_vendor.index.showToast({ title: "店铺信息无效", icon: "none" });
    return;
  }
  common_vendor.index.scanCode({
    onlyFromCamera: false,
    success: async (res) => {
      const payToken = utils_qrcodeScan.parsePayTokenFromScan((res == null ? void 0 : res.result) || "");
      if (!payToken) {
        common_vendor.index.showToast({ title: "请扫描会员付款码", icon: "none" });
        return;
      }
      const verify = await api_modules_qrcode.verifyPayQrcode({ shopId, token: payToken });
      if (!verify.ok || !verify.data) {
        common_vendor.index.showToast({ title: verify.msg || "付款码无效", icon: "none" });
        return;
      }
      if (typeof onPayVerified === "function") {
        onPayVerified(payToken, verify.data);
        return;
      }
      openManageCheckout(shopId);
    },
    fail: () => {
      common_vendor.index.showToast({ title: "扫码取消", icon: "none" });
    }
  });
}
function scanStaffVerifyCode({ shopId, onPayVerified }) {
  if (!shopId) {
    common_vendor.index.showToast({ title: "店铺信息无效", icon: "none" });
    return;
  }
  common_vendor.index.scanCode({
    onlyFromCamera: false,
    success: async (res) => {
      const raw = (res == null ? void 0 : res.result) || "";
      const orderToken = utils_qrcodeScan.parseOrderTokenFromScan(raw);
      if (orderToken) {
        const verify2 = await api_modules_points.verifyOrderQrcode({
          shopId: Number(shopId),
          token: orderToken
        });
        if (!verify2.ok) {
          common_vendor.index.showToast({ title: verify2.msg || "核销失败", icon: "none" });
          return;
        }
        common_vendor.index.showToast({ title: "订单核销成功", icon: "success" });
        utils_memberOrdersCache.invalidateMemberOrdersCache();
        return;
      }
      const payToken = utils_qrcodeScan.parsePayTokenFromScan(raw);
      if (!payToken) {
        common_vendor.index.showToast({ title: "无效的二维码", icon: "none" });
        return;
      }
      const verify = await api_modules_qrcode.verifyPayQrcode({ shopId, token: payToken });
      if (!verify.ok || !verify.data) {
        common_vendor.index.showToast({ title: verify.msg || "付款码无效", icon: "none" });
        return;
      }
      if (typeof onPayVerified === "function") {
        onPayVerified(payToken, verify.data);
        return;
      }
      openManageCheckout(shopId);
    },
    fail: () => {
      common_vendor.index.showToast({ title: "扫码取消", icon: "none" });
    }
  });
}
function scanStaffInviteCode({ shopId, onAdded }) {
  if (!shopId) {
    common_vendor.index.showToast({ title: "店铺信息无效", icon: "none" });
    return;
  }
  common_vendor.index.scanCode({
    onlyFromCamera: false,
    success: async (res) => {
      var _a;
      const token = utils_qrcodeScan.parseStaffInviteTokenFromScan((res == null ? void 0 : res.result) || "");
      if (!token) {
        common_vendor.index.showToast({ title: "请扫描店员邀请码", icon: "none" });
        return;
      }
      const result = await api_modules_shop.addShopStaffByToken({ shopId: Number(shopId), token });
      if (!result.ok) {
        common_vendor.index.showToast({ title: result.msg || "添加失败", icon: "none" });
        return;
      }
      const name = ((_a = result.data) == null ? void 0 : _a.nickname) || "该用户";
      common_vendor.index.showToast({ title: `已添加 ${name} 为店员`, icon: "success" });
      if (typeof onAdded === "function") {
        onAdded(result.data);
      }
    },
    fail: () => {
      common_vendor.index.showToast({ title: "扫码取消", icon: "none" });
    }
  });
}
exports.scanStaffInviteCode = scanStaffInviteCode;
exports.scanStaffPayCode = scanStaffPayCode;
exports.scanStaffVerifyCode = scanStaffVerifyCode;
