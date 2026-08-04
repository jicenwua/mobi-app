"use strict";
function calcGiftFromRules(basePoints, rules) {
  if (!basePoints || basePoints <= 0 || !Array.isArray(rules) || !rules.length)
    return 0;
  let bestGift = 0;
  let bestThreshold = -1;
  for (const rule of rules) {
    const threshold = Number(rule == null ? void 0 : rule.thresholdAmount);
    const gift = Number(rule == null ? void 0 : rule.giftPoints);
    if (Number.isNaN(threshold) || Number.isNaN(gift))
      continue;
    if (basePoints >= threshold && threshold >= bestThreshold) {
      bestThreshold = threshold;
      bestGift = gift;
    }
  }
  return bestGift;
}
function calcRechargeGiftPoints(basePoints, ongoingActivities) {
  if (!basePoints || basePoints <= 0 || !Array.isArray(ongoingActivities))
    return 0;
  let bestGift = 0;
  for (const activity of ongoingActivities) {
    if ((activity == null ? void 0 : activity.activityType) !== 1)
      continue;
    const gift = calcGiftFromRules(basePoints, activity.rules);
    if (gift > bestGift)
      bestGift = gift;
  }
  return bestGift;
}
function calcBasePointsFromYuan(amountYuan, ratio) {
  const yuan = Number(amountYuan);
  const rate = Number(ratio);
  if (Number.isNaN(yuan) || yuan <= 0 || Number.isNaN(rate) || rate <= 0)
    return 0;
  return Math.round(yuan * rate);
}
function previewRechargePoints(amountYuan, ratio, ongoingActivities) {
  const basePoints = calcBasePointsFromYuan(amountYuan, ratio);
  const giftPoints = calcRechargeGiftPoints(basePoints, ongoingActivities);
  return {
    basePoints,
    giftPoints,
    totalPoints: basePoints + giftPoints
  };
}
exports.previewRechargePoints = previewRechargePoints;
