import { WX_PERM } from '@/api/constants/customer.js'
import { hasPermission, hasAnyPermission } from '@/utils/permissions.js'
import { isShopClerkCapable } from '@/utils/shop-role.js'

/** 底部「店铺」Tab、店主店铺列表、店铺管理、商品维护 */
export function canShowShopTab() {
	return hasPermission(WX_PERM.SHOP_LIST)
}

/** 添加店铺、提交开店申请（多商户能力，审核单店时可关闭） */
export function canAddShop() {
	return hasPermission(WX_PERM.SHOP_ADD)
}

/** 底部「会员」Tab、会员店内详情、顾客读接口 */
export function canShowMemberTab() {
	return hasPermission(WX_PERM.USER)
}

/** 会员页搜索/预览店铺、店主端店铺邀请码 */
export function canPreviewShopByCode() {
	return hasPermission(WX_PERM.USER_QUERY)
}

/** 会员页扫码加入店铺 */
export function canJoinShop() {
	return hasPermission(WX_PERM.USER_ENTER)
}

/** 会员出示付款码（所有会员均可，与 wx:user 能力一致） */
export function canGeneratePayQrcode() {
	return hasAnyPermission(WX_PERM.PAY_QRCODE, WX_PERM.USER)
}

/** 店主端扫码收银（全局权限，兼容旧逻辑） */
export function canShopConsume() {
	return hasPermission(WX_PERM.SHOP_CONSUME)
}

/**
 * 是否可在指定店铺扫码收银/核销订单（店长、店员，按店铺角色判断）
 * @param {object|null|undefined} shop
 */
export function canStaffVerifyAtShop(shop) {
	return isShopClerkCapable(shop)
}

/** 店铺管理页入口（商品/活动/券，店长身份由页面内 isShopManager 再校验） */
export function canManageShop() {
	return hasPermission(WX_PERM.SHOP_LIST)
}

/** 店长活动列表 Tab */
export function canViewActivity() {
	return hasPermission(WX_PERM.ACTIVITY_QUERY)
}

/** 店长活动/优惠券创建与修改 */
export function canEditActivity() {
	return hasPermission(WX_PERM.ACTIVITY_EDIT)
}

/** 店长活动/优惠券删除 */
export function canRemoveActivity() {
	return hasPermission(WX_PERM.ACTIVITY_REMOVE)
}
