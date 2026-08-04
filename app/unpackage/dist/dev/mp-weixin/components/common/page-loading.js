"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "page-loading",
  props: {
    text: { type: String, default: "加载中…" },
    /** 列表/区块内占位 */
    inline: { type: Boolean, default: true },
    /** 全屏半透明遮罩 */
    overlay: { type: Boolean, default: false },
    /** 底部「加载更多」等紧凑场景 */
    compact: { type: Boolean, default: false },
    size: { type: Number, default: 32 },
    color: { type: String, default: "#007aff" },
    overlayBg: { type: String, default: "" }
  },
  setup(__props) {
    const props = __props;
    const rootStyle = common_vendor.computed(() => {
      if (!props.overlay || !props.overlayBg)
        return {};
      return { backgroundColor: props.overlayBg };
    });
    const spinnerStyle = common_vendor.computed(() => ({
      width: `${props.size}px`,
      height: `${props.size}px`,
      borderColor: `${props.color}26`,
      borderTopColor: props.color
    }));
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.s(spinnerStyle.value),
        b: __props.text
      }, __props.text ? {
        c: common_vendor.t(__props.text)
      } : {}, {
        d: __props.inline && !__props.overlay ? 1 : "",
        e: __props.overlay ? 1 : "",
        f: __props.compact ? 1 : "",
        g: common_vendor.s(rootStyle.value)
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-c3ec4776"]]);
wx.createComponent(Component);
