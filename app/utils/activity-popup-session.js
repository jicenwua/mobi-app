/** 活动/公告弹窗：当日关闭后至次日 0 点前不再展示（按店铺落盘） */
const STORAGE_PREFIX = 'shop_promo_popup_'

function normalizeShopId(shopId) {
	const id = shopId != null && shopId !== '' ? String(shopId).trim() : ''
	return id
}

function todayKey() {
	const d = new Date()
	const month = String(d.getMonth() + 1).padStart(2, '0')
	const day = String(d.getDate()).padStart(2, '0')
	return `${d.getFullYear()}-${month}-${day}`
}

function storageKey(shopId) {
	return `${STORAGE_PREFIX}${normalizeShopId(shopId)}`
}

/** 该店铺今日是否已关闭过活动/公告弹窗 */
export function hasShownActivityPopup(shopId) {
	const id = normalizeShopId(shopId)
	if (!id) return true
	try {
		return uni.getStorageSync(storageKey(id)) === todayKey()
	} catch {
		return false
	}
}

/** 标记该店铺今日已关闭活动/公告弹窗 */
export function markActivityPopupShown(shopId) {
	const id = normalizeShopId(shopId)
	if (!id) return
	try {
		uni.setStorageSync(storageKey(id), todayKey())
	} catch {
		/* ignore */
	}
}
