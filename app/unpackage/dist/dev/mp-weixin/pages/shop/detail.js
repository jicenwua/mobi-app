"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_shopPageContext = require("../../utils/shop-page-context.js");
if (!Math) {
  ShopDetailContent();
}
const ShopDetailContent = () => "../../components/shop/shop-detail-content.js";
const _sfc_main = {
  __name: "detail",
  setup(__props) {
    const shopId = common_vendor.ref("");
    const detailRef = common_vendor.ref(null);
    function applyShopToDetail(data) {
      common_vendor.nextTick$1(() => {
        var _a;
        (_a = detailRef.value) == null ? void 0 : _a.applyShop(data);
      });
    }
    common_vendor.onLoad((options) => {
      shopId.value = (options == null ? void 0 : options.id) ? String(options.id) : "";
      if (!shopId.value) {
        applyShopToDetail(null);
        return;
      }
      const cached = utils_shopPageContext.peekShopDetail(shopId.value);
      if (cached) {
        applyShopToDetail(cached);
      }
      utils_shopPageContext.bindOpenerShop((data) => {
        applyShopToDetail(data);
      });
    });
    return (_ctx, _cache) => {
      return {
        a: common_vendor.sr(detailRef, "0c8ebaaa-0", {
          "k": "detailRef"
        }),
        b: common_vendor.p({
          ["shop-id"]: shopId.value
        })
      };
    };
  }
};
wx.createPage(_sfc_main);
