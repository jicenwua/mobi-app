"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "shop-manage-product-form",
  props: {
    visible: { type: Boolean, default: false },
    editingProductId: { type: [String, Number], default: null },
    form: { type: Object, required: true },
    categoryLabels: { type: Array, default: () => [] },
    hasCategories: { type: Boolean, default: false }
  },
  emits: ["close", "submit", "category-change", "pick-image", "clear-image", "preview-image", "open-category-form"],
  setup(__props) {
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.visible
      }, __props.visible ? common_vendor.e({
        b: common_vendor.t(__props.editingProductId ? "修改商品" : "添加商品"),
        c: __props.form.imagePath
      }, __props.form.imagePath ? {
        d: __props.form.imagePath,
        e: common_vendor.o(($event) => _ctx.$emit("clear-image"), "90"),
        f: common_vendor.o(($event) => _ctx.$emit("preview-image"), "a5")
      } : {
        g: common_vendor.o(($event) => _ctx.$emit("pick-image"), "e0")
      }, {
        h: common_vendor.o(($event) => _ctx.$emit("open-category-form"), "27"),
        i: __props.hasCategories
      }, __props.hasCategories ? {
        j: common_vendor.t(__props.categoryLabels[__props.form.categoryIndex]),
        k: __props.categoryLabels,
        l: __props.form.categoryIndex,
        m: common_vendor.o(($event) => _ctx.$emit("category-change", $event), "c3")
      } : {
        n: common_vendor.o(($event) => _ctx.$emit("open-category-form"), "2e")
      }, {
        o: __props.form.name,
        p: common_vendor.o(($event) => __props.form.name = $event.detail.value, "1b"),
        q: __props.form.description,
        r: common_vendor.o(($event) => __props.form.description = $event.detail.value, "1d"),
        s: __props.form.points,
        t: common_vendor.o(($event) => __props.form.points = $event.detail.value, "12"),
        v: !__props.editingProductId
      }, !__props.editingProductId ? {
        w: __props.form.stock,
        x: common_vendor.o(($event) => __props.form.stock = $event.detail.value, "07")
      } : {}, {
        y: __props.editingProductId
      }, __props.editingProductId ? {
        z: __props.form.addStock,
        A: common_vendor.o(($event) => __props.form.addStock = $event.detail.value, "85")
      } : {}, {
        B: common_vendor.o(($event) => _ctx.$emit("close"), "fe"),
        C: common_vendor.t(__props.editingProductId ? "保存修改" : "添加商品"),
        D: common_vendor.o(($event) => _ctx.$emit("submit"), "a8"),
        E: common_vendor.o(() => {
        }, "25"),
        F: common_vendor.o(() => {
        }, "34"),
        G: common_vendor.o(($event) => _ctx.$emit("close"), "32")
      }) : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-931d1570"]]);
wx.createComponent(Component);
