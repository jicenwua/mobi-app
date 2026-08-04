"use strict";
require("../common/vendor.js");
const WX_PLACEHOLDER_NICKNAME = "微信用户";
function isWxPlaceholderProfile({ nickName, nickname } = {}) {
  const name = (nickName || nickname || "").trim();
  return !name || name === WX_PLACEHOLDER_NICKNAME;
}
exports.isWxPlaceholderProfile = isWxPlaceholderProfile;
