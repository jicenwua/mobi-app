const CART_KEY_PREFIX = 'shop_cart_v1_'

function cartKey(shopId) {
	return `${CART_KEY_PREFIX}${shopId}`
}

/** 读取店铺购物车 { [productId]: count } */
export function getShopCart(shopId) {
	if (!shopId) return {}
	try {
		const raw = uni.getStorageSync(cartKey(shopId))
		if (!raw || typeof raw !== 'object') return {}
		const cart = {}
		for (const [id, qty] of Object.entries(raw)) {
			const n = Number(qty)
			if (n > 0) cart[id] = n
		}
		return cart
	} catch {
		return {}
	}
}

/** 写入店铺购物车 */
export function setShopCart(shopId, cart) {
	if (!shopId) return
	try {
		const normalized = {}
		if (cart && typeof cart === 'object') {
			for (const [id, qty] of Object.entries(cart)) {
				const n = Number(qty)
				if (n > 0) normalized[id] = n
			}
		}
		if (Object.keys(normalized).length) {
			uni.setStorageSync(cartKey(shopId), normalized)
		} else {
			uni.removeStorageSync(cartKey(shopId))
		}
	} catch {
		/* 忽略存储失败 */
	}
}

/** 清空店铺购物车 */
export function clearShopCart(shopId) {
	if (!shopId) return
	try {
		uni.removeStorageSync(cartKey(shopId))
	} catch {
		/* 忽略 */
	}
}

/** 调整单个商品数量，返回更新后的购物车 */
export function changeShopCartQty(shopId, productId, delta) {
	const cart = getShopCart(shopId)
	const id = String(productId)
	const cur = Number(cart[id]) || 0
	const next = cur + delta
	if (next <= 0) {
		delete cart[id]
	} else {
		cart[id] = next
	}
	setShopCart(shopId, cart)
	return cart
}

/** 读取单个商品在购物车中的数量 */
export function getShopCartQty(shopId, productId) {
	if (!shopId || productId == null) return 0
	const cart = getShopCart(shopId)
	return Number(cart[String(productId)]) || 0
}
