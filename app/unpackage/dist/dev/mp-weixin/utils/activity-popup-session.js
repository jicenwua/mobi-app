"use strict";
const common_vendor = require("../common/vendor.js");
const STORAGE_PREFIX = "shop_promo_popup_";
function normalizeShopId(shopId) {
  const id = shopId != null && shopId !== "" ? String(shopId).trim() : "";
  return id;
}
function todayKey() {
  const d = /* @__PURE__ */ new Date();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${month}-${day}`;
}
function storageKey(shopId) {
  return `${STORAGE_PREFIX}${normalizeShopId(shopId)}`;
}
function hasShownActivityPopup(shopId) {
  const id = normalizeShopId(shopId);
  if (!id)
    return true;
  try {
    return common_vendor.index.getStorageSync(storageKey(id)) === todayKey();
  } catch {
    return false;
  }
}
function markActivityPopupShown(shopId) {
  const id = normalizeShopId(shopId);
  if (!id)
    return;
  try {
    common_vendor.index.setStorageSync(storageKey(id), todayKey());
  } catch {
  }
}
exports.hasShownActivityPopup = hasShownActivityPopup;
exports.markActivityPopupShown = markActivityPopupShown;
