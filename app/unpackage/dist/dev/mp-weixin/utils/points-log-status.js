"use strict";
const POINTS_LOG_STATUS = {
  VERIFIED: 1,
  CANCELLED: 2,
  EXPIRED: 3,
  RECHARGE: 4,
  PENDING_USE: 5,
  ADMIN_ADD: 6,
  ADMIN_DEDUCT: 7
};
const POINTS_LOG_STATUS_LABELS = {
  [POINTS_LOG_STATUS.VERIFIED]: "已核销",
  [POINTS_LOG_STATUS.CANCELLED]: "已退款",
  [POINTS_LOG_STATUS.EXPIRED]: "订单过期",
  [POINTS_LOG_STATUS.RECHARGE]: "积分充值",
  [POINTS_LOG_STATUS.PENDING_USE]: "待使用",
  [POINTS_LOG_STATUS.ADMIN_ADD]: "后台添加",
  [POINTS_LOG_STATUS.ADMIN_DEDUCT]: "后台扣除"
};
function pointsLogStatusLabel(status) {
  return POINTS_LOG_STATUS_LABELS[status] || "—";
}
const POINTS_LOG_STATUS_TAG = {
  [POINTS_LOG_STATUS.VERIFIED]: "verified",
  [POINTS_LOG_STATUS.CANCELLED]: "cancelled",
  [POINTS_LOG_STATUS.EXPIRED]: "expired",
  [POINTS_LOG_STATUS.RECHARGE]: "recharge",
  [POINTS_LOG_STATUS.PENDING_USE]: "pending",
  [POINTS_LOG_STATUS.ADMIN_ADD]: "admin-add",
  [POINTS_LOG_STATUS.ADMIN_DEDUCT]: "admin-deduct"
};
function pointsLogStatusTagClass(status) {
  return POINTS_LOG_STATUS_TAG[status] || "unknown";
}
function isPointsLogGain(row) {
  if (!row)
    return false;
  if (row.actionType === 1)
    return true;
  const status = row.status;
  return status === POINTS_LOG_STATUS.RECHARGE || status === POINTS_LOG_STATUS.ADMIN_ADD;
}
exports.POINTS_LOG_STATUS = POINTS_LOG_STATUS;
exports.isPointsLogGain = isPointsLogGain;
exports.pointsLogStatusLabel = pointsLogStatusLabel;
exports.pointsLogStatusTagClass = pointsLogStatusTagClass;
