"use strict";
const common_vendor = require("../common/vendor.js");
const utils_navigation = require("./navigation.js");
const LEGACY_PERMISSIONS_KEY = "user_permissions";
const sessionPermissions = common_vendor.ref([]);
const permRevision = common_vendor.ref(0);
function getPermRevision() {
  return permRevision;
}
function purgeLegacyPermissionsStorage() {
  try {
    common_vendor.index.removeStorageSync(LEGACY_PERMISSIONS_KEY);
  } catch {
  }
}
purgeLegacyPermissionsStorage();
function normalizePermissions(raw) {
  if (!raw)
    return [];
  if (Array.isArray(raw))
    return raw.map((p) => String(p)).filter(Boolean);
  if (typeof raw === "object") {
    const flat = [];
    for (const val of Object.values(raw)) {
      if (Array.isArray(val)) {
        flat.push(...val.map((p) => String(p)));
      } else if (typeof val === "string" && val) {
        flat.push(val);
      }
    }
    return flat.filter(Boolean);
  }
  return [];
}
function setSessionPermissions(raw) {
  sessionPermissions.value = normalizePermissions(raw);
  permRevision.value += 1;
  return sessionPermissions.value;
}
function clearSessionPermissions() {
  sessionPermissions.value = [];
  permRevision.value += 1;
}
function hasPermission(perm) {
  if (!perm)
    return false;
  const list = sessionPermissions.value;
  return list.includes(perm) || list.includes("*:*:*");
}
function hasAnyPermission(...perms) {
  return perms.some((p) => hasPermission(p));
}
function assertPermission(perm, message = "无操作权限") {
  if (hasPermission(perm))
    return true;
  common_vendor.index.showToast({ title: message, icon: "none" });
  utils_navigation.navigateBackDelayed(800, 1);
  return false;
}
exports.assertPermission = assertPermission;
exports.clearSessionPermissions = clearSessionPermissions;
exports.getPermRevision = getPermRevision;
exports.hasAnyPermission = hasAnyPermission;
exports.hasPermission = hasPermission;
exports.setSessionPermissions = setSessionPermissions;
