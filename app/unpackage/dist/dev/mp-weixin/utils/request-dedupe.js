"use strict";
const common_vendor = require("../common/vendor.js");
const pending = /* @__PURE__ */ new Map();
const lastDoneAt = /* @__PURE__ */ new Map();
const MUTATING_METHODS = ["POST", "PUT", "PATCH", "DELETE"];
const DEFAULT_COOLDOWN_MS = 800;
class DuplicateRequestError extends Error {
  constructor(message = "请勿重复提交") {
    super(message);
    this.name = "DuplicateRequestError";
    this.duplicateRequest = true;
  }
}
function serializePart(value) {
  if (value == null)
    return "";
  if (typeof value === "string")
    return value;
  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
}
function buildRequestKey(config = {}) {
  const method = String(config.method || "GET").toUpperCase();
  const url = config.url || "";
  return [method, url, serializePart(config.data), serializePart(config.params)].join("|");
}
function isMutating(method) {
  return MUTATING_METHODS.includes(String(method || "GET").toUpperCase());
}
function notifyDuplicate(message) {
  common_vendor.index.showToast({ title: message, icon: "none", duration: 2e3 });
}
function runWithDedupe(executor, config, options = {}) {
  const { cooldownMs = DEFAULT_COOLDOWN_MS, silent = false } = options;
  const method = String(config.method || "GET").toUpperCase();
  const key = buildRequestKey(config);
  const mutate = isMutating(method);
  if (mutate) {
    const last = lastDoneAt.get(key);
    if (last != null && Date.now() - last < cooldownMs) {
      const err = new DuplicateRequestError("操作过于频繁，请稍后再试");
      if (!silent)
        notifyDuplicate(err.message);
      return Promise.reject(err);
    }
  }
  if (pending.has(key)) {
    return pending.get(key);
  }
  const promise = Promise.resolve().then(() => executor()).catch((err) => {
    if ((err == null ? void 0 : err.duplicateRequest) && !silent) {
      notifyDuplicate(err.message);
    }
    return Promise.reject(err);
  }).finally(() => {
    pending.delete(key);
    if (mutate) {
      lastDoneAt.set(key, Date.now());
    }
  });
  pending.set(key, promise);
  return promise;
}
exports.runWithDedupe = runWithDedupe;
