"use strict";
const common_vendor = require("../common/vendor.js");
function parseShopCodeFromScan(raw) {
  const text = (raw || "").trim();
  if (!text)
    return "";
  const inviteMatch = text.match(/^MOBI:SHOP:INV:(.+)$/i);
  if (inviteMatch)
    return "";
  const mobiMatch = text.match(/^MOBI:SHOP:(.+)$/i);
  if (mobiMatch)
    return mobiMatch[1].trim();
  if (/^[A-Za-z0-9]{6,16}$/.test(text))
    return text;
  return text;
}
function parseShopInviteFromScan(raw) {
  const text = (raw || "").trim();
  if (!text)
    return "";
  const match = text.match(/^MOBI:SHOP:INV:(.+)$/i);
  return match ? match[1].trim() : "";
}
function parsePayTokenFromScan(raw) {
  const text = (raw || "").trim();
  const match = text.match(/^MOBI:PAY:(.+)$/i);
  return match ? match[1].trim() : "";
}
function parseOrderTokenFromScan(raw) {
  const text = (raw || "").trim();
  const match = text.match(/^MOBI:ORDER:(.+)$/i);
  return match ? match[1].trim() : "";
}
function parseStaffInviteTokenFromScan(raw) {
  const text = (raw || "").trim();
  const match = text.match(/^MOBI:STAFF:INV:(.+)$/i);
  return match ? match[1].trim() : "";
}
function parseLaunchShopInvite(options) {
  if (!options)
    return null;
  if (options.inviteToken) {
    return { token: String(options.inviteToken).trim() };
  }
  if (options.shopCode) {
    return { shopCode: String(options.shopCode).trim() };
  }
  if (options.scene) {
    const scene = decodeURIComponent(String(options.scene));
    if (scene.startsWith("t") && scene.length > 1) {
      return { token: scene.slice(1).trim() };
    }
    if (scene.startsWith("s=")) {
      return { shopCode: scene.slice(2).trim() };
    }
    return { shopCode: scene.trim() };
  }
  return null;
}
const PENDING_SHOP_CODE_KEY = "pending_shop_code";
const PENDING_SHOP_INVITE_KEY = "pending_shop_invite";
function savePendingShopCode(code) {
  if (!code)
    return;
  try {
    common_vendor.index.setStorageSync(PENDING_SHOP_CODE_KEY, code);
  } catch {
  }
}
function takePendingShopCode() {
  try {
    const code = common_vendor.index.getStorageSync(PENDING_SHOP_CODE_KEY);
    if (code)
      common_vendor.index.removeStorageSync(PENDING_SHOP_CODE_KEY);
    return code ? String(code).trim() : "";
  } catch {
    return "";
  }
}
function savePendingShopInvite(token) {
  if (!token)
    return;
  try {
    common_vendor.index.setStorageSync(PENDING_SHOP_INVITE_KEY, token);
  } catch {
  }
}
function takePendingShopInvite() {
  try {
    const token = common_vendor.index.getStorageSync(PENDING_SHOP_INVITE_KEY);
    if (token)
      common_vendor.index.removeStorageSync(PENDING_SHOP_INVITE_KEY);
    return token ? String(token).trim() : "";
  } catch {
    return "";
  }
}
exports.parseLaunchShopInvite = parseLaunchShopInvite;
exports.parseOrderTokenFromScan = parseOrderTokenFromScan;
exports.parsePayTokenFromScan = parsePayTokenFromScan;
exports.parseShopCodeFromScan = parseShopCodeFromScan;
exports.parseShopInviteFromScan = parseShopInviteFromScan;
exports.parseStaffInviteTokenFromScan = parseStaffInviteTokenFromScan;
exports.savePendingShopCode = savePendingShopCode;
exports.savePendingShopInvite = savePendingShopInvite;
exports.takePendingShopCode = takePendingShopCode;
exports.takePendingShopInvite = takePendingShopInvite;
