"use strict";
require("../common/vendor.js");
function isLocalUploadPath(path) {
  const p = String(path || "").trim();
  if (!p)
    return false;
  return p.startsWith("wxfile://") || p.startsWith("file://") || p.startsWith("http://tmp/") || !p.startsWith("http://") && !p.startsWith("https://") && !p.startsWith("//");
}
exports.isLocalUploadPath = isLocalUploadPath;
