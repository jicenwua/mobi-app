const CTX_KEY_PREFIX = 'shop_purchase_ctx_v1_'

function ctxKey(shopId) {
	return `${CTX_KEY_PREFIX}${shopId}`
}

/**
 * 进入确认订单页前写入店铺与商品快照（避免重复请求商品目录）。
 * @param {string|number} shopId
 * @param {{ shopName?: string, ratio?: number, products?: Record<string, object>, remainingPoints?: number }} context
 */
export function setShopPurchaseContext(shopId, context) {
	if (!shopId || !context) return
	const remaining = Number(context.remainingPoints)
	try {
		uni.setStorageSync(ctxKey(shopId), {
			shopName: context.shopName || '',
			ratio: Number(context.ratio) > 0 ? Number(context.ratio) : 1,
			products: context.products && typeof context.products === 'object' ? context.products : {},
			remainingPoints: Number.isNaN(remaining) ? 0 : remaining
		})
	} catch {
		/* 忽略存储失败 */
	}
}

/** 读取确认订单页所需的店铺与商品快照 */
export function getShopPurchaseContext(shopId) {
	if (!shopId) return null
	try {
		const raw = uni.getStorageSync(ctxKey(shopId))
		if (!raw || typeof raw !== 'object') return null
		return {
			shopName: raw.shopName || '',
			ratio: Number(raw.ratio) > 0 ? Number(raw.ratio) : 1,
			products: raw.products && typeof raw.products === 'object' ? raw.products : {},
			remainingPoints: Number(raw.remainingPoints)
		}
	} catch {
		return null
	}
}

export function clearShopPurchaseContext(shopId) {
	if (!shopId) return
	try {
		uni.removeStorageSync(ctxKey(shopId))
	} catch {
		/* 忽略 */
	}
}

/** 将商品分类列表扁平化为 { [productId]: product } */
export function flattenProductCategories(categories) {
	const map = {}
	if (!Array.isArray(categories)) return map
	for (const cat of categories) {
		const list = Array.isArray(cat?.products) ? cat.products : []
		for (const product of list) {
			if (product?.productId != null) {
				map[String(product.productId)] = product
			}
		}
	}
	return map
}
