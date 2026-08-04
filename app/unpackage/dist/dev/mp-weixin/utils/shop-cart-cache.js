"use strict";
const common_vendor = require("../common/vendor.js");
const CART_KEY_PREFIX = "shop_cart_v1_";
function cartKey(shopId) {
  return `${CART_KEY_PREFIX}${shopId}`;
}
function getShopCart(shopId) {
  if (!shopId)
    return {};
  try {
    const raw = common_vendor.index.getStorageSync(cartKey(shopId));
    if (!raw || typeof raw !== "object")
      return {};
    const cart = {};
    for (const [id, qty] of Object.entries(raw)) {
      const n = Number(qty);
      if (n > 0)
        cart[id] = n;
    }
    return cart;
  } catch {
    return {};
  }
}
function setShopCart(shopId, cart) {
  if (!shopId)
    return;
  try {
    const normalized = {};
    if (cart && typeof cart === "object") {
      for (const [id, qty] of Object.entries(cart)) {
        const n = Number(qty);
        if (n > 0)
          normalized[id] = n;
      }
    }
    if (Object.keys(normalized).length) {
      common_vendor.index.setStorageSync(cartKey(shopId), normalized);
    } else {
      common_vendor.index.removeStorageSync(cartKey(shopId));
    }
  } catch {
  }
}
function clearShopCart(shopId) {
  if (!shopId)
    return;
  try {
    common_vendor.index.removeStorageSync(cartKey(shopId));
  } catch {
  }
}
function changeShopCartQty(shopId, productId, delta) {
  const cart = getShopCart(shopId);
  const id = String(productId);
  const cur = Number(cart[id]) || 0;
  const next = cur + delta;
  if (next <= 0) {
    delete cart[id];
  } else {
    cart[id] = next;
  }
  setShopCart(shopId, cart);
  return cart;
}
function getShopCartQty(shopId, productId) {
  if (!shopId || productId == null)
    return 0;
  const cart = getShopCart(shopId);
  return Number(cart[String(productId)]) || 0;
}
exports.changeShopCartQty = changeShopCartQty;
exports.clearShopCart = clearShopCart;
exports.getShopCart = getShopCart;
exports.getShopCartQty = getShopCartQty;
exports.setShopCart = setShopCart;
