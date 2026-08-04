/** 会话内店铺详情缓存（含 role），避免重复请求 GET /shop */
const cache = new Map()

export function setCachedShopDetail(shopId, detail) {
	if (shopId == null || shopId === '' || !detail) return
	cache.set(String(shopId), detail)
}

export function getCachedShopDetail(shopId) {
	if (shopId == null || shopId === '') return null
	return cache.get(String(shopId)) ?? null
}

export function invalidateShopDetailCache(shopId) {
	if (shopId == null || shopId === '') return
	cache.delete(String(shopId))
}

export function clearShopDetailCache() {
	cache.clear()
}
