"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "shop-detail",
  setup(__props) {
    common_vendor.onLoad((options) => {
      const id = (options == null ? void 0 : options.id) ? String(options.id) : "";
      if (!id) {
        common_vendor.index.redirectTo({ url: "/pages/main/main" });
        return;
      }
      common_vendor.index.redirectTo({ url: `/pages/main/main?shopId=${id}` });
    });
    return (_ctx, _cache) => {
      return {};
    };
  }
};
wx.createPage(_sfc_main);
