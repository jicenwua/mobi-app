"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "segmented-tabs",
  props: {
    tabs: { type: Array, default: () => [] },
    modelValue: { type: String, default: "" }
  },
  emits: ["update:modelValue"],
  setup(__props) {
    const props = __props;
    const sliderStyle = common_vendor.computed(() => {
      const idx = Math.max(
        0,
        props.tabs.findIndex((t) => t.key === props.modelValue)
      );
      const count = props.tabs.length || 1;
      const width = 100 / count;
      return {
        width: `calc(${width}% - 3px)`,
        transform: `translateX(${idx * 100}%)`
      };
    });
    return (_ctx, _cache) => {
      return {
        a: common_vendor.f(__props.tabs, (tab, k0, i0) => {
          return {
            a: common_vendor.t(tab.label),
            b: tab.key,
            c: __props.modelValue === tab.key ? 1 : "",
            d: common_vendor.o(($event) => _ctx.$emit("update:modelValue", tab.key), tab.key)
          };
        }),
        b: common_vendor.s(sliderStyle.value)
      };
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-0601b7af"]]);
wx.createComponent(Component);
