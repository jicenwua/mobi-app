"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_points = require("../../api/modules/points.js");
const utils_memberOrdersCache = require("../../utils/member-orders-cache.js");
const composables_useTheme = require("../../composables/use-theme.js");
const _sfc_main = {
  __name: "purchase-success",
  setup(__props) {
    const shopId = common_vendor.ref("");
    const logId = common_vendor.ref("");
    const { isDark, loadTheme } = composables_useTheme.useTheme();
    const loading = common_vendor.ref(true);
    const detail = common_vendor.ref(null);
    const errorMsg = common_vendor.ref("");
    common_vendor.onLoad((options) => {
      loadTheme();
      shopId.value = (options == null ? void 0 : options.shopId) ? String(options.shopId) : "";
      logId.value = (options == null ? void 0 : options.logId) ? String(options.logId) : "";
      if (!shopId.value || !logId.value) {
        loading.value = false;
        errorMsg.value = "订单信息无效";
        return;
      }
      utils_memberOrdersCache.invalidateMemberOrdersCache();
      void loadDetail();
    });
    async function loadDetail() {
      loading.value = true;
      const res = await api_modules_points.fetchConsumeLogDetail(Number(logId.value), Number(shopId.value));
      loading.value = false;
      if (!res.ok || !res.data) {
        errorMsg.value = res.msg || "加载订单失败";
        detail.value = null;
        return;
      }
      detail.value = res.data;
    }
    function goOrders() {
      const sid = shopId.value;
      const lid = logId.value;
      if (!sid || !lid) {
        common_vendor.index.reLaunch({ url: "/pages/main/main?tab=orders" });
        return;
      }
      common_vendor.index.navigateTo({
        url: `/pages/member/order-detail?shopId=${sid}&logId=${lid}`,
        animationType: "slide-in-right",
        animationDuration: 200
      });
    }
    function goBackShop() {
      const id = shopId.value;
      if (!id) {
        common_vendor.index.navigateBack();
        return;
      }
      common_vendor.index.navigateBack({
        delta: 1,
        fail: () => {
          common_vendor.index.redirectTo({ url: `/pages/main/main?shopId=${id}` });
        }
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: loading.value
      }, loading.value ? {} : detail.value ? common_vendor.e({
        c: common_vendor.t(detail.value.shopName || "—"),
        d: common_vendor.t(detail.value.consumePoints ?? 0),
        e: common_vendor.t(detail.value.remainingPoints ?? 0),
        f: detail.value.items && detail.value.items.length
      }, detail.value.items && detail.value.items.length ? {
        g: common_vendor.f(detail.value.items, (item, k0, i0) => {
          return {
            a: common_vendor.t(item.productName),
            b: common_vendor.t(item.count),
            c: common_vendor.t(item.linePoints ?? 0),
            d: item.receiptId
          };
        })
      } : {}, {
        h: common_vendor.o(goOrders, "8e"),
        i: common_vendor.o(goBackShop, "40")
      }) : {
        j: common_vendor.t(errorMsg.value || "订单信息无效")
      }, {
        b: detail.value,
        k: common_vendor.n(common_vendor.unref(isDark) ? "page--dark" : "page--light")
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-22ccdaa7"]]);
wx.createPage(MiniProgramPage);
