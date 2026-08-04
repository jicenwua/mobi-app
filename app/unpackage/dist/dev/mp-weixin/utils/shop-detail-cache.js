"use strict";
const cache = /* @__PURE__ */ new Map();
function setCachedShopDetail(shopId, detail) {
  if (shopId == null || shopId === "" || !detail)
    return;
  cache.set(String(shopId), detail);
}
function getCachedShopDetail(shopId) {
  if (shopId == null || shopId === "")
    return null;
  return cache.get(String(shopId)) ?? null;
}
exports.getCachedShopDetail = getCachedShopDetail;
exports.setCachedShopDetail = setCachedShopDetail;
