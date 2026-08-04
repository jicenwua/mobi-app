"use strict";
const COUPON_TYPE_LABELS = { 1: "折扣", 2: "满减" };
const COUPON_ICON_MAP = {
  1: "/static/折扣券.png",
  2: "/static/满减券.png"
};
function couponIconSrc(type) {
  return COUPON_ICON_MAP[type] || COUPON_ICON_MAP[2];
}
function couponTypeLabel(type) {
  return COUPON_TYPE_LABELS[type] || "优惠";
}
function formatCouponDesc(coupon, options = {}) {
  if (!coupon)
    return "";
  const threshold = coupon.thresholdAmount;
  const value = coupon.discountValue;
  if (value == null) {
    return options.emptyValueFallback ? couponTypeLabel(coupon.type) : "";
  }
  const hasThreshold = threshold != null && Number(threshold) > 0;
  if (coupon.type === 1) {
    const fold = Number(value) / 10;
    const foldText = Number.isInteger(fold) ? String(fold) : String(Math.round(fold * 10) / 10);
    return hasThreshold ? `满 ${threshold} 积分享 ${foldText} 折` : `无门槛 ${foldText} 折`;
  }
  return hasThreshold ? `满 ${threshold} 积分减 ${value} 积分` : `无门槛减 ${value} 积分`;
}
function formatCouponRemaining(coupon) {
  if (!coupon)
    return "0";
  if (coupon.remainingQuantity != null && coupon.remainingQuantity !== "") {
    return String(coupon.remainingQuantity);
  }
  const total = coupon.totalQuantity;
  if (total == null || total === "")
    return "不限";
  const issued = coupon.issuedQuantity ?? 0;
  return String(Math.max(0, Number(total) - Number(issued)));
}
exports.couponIconSrc = couponIconSrc;
exports.couponTypeLabel = couponTypeLabel;
exports.formatCouponDesc = formatCouponDesc;
exports.formatCouponRemaining = formatCouponRemaining;
