"use strict";
const common_vendor = require("../common/vendor.js");
function navigateBackDelayed(ms = 800, delta = 1) {
  setTimeout(() => common_vendor.index.navigateBack({ delta }), ms);
}
exports.navigateBackDelayed = navigateBackDelayed;
