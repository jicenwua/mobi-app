"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_points = require("../../api/modules/points.js");
const api_modules_coupon = require("../../api/modules/coupon.js");
const api_modules_user = require("../../api/modules/user.js");
const utils_shopCartCache = require("../../utils/shop-cart-cache.js");
const utils_memberOrdersCache = require("../../utils/member-orders-cache.js");
const utils_shopPurchaseContext = require("../../utils/shop-purchase-context.js");
const api_constants_customer = require("../../api/constants/customer.js");
const utils_permissions = require("../../utils/permissions.js");
const utils_pointsFormat = require("../../utils/points-format.js");
const services_payment = require("../../services/payment.js");
const services_userSecurity = require("../../services/user-security.js");
const composables_useTheme = require("../../composables/use-theme.js");
const utils_requestId = require("../../utils/request-id.js");
const utils_couponDisplay = require("../../utils/coupon-display.js");
if (!Math) {
  (PayPinPad + PageLoading)();
}
const PayPinPad = () => "../../components/pay/pay-pin-pad.js";
const PageLoading = () => "../../components/common/page-loading.js";
const MAX_PIN_ERRORS = 3;
const _sfc_main = {
  __name: "purchase-confirm",
  setup(__props) {
    const shopId = common_vendor.ref("");
    const { isDark, loadTheme } = composables_useTheme.useTheme();
    const loading = common_vendor.ref(true);
    const couponsLoading = common_vendor.ref(false);
    const submitting = common_vendor.ref(false);
    const preview = common_vendor.ref(null);
    const selectedCouponId = common_vendor.ref(null);
    const errorMsg = common_vendor.ref("");
    const applicableCoupons = common_vendor.ref([]);
    const remainingPoints = common_vendor.ref(0);
    const showPinPad = common_vendor.ref(false);
    const pinError = common_vendor.ref("");
    const pinResetKey = common_vendor.ref(0);
    const pinVerifying = common_vendor.ref(false);
    let payLoadingVisible = false;
    let passwordGateDone = false;
    let skipPinOnce = false;
    let pinErrorCount = 0;
    let purchaseRequestId = "";
    const productMap = common_vendor.ref({});
    let shopName = "";
    let rechargeRatio = null;
    common_vendor.onLoad(async (options) => {
      if (!utils_permissions.assertPermission(api_constants_customer.WX_PERM.USER, "无会员访问权限"))
        return;
      loadTheme();
      shopId.value = (options == null ? void 0 : options.shopId) ? String(options.shopId) : "";
      if (!shopId.value) {
        loading.value = false;
        errorMsg.value = "店铺信息无效";
        return;
      }
      const guard = await services_payment.guardPayPassword();
      passwordGateDone = true;
      if (!guard.ok) {
        if (guard.navigatedToSet) {
          skipPinOnce = true;
        } else {
          common_vendor.index.navigateBack();
        }
        return;
      }
      await initOrderPage();
    });
    common_vendor.onShow(() => {
      if (!passwordGateDone || loading.value)
        return;
      if (skipPinOnce && services_userSecurity.hasPayPasswordSet()) {
        skipPinOnce = false;
        void initOrderPage();
      }
    });
    common_vendor.onUnload(() => {
      hidePayLoading();
    });
    function buildItemsFromCart() {
      const cart = utils_shopCartCache.getShopCart(shopId.value);
      return Object.entries(cart).filter(([, count]) => Number(count) > 0).map(([productId, count]) => ({ productId: Number(productId), count: Number(count) }));
    }
    function buildPreviewItems(cartItems) {
      return cartItems.map(({ productId, count }) => {
        const product = productMap.value[String(productId)] || {};
        const linePoints = utils_pointsFormat.calculateLinePoints(product.price, count);
        return {
          productId,
          productName: product.productName || "商品",
          imageUrl: product.imageUrl || "",
          count,
          unitPoints: utils_pointsFormat.calculateUnitPoints(product.price),
          linePoints
        };
      });
    }
    function mapCouponOptions(coupons, originalPoints, selectedId) {
      return (coupons || []).map((coupon) => {
        const applicable = utils_pointsFormat.meetsCouponThreshold(coupon.thresholdAmount, originalPoints);
        let disabledReason = null;
        if (!applicable) {
          const threshold = coupon.thresholdAmount;
          if (threshold != null && Number(threshold) > 0) {
            disabledReason = `未满 ${threshold} 积分`;
          } else {
            disabledReason = "不可用";
          }
        }
        return {
          userCouponId: coupon.userCouponId,
          couponName: coupon.couponName,
          type: coupon.type,
          thresholdAmount: coupon.thresholdAmount,
          discountValue: coupon.discountValue,
          applicable,
          disabledReason
        };
      });
    }
    function applyCouponSettlement(items, coupons, userCouponId) {
      var _a;
      const originalPoints = items.reduce((sum, item) => sum + Number(item.linePoints || 0), 0);
      if (!originalPoints) {
        return null;
      }
      const couponOptions = mapCouponOptions(coupons, originalPoints);
      const selectedCoupon = userCouponId ? couponOptions.find((c) => c.userCouponId === userCouponId) : null;
      let payablePoints = originalPoints;
      let discountPoints = 0;
      if (selectedCoupon == null ? void 0 : selectedCoupon.applicable) {
        payablePoints = utils_pointsFormat.calculateCouponPayablePoints(
          originalPoints,
          selectedCoupon.type,
          selectedCoupon.discountValue
        );
        discountPoints = utils_pointsFormat.calculateCouponDiscountPoints(originalPoints, payablePoints);
      }
      return {
        shopId: Number(shopId.value),
        shopName,
        ratio: rechargeRatio ?? ((_a = preview.value) == null ? void 0 : _a.ratio),
        items,
        originalPoints,
        discountPoints,
        payablePoints,
        selectedUserCouponId: (selectedCoupon == null ? void 0 : selectedCoupon.applicable) ? userCouponId : null,
        coupons: couponOptions
      };
    }
    function refreshPreview(userCouponId = selectedCouponId.value) {
      var _a;
      const cartItems = buildItemsFromCart();
      if (!cartItems.length) {
        preview.value = null;
        errorMsg.value = "购物车是空的";
        return false;
      }
      const items = buildPreviewItems(cartItems);
      const coupons = ((_a = preview.value) == null ? void 0 : _a.coupons) || [];
      const next = applyCouponSettlement(items, coupons, userCouponId);
      if (!next) {
        preview.value = null;
        errorMsg.value = "合计积分无效";
        return false;
      }
      preview.value = next;
      selectedCouponId.value = next.selectedUserCouponId || null;
      applicableCoupons.value = (next.coupons || []).filter((c) => c.applicable);
      errorMsg.value = "";
      return true;
    }
    async function loadShopCoupons() {
      couponsLoading.value = true;
      const res = await api_modules_coupon.fetchMyCoupons({
        shopId: Number(shopId.value),
        status: 0,
        pageNum: 1,
        pageSize: 100
      });
      couponsLoading.value = false;
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "优惠券加载失败", icon: "none" });
        return [];
      }
      return res.rows || [];
    }
    async function initOrderPage() {
      loading.value = true;
      errorMsg.value = "";
      const ctx = utils_shopPurchaseContext.getShopPurchaseContext(shopId.value);
      const cartItems = buildItemsFromCart();
      if (!cartItems.length) {
        loading.value = false;
        errorMsg.value = "购物车是空的";
        preview.value = null;
        return;
      }
      if (!ctx || !Object.keys(ctx.products || {}).length) {
        loading.value = false;
        errorMsg.value = "商品信息已失效，请返回店铺重新选择";
        preview.value = null;
        return;
      }
      productMap.value = ctx.products;
      rechargeRatio = ctx.ratio;
      shopName = ctx.shopName;
      const balance = Number(ctx.remainingPoints);
      remainingPoints.value = Number.isNaN(balance) ? 0 : balance;
      const items = buildPreviewItems(cartItems);
      const coupons = await loadShopCoupons();
      const settlement = applyCouponSettlement(items, coupons, selectedCouponId.value);
      loading.value = false;
      if (!settlement) {
        errorMsg.value = "合计积分无效";
        preview.value = null;
        return;
      }
      preview.value = settlement;
      selectedCouponId.value = settlement.selectedUserCouponId || null;
      applicableCoupons.value = (settlement.coupons || []).filter((c) => c.applicable);
    }
    function selectCoupon(coupon) {
      if (coupon && !coupon.applicable) {
        common_vendor.index.showToast({ title: coupon.disabledReason || "优惠券不可用", icon: "none" });
        return;
      }
      const nextId = (coupon == null ? void 0 : coupon.userCouponId) || null;
      if (selectedCouponId.value === nextId)
        return;
      selectedCouponId.value = nextId;
      refreshPreview(nextId);
    }
    async function submitPurchase() {
      if (submitting.value || couponsLoading.value || !preview.value)
        return;
      if (showPinPad.value)
        return;
      const payable = Number(preview.value.payablePoints);
      const available = remainingPoints.value;
      if (!Number.isNaN(payable) && payable > available) {
        common_vendor.index.showToast({
          title: `积分不足，当前剩余 ${utils_pointsFormat.formatPointsAmount(available)} 积分`,
          icon: "none"
        });
        return;
      }
      if (!purchaseRequestId) {
        purchaseRequestId = utils_requestId.createRequestId();
      }
      showPinPad.value = true;
      pinError.value = "";
      pinErrorCount = 0;
    }
    function showPayLoading() {
      if (payLoadingVisible)
        return;
      payLoadingVisible = true;
      common_vendor.index.showLoading({ title: "支付处理中…", mask: true });
    }
    function hidePayLoading() {
      if (!payLoadingVisible)
        return;
      payLoadingVisible = false;
      common_vendor.index.hideLoading();
    }
    function onPinCancel() {
      hidePayLoading();
      pinVerifying.value = false;
      showPinPad.value = false;
    }
    function recordPinError(msg) {
      pinErrorCount += 1;
      if (pinErrorCount >= MAX_PIN_ERRORS) {
        showPinPad.value = false;
        common_vendor.index.showModal({
          title: "提示",
          content: "错误次数过多，请稍后再试",
          showCancel: false
        });
        return;
      }
      const left = MAX_PIN_ERRORS - pinErrorCount;
      pinResetKey.value += 1;
      pinError.value = `${msg}（还可尝试 ${left} 次）`;
    }
    async function onPinComplete(digits) {
      if (!/^\d{6}$/.test(digits)) {
        recordPinError("请输入 6 位数字");
        return;
      }
      pinVerifying.value = true;
      await common_vendor.nextTick$1();
      showPayLoading();
      let navigated = false;
      try {
        const verify = await api_modules_user.verifyPayPassword(digits);
        if (!verify.ok) {
          if (verify.type === "forbidden") {
            showPinPad.value = false;
            common_vendor.index.showModal({
              title: "无法校验密码",
              content: verify.msg || "当前账号无权限校验支付密码，请联系管理员",
              showCancel: false
            });
            return;
          }
          if (verify.type === "network") {
            showPinPad.value = false;
            common_vendor.index.showToast({ title: verify.msg || "网络错误", icon: "none" });
            return;
          }
          recordPinError(verify.msg || "支付密码错误");
          return;
        }
        navigated = await doPurchase();
      } finally {
        if (!navigated) {
          pinVerifying.value = false;
          hidePayLoading();
        }
      }
    }
    function goPurchaseSuccessPage(logId) {
      hidePayLoading();
      const url = `/pages/member/purchase-success?shopId=${shopId.value}&logId=${logId}`;
      return new Promise((resolve) => {
        common_vendor.index.redirectTo({
          url,
          animationType: "slide-in-right",
          animationDuration: 200,
          success: () => resolve(true),
          fail: () => {
            common_vendor.index.navigateTo({
              url,
              animationType: "slide-in-right",
              animationDuration: 200,
              success: () => resolve(true),
              fail: () => {
                showPinPad.value = false;
                hidePayLoading();
                common_vendor.index.showToast({ title: "打开成功页失败，请从订单列表查看", icon: "none" });
                resolve(false);
              }
            });
          }
        });
      });
    }
    async function doPurchase() {
      var _a, _b;
      if (submitting.value || couponsLoading.value || !preview.value)
        return false;
      const items = buildItemsFromCart();
      if (!items.length) {
        common_vendor.index.showToast({ title: "购物车是空的", icon: "none" });
        return false;
      }
      const usePinOverlay = showPinPad.value;
      if (!usePinOverlay) {
        submitting.value = true;
      }
      const payload = {
        shopId: Number(shopId.value),
        items,
        requestId: purchaseRequestId || utils_requestId.createRequestId()
      };
      if (selectedCouponId.value)
        payload.userCouponId = selectedCouponId.value;
      let res;
      try {
        res = await api_modules_points.purchaseProducts(payload);
      } finally {
        if (!usePinOverlay) {
          submitting.value = false;
        }
      }
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "购买失败", icon: "none" });
        return false;
      }
      purchaseRequestId = "";
      utils_shopCartCache.clearShopCart(shopId.value);
      utils_memberOrdersCache.invalidateMemberOrdersCache();
      const logId = (_a = res.data) == null ? void 0 : _a.logId;
      if (logId) {
        await common_vendor.nextTick$1();
        return goPurchaseSuccessPage(logId);
      }
      showPinPad.value = false;
      const consumed = ((_b = res.data) == null ? void 0 : _b.consumePoints) ?? preview.value.payablePoints;
      common_vendor.index.showToast({ title: `购买成功，-${utils_pointsFormat.formatPointsAmount(consumed)} 积分`, icon: "success" });
      setTimeout(() => {
        common_vendor.index.navigateBack();
      }, 600);
      return false;
    }
    return (_ctx, _cache) => {
      var _a, _b;
      return common_vendor.e({
        a: showPinPad.value
      }, showPinPad.value ? {
        b: common_vendor.o(onPinComplete, "e0"),
        c: common_vendor.o(onPinCancel, "da"),
        d: common_vendor.p({
          title: "请输入支付密码",
          ["error-hint"]: pinError.value,
          ["reset-key"]: pinResetKey.value,
          busy: pinVerifying.value,
          ["busy-text"]: "支付处理中…"
        })
      } : loading.value ? {
        f: common_vendor.p({
          text: "加载订单信息…"
        })
      } : preview.value ? common_vendor.e({
        h: common_vendor.t(preview.value.shopName || "店铺"),
        i: preview.value.ratio
      }, preview.value.ratio ? {
        j: common_vendor.t(preview.value.ratio)
      } : {}, {
        k: common_vendor.f(preview.value.items, (item, k0, i0) => {
          return common_vendor.e({
            a: item.imageUrl
          }, item.imageUrl ? {
            b: item.imageUrl
          } : {}, {
            c: common_vendor.t(item.productName),
            d: common_vendor.t(item.count),
            e: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(item.linePoints)),
            f: item.productId
          });
        }),
        l: applicableCoupons.value.length
      }, applicableCoupons.value.length ? {
        m: common_vendor.t(applicableCoupons.value.length)
      } : {}, {
        n: couponsLoading.value
      }, couponsLoading.value ? {
        o: common_vendor.p({
          text: "加载优惠券…",
          compact: true
        })
      } : common_vendor.e({
        p: !selectedCouponId.value
      }, !selectedCouponId.value ? {} : {}, {
        q: !selectedCouponId.value ? 1 : "",
        r: common_vendor.o(($event) => selectCoupon(null), "44"),
        s: common_vendor.f(preview.value.coupons, (coupon, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.unref(utils_couponDisplay.couponIconSrc)(coupon.type),
            b: common_vendor.t(coupon.couponName || "优惠券"),
            c: common_vendor.t(common_vendor.unref(utils_couponDisplay.formatCouponDesc)(coupon)),
            d: !coupon.applicable && coupon.disabledReason
          }, !coupon.applicable && coupon.disabledReason ? {
            e: common_vendor.t(coupon.disabledReason)
          } : {}, {
            f: selectedCouponId.value === coupon.userCouponId
          }, selectedCouponId.value === coupon.userCouponId ? {} : {}, {
            g: coupon.userCouponId,
            h: selectedCouponId.value === coupon.userCouponId ? 1 : "",
            i: !coupon.applicable ? 1 : "",
            j: common_vendor.o(($event) => selectCoupon(coupon), coupon.userCouponId)
          });
        }),
        t: !((_a = preview.value.coupons) == null ? void 0 : _a.length)
      }, !((_b = preview.value.coupons) == null ? void 0 : _b.length) ? {} : {}), {
        v: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(preview.value.originalPoints)),
        w: Number(preview.value.discountPoints) > 0
      }, Number(preview.value.discountPoints) > 0 ? {
        x: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(preview.value.discountPoints))
      } : {}, {
        y: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(preview.value.payablePoints)),
        z: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(preview.value.payablePoints)),
        A: common_vendor.t(submitting.value ? "提交中…" : "确认购买"),
        B: submitting.value || couponsLoading.value ? 1 : "",
        C: common_vendor.o(submitPurchase, "75"),
        D: common_vendor.n(common_vendor.unref(isDark) ? "bottom-bar--dark" : "bottom-bar--light")
      }) : {
        E: common_vendor.t(errorMsg.value || "订单信息无效")
      }, {
        e: loading.value,
        g: preview.value,
        F: submitting.value
      }, submitting.value ? {
        G: common_vendor.p({
          overlay: true,
          inline: false,
          text: "正在提交订单…",
          ["overlay-bg"]: common_vendor.unref(isDark) ? "rgba(18, 18, 18, 0.78)" : "rgba(255, 255, 255, 0.78)",
          color: common_vendor.unref(isDark) ? "#ffb800" : "#ff9800"
        })
      } : {}, {
        H: common_vendor.n(common_vendor.unref(isDark) ? "page--dark" : "page--light")
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-5eaa8c39"]]);
wx.createPage(MiniProgramPage);
