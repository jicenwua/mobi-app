"use strict";
const PRODUCT_STATUS = {
  SOLD_OUT: 0,
  ON_SALE: 1,
  OFF_SHELF: 2
};
const PRODUCT_STATUS_MASK = {
  [PRODUCT_STATUS.SOLD_OUT]: "/static/售罄标签.png",
  [PRODUCT_STATUS.OFF_SHELF]: "/static/下架.png"
};
function productStatusLabel(status) {
  if (status === PRODUCT_STATUS.SOLD_OUT)
    return "已售完";
  if (status === PRODUCT_STATUS.OFF_SHELF)
    return "已下架";
  if (status === PRODUCT_STATUS.ON_SALE)
    return "出售中";
  return "—";
}
function productStatusMask(status) {
  return PRODUCT_STATUS_MASK[status] || "";
}
function isProductOffShelf(status) {
  return status === PRODUCT_STATUS.OFF_SHELF;
}
function isProductOnSale(status) {
  return status === PRODUCT_STATUS.ON_SALE;
}
function productDisplaySortRank(product) {
  const status = product == null ? void 0 : product.status;
  if (status === PRODUCT_STATUS.OFF_SHELF)
    return 2;
  if (status === PRODUCT_STATUS.SOLD_OUT)
    return 1;
  const stock = product == null ? void 0 : product.stock;
  if (stock != null && stock !== "" && Number(stock) === 0)
    return 1;
  return 0;
}
function sortProductsForDisplay(products) {
  const list = Array.isArray(products) ? [...products] : [];
  return list.sort((a, b) => {
    const rankDiff = productDisplaySortRank(a) - productDisplaySortRank(b);
    if (rankDiff !== 0)
      return rankDiff;
    return ((a == null ? void 0 : a.productId) ?? 0) - ((b == null ? void 0 : b.productId) ?? 0);
  });
}
exports.PRODUCT_STATUS = PRODUCT_STATUS;
exports.isProductOffShelf = isProductOffShelf;
exports.isProductOnSale = isProductOnSale;
exports.productStatusLabel = productStatusLabel;
exports.productStatusMask = productStatusMask;
exports.sortProductsForDisplay = sortProductsForDisplay;
