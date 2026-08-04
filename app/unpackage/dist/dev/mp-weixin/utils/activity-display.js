"use strict";
const utils_datetimeFormat = require("./datetime-format.js");
function formatActivityTimeRange(activity) {
  const start = utils_datetimeFormat.formatDateTime(activity == null ? void 0 : activity.startTime, { maxLen: 16 });
  const end = utils_datetimeFormat.formatDateTime(activity == null ? void 0 : activity.endTime, { maxLen: 16 });
  if (start === "—" && end === "—")
    return "—";
  return `${start} 至 ${end}`;
}
function formatActivityRule(rule, activityType) {
  const threshold = (rule == null ? void 0 : rule.thresholdAmount) ?? (rule == null ? void 0 : rule.thresholdPoints);
  const gift = rule == null ? void 0 : rule.giftPoints;
  if (threshold == null || gift == null)
    return "规则配置不完整";
  const action = activityType === 2 ? "消费满" : "充值满";
  return `${action} ${threshold} 积分，赠送 ${gift} 积分`;
}
exports.formatActivityRule = formatActivityRule;
exports.formatActivityTimeRange = formatActivityTimeRange;
