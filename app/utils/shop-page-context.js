import { normalizeShopDetail } from '@/api/modules/shop.js'
import { setCachedShopDetail, getCachedShopDetail } from '@/utils/shop-detail-cache.js'

/** 写入会话缓存并返回规范化后的店铺详情 */
export function rememberShopDetail(shop) {
	const normalized = normalizeShopDetail(shop)
	if (!normalized) return null
	setCachedShopDetail(normalized.id, normalized)
	return normalized
}

/** 读取已缓存的店铺详情（不发起请求） */
export function peekShopDetail(shopId) {
	return getCachedShopDetail(shopId)
}

export function getOpenerEventChannel() {
	const pages = getCurrentPages()
	const page = pages[pages.length - 1]
	return page?.getOpenerEventChannel?.()
}

/** 监听上一页经 eventChannel 传入的店铺，并写入缓存 */
export function bindOpenerShop(onShop) {
	const channel = getOpenerEventChannel()
	if (!channel || typeof onShop !== 'function') return
	channel.on('shop', (data) => {
		const normalized = rememberShopDetail(data)
		if (normalized) onShop(normalized)
	})
}

/** 跳转子页并传递店铺上下文（先写缓存，再 emit） */
export function navigateWithShop({
	url,
	shop,
	animationType = 'slide-in-right',
	animationDuration = 200,
	success,
	...rest
}) {
	const normalized = shop ? rememberShopDetail(shop) : null
	return uni.navigateTo({
		url,
		animationType,
		animationDuration,
		...rest,
		success(res) {
			if (normalized) res.eventChannel?.emit('shop', normalized)
			success?.(res)
		}
	})
}
