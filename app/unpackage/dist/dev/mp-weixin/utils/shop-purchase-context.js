"use strict";
const common_vendor = require("../common/vendor.js");
const CTX_KEY_PREFIX = "shop_purchase_ctx_v1_";
function ctxKey(shopId) {
  return `${CTX_KEY_PREFIX}${shopId}`;
}
function setShopPurchaseContext(shopId, context) {
  if (!shopId || !context)
    return;
  const remaining = Number(context.remainingPoints);
  try {
    common_vendor.index.setStorageSync(ctxKey(shopId), {
      shopName: context.shopName || "",
      ratio: Number(context.ratio) > 0 ? Number(context.ratio) : 1,
      products: context.products && typeof context.products === "object" ? context.products : {},
      remainingPoints: Number.isNaN(remaining) ? 0 : remaining
    });
  } catch {
  }
}
function getShopPurchaseContext(shopId) {
  if (!shopId)
    return null;
  try {
    const raw = common_vendor.index.getStorageSync(ctxKey(shopId));
    if (!raw || typeof raw !== "object")
      return null;
    return {
      shopName: raw.shopName || "",
      ratio: Number(raw.ratio) > 0 ? Number(raw.ratio) : 1,
      products: raw.products && typeof raw.products === "object" ? raw.products : {},
      remainingPoints: Number(raw.remainingPoints)
    };
  } catch {
    return null;
  }
}
exports.getShopPurchaseContext = getShopPurchaseContext;
exports.setShopPurchaseContext = setShopPurchaseContext;
