"use strict";
function formatDateTime(iso, { empty = "—", maxLen = 19 } = {}) {
  if (!iso)
    return empty;
  return String(iso).replace("T", " ").slice(0, maxLen);
}
exports.formatDateTime = formatDateTime;
