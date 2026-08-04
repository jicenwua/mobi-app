"use strict";
const POINTS_DECIMALS = 2;
function ceilPointsToDecimals(value, decimals = POINTS_DECIMALS) {
  const n = Number(value);
  if (Number.isNaN(n))
    return 0;
  const factor = 10 ** decimals;
  return Math.ceil(n * factor) / factor;
}
function calculateCouponPayablePoints(originalPoints, couponType, discountValue) {
  const original = Number(originalPoints);
  if (!original || original <= 0)
    return 0;
  if (!couponType || discountValue == null || Number(discountValue) <= 0) {
    return ceilPointsToDecimals(original);
  }
  if (couponType === 1) {
    return ceilPointsToDecimals(original * Number(discountValue) / 100);
  }
  if (couponType === 2) {
    return ceilPointsToDecimals(Math.max(0, original - Number(discountValue)));
  }
  return ceilPointsToDecimals(original);
}
function formatPointsAmount(value) {
  const n = Number(value);
  if (Number.isNaN(n))
    return "0.00";
  return n.toFixed(POINTS_DECIMALS);
}
function meetsCouponThreshold(thresholdAmount, originalPoints) {
  const original = Number(originalPoints);
  if (!original || original <= 0)
    return false;
  const threshold = Number(thresholdAmount);
  if (!threshold || threshold <= 0)
    return true;
  return original >= threshold;
}
function calculateCouponDiscountPoints(originalPoints, payablePoints) {
  const original = Number(originalPoints);
  const payable = Number(payablePoints);
  if (Number.isNaN(original) || Number.isNaN(payable))
    return 0;
  const discount = original - payable;
  return discount < 0 ? 0 : ceilPointsToDecimals(discount);
}
function calculateLinePoints(price, count) {
  const p = Number(price);
  const c = Number(count);
  if (Number.isNaN(p) || Number.isNaN(c) || c <= 0)
    return 0;
  return Math.round(p * c);
}
function calculateUnitPoints(price) {
  const p = Number(price);
  if (Number.isNaN(p))
    return 0;
  return Math.round(p);
}
exports.calculateCouponDiscountPoints = calculateCouponDiscountPoints;
exports.calculateCouponPayablePoints = calculateCouponPayablePoints;
exports.calculateLinePoints = calculateLinePoints;
exports.calculateUnitPoints = calculateUnitPoints;
exports.formatPointsAmount = formatPointsAmount;
exports.meetsCouponThreshold = meetsCouponThreshold;
