"use strict";
const PRODUCT_CATALOG_VIEWPORT_HEIGHT = 500;
function productCatalogViewportStyle(height = PRODUCT_CATALOG_VIEWPORT_HEIGHT) {
  return { height: `${height}px` };
}
function productCatalogFlexViewportStyle(minHeight = 280) {
  return { height: "100%", minHeight: `${minHeight}px` };
}
exports.productCatalogViewportStyle = productCatalogViewportStyle;
exports.productCatalogFlexViewportStyle = productCatalogFlexViewportStyle;
