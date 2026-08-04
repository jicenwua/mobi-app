"use strict";
const SHOP_ROLE_MANAGER = 1;
const SHOP_ROLE_CLERK = 2;
const SHOP_ROLE_CUSTOMER = 3;
function normalizeRoleCode(role) {
  const n = Number(role);
  return Number.isNaN(n) ? null : n;
}
function isShopManager(shop) {
  return normalizeRoleCode(shop == null ? void 0 : shop.role) === SHOP_ROLE_MANAGER;
}
function isShopClerkCapable(shop) {
  const role = normalizeRoleCode(shop == null ? void 0 : shop.role);
  return role === SHOP_ROLE_MANAGER || role === SHOP_ROLE_CLERK || role === 4;
}
const ROLE_LABEL_MAP = {
  [SHOP_ROLE_MANAGER]: "店长",
  [SHOP_ROLE_CLERK]: "店员",
  [SHOP_ROLE_CUSTOMER]: "顾客",
  4: "顾客"
};
function getShopRoleLabel(shop) {
  const role = normalizeRoleCode(shop == null ? void 0 : shop.role);
  if (role == null)
    return "";
  return ROLE_LABEL_MAP[role] || "";
}
function getShopRoleTagType(shop) {
  const role = normalizeRoleCode(shop == null ? void 0 : shop.role);
  if (role === SHOP_ROLE_MANAGER)
    return "manager";
  if (role === SHOP_ROLE_CUSTOMER || role === 4)
    return "customer";
  if (role === SHOP_ROLE_CLERK)
    return "clerk";
  return "";
}
exports.getShopRoleLabel = getShopRoleLabel;
exports.getShopRoleTagType = getShopRoleTagType;
exports.isShopClerkCapable = isShopClerkCapable;
exports.isShopManager = isShopManager;
