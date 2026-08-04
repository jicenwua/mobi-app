"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_pointsFormat = require("../../utils/points-format.js");
const utils_productStatus = require("../../utils/product-status.js");
if (!Array) {
  const _easycom_uni_icons2 = common_vendor.resolveComponent("uni-icons");
  _easycom_uni_icons2();
}
const _easycom_uni_icons = () => "../../uni_modules/uni-icons/components/uni-icons/uni-icons.js";
if (!Math) {
  _easycom_uni_icons();
}
const _sfc_main = {
  __name: "member-shop-cart-bar",
  props: {
    cartLines: { type: Array, default: () => [] },
    cartTotalPointsText: { type: String, default: "0" },
    cartItemCount: { type: Number, default: 0 },
    isDark: { type: Boolean, default: false },
    checkingOut: { type: Boolean, default: false }
  },
  emits: ["checkout", "update-qty", "open-cart-detail"],
  setup(__props, { emit: __emit }) {
    const props = __props;
    const emit = __emit;
    const sheetVisible = common_vendor.ref(false);
    common_vendor.watch(
      () => props.cartItemCount,
      (count) => {
        if (count <= 0) {
          sheetVisible.value = false;
        }
      }
    );
    function canIncreaseLine(line) {
      const product = line == null ? void 0 : line.product;
      if (!product)
        return false;
      const status = product.status;
      if (status === utils_productStatus.PRODUCT_STATUS.OFF_SHELF || status === utils_productStatus.PRODUCT_STATUS.SOLD_OUT)
        return false;
      const stock = product.stock;
      if (stock == null || stock === "" || Number(stock) === -1)
        return true;
      return (Number(line.count) || 0) < Number(stock);
    }
    function onOpenCartDetail() {
      if (props.cartItemCount <= 0) {
        common_vendor.index.showToast({ title: "购物车是空的", icon: "none" });
        return;
      }
      emit("open-cart-detail");
      sheetVisible.value = true;
    }
    function emitUpdateQty(productId, delta) {
      if (productId == null)
        return;
      if (delta > 0) {
        const line = props.cartLines.find((item) => item.productId === productId);
        if (line && !canIncreaseLine(line))
          return;
      }
      emit("update-qty", { productId, delta });
    }
    function onCheckout() {
      if (props.checkingOut)
        return;
      emit("checkout");
    }
    function onCheckoutFromSheet() {
      if (props.checkingOut || !props.cartLines.length)
        return;
      emit("checkout");
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.cartItemCount > 0
      }, __props.cartItemCount > 0 ? common_vendor.e({
        b: common_vendor.p({
          type: "cart-filled",
          size: 22,
          color: "#ffffff"
        }),
        c: common_vendor.t(__props.cartItemCount > 99 ? "99+" : __props.cartItemCount),
        d: common_vendor.o(onOpenCartDetail, "73"),
        e: common_vendor.t(__props.cartTotalPointsText),
        f: common_vendor.p({
          type: "cart-filled",
          size: 16,
          color: "#ffffff"
        }),
        g: common_vendor.t(__props.checkingOut ? "处理中…" : "购买"),
        h: __props.checkingOut ? 1 : "",
        i: __props.checkingOut ? "none" : "tap-hover-opacity-strong",
        j: common_vendor.o(onCheckout, "d3"),
        k: common_vendor.n(__props.isDark ? "purchase-bar--dark" : "purchase-bar--light"),
        l: sheetVisible.value
      }, sheetVisible.value ? common_vendor.e({
        m: common_vendor.n(__props.isDark ? "cart-sheet-close--dark" : "cart-sheet-close--light"),
        n: common_vendor.o(($event) => sheetVisible.value = false, "cc"),
        o: __props.cartLines.length
      }, __props.cartLines.length ? {
        p: common_vendor.f(__props.cartLines, (line, k0, i0) => {
          return {
            a: common_vendor.t(line.productName),
            b: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(line.linePoints)),
            c: common_vendor.o(($event) => emitUpdateQty(line.productId, -1), line.productId),
            d: common_vendor.t(line.count),
            e: !canIncreaseLine(line) ? 1 : "",
            f: canIncreaseLine(line) ? "tap-hover-opacity" : "none",
            g: common_vendor.o(($event) => emitUpdateQty(line.productId, 1), line.productId),
            h: line.productId
          };
        })
      } : {}, {
        q: common_vendor.t(__props.cartTotalPointsText),
        r: common_vendor.p({
          type: "cart-filled",
          size: 16,
          color: "#ffffff"
        }),
        s: common_vendor.t(__props.checkingOut ? "处理中…" : "购买"),
        t: !__props.cartLines.length || __props.checkingOut ? 1 : "",
        v: common_vendor.o(onCheckoutFromSheet, "af"),
        w: common_vendor.n(__props.isDark ? "cart-sheet--dark" : "cart-sheet--light"),
        x: common_vendor.o(() => {
        }, "3a"),
        y: common_vendor.o(($event) => sheetVisible.value = false, "b6")
      }) : {}) : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-43ef0f3e"]]);
wx.createComponent(Component);
