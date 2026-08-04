"use strict";
const api_constants_customer = require("../api/constants/customer.js");
const utils_permissions = require("./permissions.js");
const utils_shopRole = require("./shop-role.js");
function canShowShopTab() {
  return utils_permissions.hasPermission(api_constants_customer.WX_PERM.SHOP_LIST);
}
function canAddShop() {
  return utils_permissions.hasPermission(api_constants_customer.WX_PERM.SHOP_ADD);
}
function canShowMemberTab() {
  return utils_permissions.hasPermission(api_constants_customer.WX_PERM.USER);
}
function canPreviewShopByCode() {
  return utils_permissions.hasPermission(api_constants_customer.WX_PERM.USER_QUERY);
}
function canJoinShop() {
  return utils_permissions.hasPermission(api_constants_customer.WX_PERM.USER_ENTER);
}
function canGeneratePayQrcode() {
  return utils_permissions.hasAnyPermission(api_constants_customer.WX_PERM.PAY_QRCODE, api_constants_customer.WX_PERM.USER);
}
function canStaffVerifyAtShop(shop) {
  return utils_shopRole.isShopClerkCapable(shop);
}
function canManageShop() {
  return utils_permissions.hasPermission(api_constants_customer.WX_PERM.SHOP_LIST);
}
exports.canAddShop = canAddShop;
exports.canGeneratePayQrcode = canGeneratePayQrcode;
exports.canJoinShop = canJoinShop;
exports.canManageShop = canManageShop;
exports.canPreviewShopByCode = canPreviewShopByCode;
exports.canShowMemberTab = canShowMemberTab;
exports.canShowShopTab = canShowShopTab;
exports.canStaffVerifyAtShop = canStaffVerifyAtShop;
