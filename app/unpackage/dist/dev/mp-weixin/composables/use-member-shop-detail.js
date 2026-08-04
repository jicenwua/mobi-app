"use strict";
const common_vendor = require("../common/vendor.js");
const api_modules_shop = require("../api/modules/shop.js");
const utils_pointsFormat = require("../utils/points-format.js");
const utils_activityDisplay = require("../utils/activity-display.js");
const utils_wxPerm = require("../utils/wx-perm.js");
const utils_staffScan = require("../utils/staff-scan.js");
const utils_shopRole = require("../utils/shop-role.js");
const composables_useMemberShopCart = require("./use-member-shop-cart.js");
const composables_useMemberShopPromo = require("./use-member-shop-promo.js");
const utils_shopPageContext = require("../utils/shop-page-context.js");
function useMemberShopDetail(props) {
  const shopId = common_vendor.toRef(props, "shopId");
  const detail = common_vendor.ref(null);
  const loading = common_vendor.ref(true);
  const errorMsg = common_vendor.ref("");
  const products = common_vendor.ref([]);
  const productCategories = common_vendor.ref([]);
  const productsLoaded = common_vendor.ref(false);
  const productsLoading = common_vendor.ref(false);
  let detailLoadToken = 0;
  const promo = composables_useMemberShopPromo.useMemberShopPromo(shopId, detail);
  const cart = composables_useMemberShopCart.useMemberShopCart(shopId, { productCategories, detail });
  const carouselImages = common_vendor.computed(() => api_modules_shop.getShopCarouselImages(detail.value));
  const shopAddress = common_vendor.computed(() => {
    var _a;
    const formatted = api_modules_shop.formatShopAddress(detail.value);
    if (formatted)
      return formatted;
    return (((_a = detail.value) == null ? void 0 : _a.address) || "").trim();
  });
  const shopPhone = common_vendor.computed(() => {
    var _a;
    return (((_a = detail.value) == null ? void 0 : _a.phone) || "").trim();
  });
  const remainingPoints = common_vendor.computed(() => {
    var _a;
    const n = (_a = detail.value) == null ? void 0 : _a.remainingPoints;
    if (n == null || n === "")
      return "0.00";
    return utils_pointsFormat.formatPointsAmount(n);
  });
  const canPayQrcode = common_vendor.computed(() => utils_wxPerm.canShowMemberTab());
  const showManageLink = common_vendor.computed(() => utils_shopRole.isShopClerkCapable(detail.value) && utils_wxPerm.canManageShop());
  const showScanVerifyBtn = common_vendor.computed(() => utils_wxPerm.canStaffVerifyAtShop(detail.value));
  function callShopPhone() {
    const phone = shopPhone.value;
    if (!phone)
      return;
    common_vendor.index.makePhoneCall({ phoneNumber: phone });
  }
  function goManage() {
    const id = shopId.value;
    if (!id || !showManageLink.value) {
      common_vendor.index.showToast({ title: "无店铺管理权限", icon: "none" });
      return;
    }
    const shop = detail.value || { id, shopId: id };
    utils_shopPageContext.navigateWithShop({
      url: `/pages/shop/manage?id=${id}`,
      shop
    });
  }
  function goPayQrcode() {
    if (!utils_wxPerm.canGeneratePayQrcode()) {
      common_vendor.index.showToast({ title: "无付款码权限", icon: "none" });
      return;
    }
    common_vendor.index.navigateTo({
      url: "/pages/member/pay-qrcode",
      animationType: "slide-in-right",
      animationDuration: 200
    });
  }
  function scanForVerify() {
    const id = shopId.value;
    if (!id || !showScanVerifyBtn.value)
      return;
    utils_staffScan.scanStaffVerifyCode({ shopId: id });
  }
  function resetPageState() {
    products.value = [];
    productCategories.value = [];
    productsLoaded.value = false;
    productsLoading.value = false;
    promo.resetPromo();
    cart.restoreCartFromCache();
  }
  function finishDetailLoad(data) {
    const normalized = utils_shopPageContext.rememberShopDetail(data) || api_modules_shop.normalizeShopDetail(data);
    detail.value = normalized || data;
    loading.value = false;
    errorMsg.value = "";
  }
  async function loadMemberDetailExtras(token) {
    const id = shopId.value;
    if (!id || token !== detailLoadToken || !detail.value)
      return;
    if (productsLoaded.value)
      return;
    productsLoading.value = true;
    try {
      const productRes = await api_modules_shop.fetchShopProductCatalog(Number(id));
      if (token !== detailLoadToken)
        return;
      productCategories.value = productRes.ok && Array.isArray(productRes.data) ? productRes.data : [];
      products.value = productRes.ok ? productRes.rows : [];
      productsLoaded.value = true;
      if (!productRes.ok) {
        common_vendor.index.showToast({ title: productRes.msg || "商品加载失败", icon: "none" });
      }
      void loadAsyncMemberExtras(Number(id), token);
    } finally {
      if (token === detailLoadToken) {
        productsLoading.value = false;
      }
    }
  }
  async function loadAsyncMemberExtras(id, token) {
    const includeCoupons = promo.shouldIncludeCouponsInExtras();
    const extrasRes = await api_modules_shop.fetchShopMemberExtras(id, { includeCoupons });
    if (token !== detailLoadToken || !detail.value)
      return;
    detail.value = {
      ...detail.value,
      activities: extrasRes.activities,
      announcement: extrasRes.announcement,
      coupons: includeCoupons ? extrasRes.coupons : []
    };
    promo.showActivityModalIfNeeded();
    if (includeCoupons && !promo.activityModalVisible.value) {
      promo.showCouponModalIfNeeded();
    }
  }
  async function loadDetailFallback(token) {
    if (token !== detailLoadToken)
      return;
    if (detail.value && api_modules_shop.isUsableShopDetail(detail.value))
      return;
    if (!utils_wxPerm.canShowMemberTab()) {
      errorMsg.value = "无会员访问权限";
      loading.value = false;
      detail.value = null;
      return;
    }
    const id = shopId.value;
    if (!id) {
      errorMsg.value = "店铺信息无效";
      loading.value = false;
      detail.value = null;
      return;
    }
    loading.value = true;
    errorMsg.value = "";
    const res = await api_modules_shop.fetchShopFromUserList(id);
    if (token !== detailLoadToken)
      return;
    if (!res.ok || !res.data) {
      loading.value = false;
      errorMsg.value = res.msg || "加载失败";
      detail.value = null;
      return;
    }
    if (token !== detailLoadToken)
      return;
    finishDetailLoad(res.data);
    if (token !== detailLoadToken)
      return;
    await loadMemberDetailExtras(token);
  }
  async function applyShop(data) {
    detailLoadToken += 1;
    const token = detailLoadToken;
    if (!utils_wxPerm.canShowMemberTab()) {
      errorMsg.value = "无会员访问权限";
      loading.value = false;
      detail.value = null;
      return;
    }
    const id = shopId.value;
    if (!id) {
      errorMsg.value = "店铺信息无效";
      loading.value = false;
      detail.value = null;
      return;
    }
    if (detail.value && api_modules_shop.isUsableShopDetail(detail.value)) {
      if (!productsLoaded.value && token === detailLoadToken) {
        await loadMemberDetailExtras(token);
      }
      return;
    }
    if (!api_modules_shop.isUsableShopDetail(data)) {
      await loadDetailFallback(token);
      return;
    }
    const normalized = api_modules_shop.normalizeShopDetail(data);
    if (String(normalized.id) !== String(id)) {
      await loadDetailFallback(token);
      return;
    }
    loading.value = true;
    errorMsg.value = "";
    finishDetailLoad(normalized);
    await loadMemberDetailExtras(token);
  }
  function formatProductPoints(product) {
    const price = product == null ? void 0 : product.price;
    if (price == null)
      return "—";
    const n = Number(price);
    if (Number.isNaN(n))
      return "—";
    if (Number.isInteger(n))
      return String(n);
    return String(Math.round(n * 100) / 100);
  }
  function openProductDetail(product) {
    const id = shopId.value;
    if (!(product == null ? void 0 : product.productId) || !id)
      return;
    utils_shopPageContext.navigateWithShop({
      url: `/pages/shop/product-detail?shopId=${id}&member=1`,
      shop: detail.value,
      success(res) {
        var _a;
        (_a = res.eventChannel) == null ? void 0 : _a.emit("product", product);
      }
    });
  }
  common_vendor.watch(
    shopId,
    (id) => {
      detailLoadToken += 1;
      const token = detailLoadToken;
      resetPageState();
      detail.value = null;
      errorMsg.value = "";
      if (!id) {
        loading.value = false;
        errorMsg.value = "店铺信息无效";
        return;
      }
      const cached = utils_shopPageContext.peekShopDetail(id);
      if (cached && api_modules_shop.isUsableShopDetail(cached)) {
        loading.value = false;
        errorMsg.value = "";
        finishDetailLoad(cached);
        void loadMemberDetailExtras(token);
        return;
      }
      loading.value = true;
      void loadDetailFallback(token);
    },
    { immediate: true }
  );
  common_vendor.watch(promo.availableCoupons, (list) => {
    if (!list.length) {
      promo.couponModalVisible.value = false;
    }
  });
  return {
    detail,
    loading,
    errorMsg,
    products,
    productCategories,
    productsLoaded,
    productsLoading,
    carouselImages,
    shopAddress,
    shopPhone,
    remainingPoints,
    canPayQrcode,
    showManageLink,
    showScanVerifyBtn,
    callShopPhone,
    goManage,
    goPayQrcode,
    scanForVerify,
    applyShop,
    reload: loadDetailFallback,
    syncCartFromCache: cart.syncCartFromCache,
    formatProductPoints,
    openProductDetail,
    formatActivityTimeRange: utils_activityDisplay.formatActivityTimeRange,
    formatActivityRule: utils_activityDisplay.formatActivityRule,
    ...promo,
    ...cart
  };
}
exports.useMemberShopDetail = useMemberShopDetail;
