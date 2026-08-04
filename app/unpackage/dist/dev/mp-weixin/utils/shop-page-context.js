"use strict";
const common_vendor = require("../common/vendor.js");
const api_modules_shop = require("../api/modules/shop.js");
const utils_shopDetailCache = require("./shop-detail-cache.js");
function rememberShopDetail(shop) {
  const normalized = api_modules_shop.normalizeShopDetail(shop);
  if (!normalized)
    return null;
  utils_shopDetailCache.setCachedShopDetail(normalized.id, normalized);
  return normalized;
}
function peekShopDetail(shopId) {
  return utils_shopDetailCache.getCachedShopDetail(shopId);
}
function getOpenerEventChannel() {
  var _a;
  const pages = getCurrentPages();
  const page = pages[pages.length - 1];
  return (_a = page == null ? void 0 : page.getOpenerEventChannel) == null ? void 0 : _a.call(page);
}
function bindOpenerShop(onShop) {
  const channel = getOpenerEventChannel();
  if (!channel || typeof onShop !== "function")
    return;
  channel.on("shop", (data) => {
    const normalized = rememberShopDetail(data);
    if (normalized)
      onShop(normalized);
  });
}
function navigateWithShop({
  url,
  shop,
  animationType = "slide-in-right",
  animationDuration = 200,
  success,
  ...rest
}) {
  const normalized = shop ? rememberShopDetail(shop) : null;
  return common_vendor.index.navigateTo({
    url,
    animationType,
    animationDuration,
    ...rest,
    success(res) {
      var _a;
      if (normalized)
        (_a = res.eventChannel) == null ? void 0 : _a.emit("shop", normalized);
      success == null ? void 0 : success(res);
    }
  });
}
exports.bindOpenerShop = bindOpenerShop;
exports.getOpenerEventChannel = getOpenerEventChannel;
exports.navigateWithShop = navigateWithShop;
exports.peekShopDetail = peekShopDetail;
exports.rememberShopDetail = rememberShopDetail;
