"use strict";
const common_vendor = require("../common/vendor.js");
const utils_staffScan = require("../utils/staff-scan.js");
const api_modules_qrcode = require("../api/modules/qrcode.js");
const api_modules_points = require("../api/modules/points.js");
const utils_requestId = require("../utils/request-id.js");
function useShopManageCheckout({ shopId, canScanVerify, productCategories, loadProducts, setActiveTab }) {
  const checkoutPayToken = common_vendor.ref("");
  const checkoutMember = common_vendor.ref(null);
  const checkoutCart = common_vendor.reactive({});
  const checkoutSubmitting = common_vendor.ref(false);
  let checkoutRequestId = "";
  const checkoutCartCount = common_vendor.computed(
    () => Object.values(checkoutCart).reduce((sum, qty) => sum + (Number(qty) || 0), 0)
  );
  const checkoutTotalPoints = common_vendor.computed(() => {
    let sum = 0;
    for (const cat of productCategories.value) {
      for (const product of cat.products || []) {
        const qty = Number(checkoutCart[product.productId]) || 0;
        if (qty <= 0)
          continue;
        const price = Number(product.price);
        if (Number.isNaN(price))
          continue;
        sum += Math.round(price) * qty;
      }
    }
    return sum;
  });
  function applyMemberForCheckout(token, memberInfo) {
    checkoutPayToken.value = token;
    checkoutMember.value = memberInfo;
    setActiveTab("product");
    common_vendor.index.showToast({ title: "已识别会员，请确认扣款", icon: "none" });
  }
  async function applyCheckoutToken(token) {
    const verify = await api_modules_qrcode.verifyPayQrcode({ shopId: shopId.value, token });
    if (!verify.ok || !verify.data) {
      common_vendor.index.showToast({ title: verify.msg || "付款码无效", icon: "none" });
      return;
    }
    applyMemberForCheckout(token, verify.data);
  }
  function exitCheckout() {
    checkoutPayToken.value = "";
    checkoutMember.value = null;
    checkoutRequestId = "";
    Object.keys(checkoutCart).forEach((key) => {
      delete checkoutCart[key];
    });
  }
  function onCheckoutQtyChange({ product, delta }) {
    const id = product == null ? void 0 : product.productId;
    if (id == null)
      return;
    const cur = Number(checkoutCart[id]) || 0;
    const next = cur + delta;
    if (next <= 0) {
      delete checkoutCart[id];
    } else {
      checkoutCart[id] = next;
    }
  }
  function buildCheckoutItems() {
    return Object.entries(checkoutCart).filter(([, count]) => Number(count) > 0).map(([productId, count]) => ({ productId: Number(productId), count: Number(count) }));
  }
  async function doCheckoutConsume(token) {
    const items = buildCheckoutItems();
    if (!items.length) {
      common_vendor.index.showToast({ title: "请选择商品", icon: "none" });
      return;
    }
    checkoutSubmitting.value = true;
    if (!checkoutRequestId) {
      checkoutRequestId = utils_requestId.createRequestId();
    }
    const res = await api_modules_points.consumePoints({
      shopId: Number(shopId.value),
      token,
      items,
      requestId: checkoutRequestId
    });
    checkoutSubmitting.value = false;
    if (!res.ok) {
      common_vendor.index.showToast({ title: res.msg || "扣款失败", icon: "none" });
      return;
    }
    checkoutRequestId = "";
    common_vendor.index.showToast({
      title: `扣款成功，-${res.data ?? checkoutTotalPoints.value} 积分`,
      icon: "success"
    });
    exitCheckout();
    void loadProducts();
  }
  async function submitCheckoutConsume() {
    if (checkoutSubmitting.value || checkoutTotalPoints.value <= 0)
      return;
    if (!checkoutPayToken.value) {
      utils_staffScan.scanStaffPayCode({
        shopId: shopId.value,
        onPayVerified: async (token, memberInfo) => {
          applyMemberForCheckout(token, memberInfo);
          await doCheckoutConsume(token);
        }
      });
      return;
    }
    await doCheckoutConsume(checkoutPayToken.value);
  }
  function scanForVerify() {
    if (!shopId.value || !canScanVerify.value)
      return;
    utils_staffScan.scanStaffVerifyCode({
      shopId: shopId.value,
      onPayVerified: (token, memberInfo) => {
        if (checkoutCartCount.value <= 0) {
          common_vendor.index.showToast({ title: "请先在下方选购商品", icon: "none" });
          return;
        }
        applyMemberForCheckout(token, memberInfo);
      }
    });
  }
  function shouldBlockProductClick() {
    return canScanVerify.value && (checkoutCartCount.value > 0 || checkoutMember.value);
  }
  return {
    checkoutPayToken,
    checkoutMember,
    checkoutCart,
    checkoutSubmitting,
    checkoutCartCount,
    checkoutTotalPoints,
    applyMemberForCheckout,
    applyCheckoutToken,
    exitCheckout,
    onCheckoutQtyChange,
    submitCheckoutConsume,
    scanForVerify,
    shouldBlockProductClick
  };
}
exports.useShopManageCheckout = useShopManageCheckout;
