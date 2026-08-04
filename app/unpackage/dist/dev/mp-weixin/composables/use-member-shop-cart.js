"use strict";
const common_vendor = require("../common/vendor.js");
const utils_shopCartCache = require("../utils/shop-cart-cache.js");
const utils_shopPurchaseContext = require("../utils/shop-purchase-context.js");
const utils_pointsFormat = require("../utils/points-format.js");
function useMemberShopCart(shopId, { productCategories, detail }) {
  const cart = common_vendor.reactive({});
  const checkingOut = common_vendor.ref(false);
  const cartItemCount = common_vendor.computed(
    () => Object.values(cart).reduce((sum, qty) => sum + (Number(qty) || 0), 0)
  );
  const cartTotalPoints = common_vendor.computed(() => {
    let sum = 0;
    for (const cat of productCategories.value) {
      const list = Array.isArray(cat == null ? void 0 : cat.products) ? cat.products : [];
      for (const product of list) {
        const qty = cart[product.productId];
        if (!qty)
          continue;
        const price = Number(product.price);
        if (Number.isNaN(price))
          continue;
        sum += Math.round(price) * qty;
      }
    }
    return sum;
  });
  const cartTotalPointsText = common_vendor.computed(() => utils_pointsFormat.formatPointsAmount(cartTotalPoints.value));
  const cartLines = common_vendor.computed(() => {
    const lines = [];
    for (const cat of productCategories.value) {
      const list = Array.isArray(cat == null ? void 0 : cat.products) ? cat.products : [];
      for (const product of list) {
        const count = Number(cart[product.productId]) || 0;
        if (count <= 0)
          continue;
        const price = Number(product.price);
        const unitPoints = Number.isNaN(price) ? 0 : Math.round(price);
        lines.push({
          productId: product.productId,
          productName: product.productName || "商品",
          count,
          unitPoints,
          linePoints: unitPoints * count,
          product
        });
      }
    }
    return lines;
  });
  function restoreCartFromCache() {
    Object.keys(cart).forEach((key) => {
      delete cart[key];
    });
    const saved = utils_shopCartCache.getShopCart(shopId.value);
    Object.assign(cart, saved);
  }
  function persistCart() {
    utils_shopCartCache.setShopCart(shopId.value, cart);
  }
  function syncCartFromCache() {
    restoreCartFromCache();
  }
  function onCartQtyChange({ product, delta }) {
    const id = product == null ? void 0 : product.productId;
    if (id == null)
      return;
    const cur = Number(cart[id]) || 0;
    const next = cur + delta;
    if (next <= 0) {
      delete cart[id];
    } else {
      cart[id] = next;
    }
    persistCart();
  }
  function onCartQtyUpdate({ productId, delta }) {
    const line = cartLines.value.find((item) => item.productId === productId);
    if (!(line == null ? void 0 : line.product))
      return;
    onCartQtyChange({ product: line.product, delta });
  }
  function goPurchaseConfirm() {
    var _a;
    if (cartItemCount.value <= 0) {
      common_vendor.index.showToast({ title: "请选择商品", icon: "none" });
      return;
    }
    const id = shopId.value;
    if (!id)
      return;
    utils_shopPurchaseContext.setShopPurchaseContext({
      shopId: id,
      shopName: ((_a = detail.value) == null ? void 0 : _a.shopName) || "",
      cartLines: cartLines.value,
      totalPoints: cartTotalPoints.value
    });
    common_vendor.index.navigateTo({
      url: `/pages/member/purchase-confirm?shopId=${id}`,
      animationType: "slide-in-right",
      animationDuration: 200
    });
  }
  async function submitPurchase() {
    void goPurchaseConfirm();
  }
  return {
    cart,
    checkingOut,
    cartItemCount,
    cartTotalPoints,
    cartTotalPointsText,
    cartLines,
    restoreCartFromCache,
    syncCartFromCache,
    onCartQtyChange,
    onCartQtyUpdate,
    submitPurchase
  };
}
exports.useMemberShopCart = useMemberShopCart;
