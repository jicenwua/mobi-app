"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "shop-manage-coupon-stock-sheet",
  props: {
    visible: { type: Boolean, default: false },
    target: { type: Object, default: null },
    form: { type: Object, default: () => ({ quantity: "" }) }
  },
  emits: ["close", "submit", "update:quantity"],
  setup(__props) {
    return (_ctx, _cache) => {
      var _a;
      return common_vendor.e({
        a: __props.visible
      }, __props.visible ? {
        b: common_vendor.t(((_a = __props.target) == null ? void 0 : _a.couponName) || ""),
        c: __props.form.quantity,
        d: common_vendor.o(($event) => _ctx.$emit("update:quantity", $event.detail.value), "8d"),
        e: common_vendor.o(($event) => _ctx.$emit("close"), "12"),
        f: common_vendor.o(($event) => _ctx.$emit("submit"), "0c"),
        g: common_vendor.o(() => {
        }, "ed"),
        h: common_vendor.o(($event) => _ctx.$emit("close"), "32")
      } : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-4441e7e7"]]);
wx.createComponent(Component);
