"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_shopManage = require("../../api/modules/shop-manage.js");
const api_modules_qrcode = require("../../api/modules/qrcode.js");
const api_modules_points = require("../../api/modules/points.js");
const utils_qrcodeScan = require("../../utils/qrcode-scan.js");
require("../../utils/permissions.js");
const utils_navigation = require("../../utils/navigation.js");
const composables_useTheme = require("../../composables/use-theme.js");
const utils_requestId = require("../../utils/request-id.js");
const _sfc_main = {
  __name: "scan-checkout",
  setup(__props) {
    const { isDark, loadTheme } = composables_useTheme.useTheme();
    const shopId = common_vendor.ref("");
    const payToken = common_vendor.ref("");
    const memberInfo = common_vendor.ref(null);
    const products = common_vendor.ref([]);
    const productsLoading = common_vendor.ref(false);
    const cart = common_vendor.reactive({});
    const submitting = common_vendor.ref(false);
    let consumeRequestId = "";
    const totalPoints = common_vendor.computed(() => {
      let sum = 0;
      for (const p of products.value) {
        const qty = cart[p.productId] || 0;
        if (qty <= 0)
          continue;
        const pts = calcProductPoints(p);
        if (pts != null)
          sum += pts * qty;
      }
      return sum;
    });
    const canSubmit = common_vendor.computed(() => totalPoints.value > 0);
    common_vendor.onLoad((options) => {
      if (!(options == null ? void 0 : options.shopId)) {
        common_vendor.index.showToast({ title: "店铺信息无效", icon: "none" });
        utils_navigation.navigateBackDelayed(800);
        return;
      }
      loadTheme();
      shopId.value = (options == null ? void 0 : options.shopId) || "";
      if (shopId.value)
        loadProducts();
      const presetToken = (options == null ? void 0 : options.token) ? decodeURIComponent(String(options.token)) : "";
      if (presetToken) {
        void applyPayToken(presetToken);
      }
    });
    async function loadProducts() {
      productsLoading.value = true;
      const res = await api_modules_shopManage.fetchManageProducts(shopId.value, 1, 100);
      productsLoading.value = false;
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "加载商品失败", icon: "none" });
        return;
      }
      products.value = res.rows || [];
    }
    function calcProductPoints(product) {
      const price = product == null ? void 0 : product.price;
      if (price == null)
        return null;
      const pts = Number(price);
      return Number.isNaN(pts) ? null : Math.round(pts);
    }
    function formatProductPoints(product) {
      const pts = calcProductPoints(product);
      return pts == null ? "—" : String(pts);
    }
    function changeQty(productId, delta) {
      const cur = cart[productId] || 0;
      const next = Math.max(0, cur + delta);
      if (next === 0) {
        delete cart[productId];
      } else {
        cart[productId] = next;
      }
    }
    async function applyPayToken(token) {
      const verify = await api_modules_qrcode.verifyPayQrcode({ shopId: shopId.value, token });
      if (!verify.ok || !verify.data) {
        common_vendor.index.showToast({ title: verify.msg || "付款码无效", icon: "none" });
        return;
      }
      payToken.value = token;
      memberInfo.value = verify.data;
    }
    function scanMemberCode() {
      common_vendor.index.scanCode({
        onlyFromCamera: false,
        success: async (res) => {
          const token = utils_qrcodeScan.parsePayTokenFromScan(res.result);
          if (!token) {
            common_vendor.index.showToast({ title: "无效的付款码", icon: "none" });
            return;
          }
          await applyPayToken(token);
        },
        fail: () => {
          common_vendor.index.showToast({ title: "扫码取消", icon: "none" });
        }
      });
    }
    async function doConsume() {
      const items = Object.entries(cart).filter(([, count]) => count > 0).map(([productId, count]) => ({ productId: Number(productId), count }));
      if (!items.length) {
        common_vendor.index.showToast({ title: "请选择商品", icon: "none" });
        return;
      }
      if (!payToken.value) {
        common_vendor.index.showToast({ title: "请先扫描会员付款码", icon: "none" });
        return;
      }
      submitting.value = true;
      if (!consumeRequestId) {
        consumeRequestId = utils_requestId.createRequestId();
      }
      const res = await api_modules_points.consumePoints({
        shopId: Number(shopId.value),
        token: payToken.value,
        items,
        requestId: consumeRequestId
      });
      submitting.value = false;
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "扣款失败", icon: "none" });
        return;
      }
      consumeRequestId = "";
      common_vendor.index.showToast({ title: `扣款成功，-${res.data ?? totalPoints.value} 积分`, icon: "success" });
      utils_navigation.navigateBackDelayed(800);
    }
    async function submitConsume() {
      if (!canSubmit.value || submitting.value)
        return;
      if (!payToken.value) {
        common_vendor.index.scanCode({
          onlyFromCamera: false,
          success: async (res) => {
            const token = utils_qrcodeScan.parsePayTokenFromScan(res.result);
            if (!token) {
              common_vendor.index.showToast({ title: "无效的付款码", icon: "none" });
              return;
            }
            await applyPayToken(token);
            await doConsume();
          },
          fail: () => {
            common_vendor.index.showToast({ title: "扫码取消", icon: "none" });
          }
        });
        return;
      }
      await doConsume();
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: productsLoading.value
      }, productsLoading.value ? {} : !products.value.length ? {} : {
        c: common_vendor.f(products.value, (p, k0, i0) => {
          return {
            a: common_vendor.t(p.productName),
            b: common_vendor.t(formatProductPoints(p)),
            c: common_vendor.o(($event) => changeQty(p.productId, -1), p.productId),
            d: common_vendor.t(cart[p.productId] || 0),
            e: common_vendor.o(($event) => changeQty(p.productId, 1), p.productId),
            f: p.productId
          };
        })
      }, {
        b: !products.value.length,
        d: memberInfo.value
      }, memberInfo.value ? {
        e: common_vendor.t(memberInfo.value.nickname || "会员"),
        f: common_vendor.t(memberInfo.value.phone || ""),
        g: common_vendor.t(memberInfo.value.remainingPoints ?? 0)
      } : {
        h: common_vendor.o(scanMemberCode, "a9")
      }, {
        i: common_vendor.t(totalPoints.value),
        j: common_vendor.t(submitting.value ? "扣款中…" : payToken.value ? "确认扣款" : "扫码扣款"),
        k: submitting.value || !canSubmit.value ? 1 : "",
        l: common_vendor.o(submitConsume, "38"),
        m: common_vendor.n(common_vendor.unref(isDark) ? "page--dark" : "page--light")
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-7248b687"]]);
wx.createPage(MiniProgramPage);
