/** 店长角色码（与后端 ShopUserRole.MANAGER 一致） */
export const SHOP_ROLE_MANAGER = 1
/** 店员 */
export const SHOP_ROLE_CLERK = 2
/** 顾客 */
export const SHOP_ROLE_CUSTOMER = 3

function normalizeRoleCode(role) {
	const n = Number(role)
	return Number.isNaN(n) ? null : n
}

/** 当前用户是否为该店铺的店长 */
export function isShopManager(shop) {
	return normalizeRoleCode(shop?.role) === SHOP_ROLE_MANAGER
}

/** 是否具备店员身份（含店长、店员；兼容历史 role=4） */
export function isShopClerkCapable(shop) {
	const role = normalizeRoleCode(shop?.role)
	return role === SHOP_ROLE_MANAGER || role === SHOP_ROLE_CLERK || role === 4
}

/** 是否具备顾客身份（兼容历史 role=4） */
export function isShopCustomerCapable(shop) {
	const role = normalizeRoleCode(shop?.role)
	return role === SHOP_ROLE_CUSTOMER || role === 4
}

const ROLE_LABEL_MAP = {
	[SHOP_ROLE_MANAGER]: '店长',
	[SHOP_ROLE_CLERK]: '店员',
	[SHOP_ROLE_CUSTOMER]: '顾客',
	4: '顾客'
}

/** 用户在店铺中的角色展示文案 */
export function getShopRoleLabel(shop) {
	const role = normalizeRoleCode(shop?.role)
	if (role == null) return ''
	return ROLE_LABEL_MAP[role] || ''
}

/** 角色标签样式类型：manager / clerk / customer */
export function getShopRoleTagType(shop) {
	const role = normalizeRoleCode(shop?.role)
	if (role === SHOP_ROLE_MANAGER) return 'manager'
	if (role === SHOP_ROLE_CUSTOMER || role === 4) return 'customer'
	if (role === SHOP_ROLE_CLERK) return 'clerk'
	return ''
}
