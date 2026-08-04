"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "qrcode-canvas",
  props: {
    text: { type: String, default: "" },
    size: { type: Number, default: 200 },
    canvasId: { type: String, default: "qrcode-canvas" }
  },
  setup(__props) {
    const props = __props;
    const instance = common_vendor.getCurrentInstance();
    async function draw() {
      const content = (props.text || "").trim();
      if (!content)
        return;
      await common_vendor.nextTick$1();
      const qr = new common_vendor.UQRCode();
      qr.data = content;
      qr.size = props.size;
      qr.margin = 8;
      qr.make();
      const ctx = common_vendor.index.createCanvasContext(props.canvasId, instance == null ? void 0 : instance.proxy);
      qr.canvasContext = ctx;
      qr.drawCanvas();
    }
    common_vendor.watch(
      () => [props.text, props.size],
      () => {
        draw();
      },
      { immediate: true }
    );
    return (_ctx, _cache) => {
      return {
        a: __props.canvasId,
        b: __props.canvasId,
        c: __props.size + "px",
        d: __props.size + "px"
      };
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-3501f0e1"]]);
wx.createComponent(Component);
