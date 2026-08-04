/** 商品售卖状态：0-售完 1-出售中 2-下架 */
export const PRODUCT_STATUS = {
	SOLD_OUT: 0,
	ON_SALE: 1,
	OFF_SHELF: 2
}

export const PRODUCT_STATUS_MASK = {
	[PRODUCT_STATUS.SOLD_OUT]: '/static/售罄标签.png',
	[PRODUCT_STATUS.OFF_SHELF]: '/static/下架.png'
}

export function productStatusLabel(status) {
	if (status === PRODUCT_STATUS.SOLD_OUT) return '已售完'
	if (status === PRODUCT_STATUS.OFF_SHELF) return '已下架'
	if (status === PRODUCT_STATUS.ON_SALE) return '出售中'
	return '—'
}

export function productStatusMask(status) {
	return PRODUCT_STATUS_MASK[status] || ''
}

export function isProductOffShelf(status) {
	return status === PRODUCT_STATUS.OFF_SHELF
}

export function isProductSoldOut(status) {
	return status === PRODUCT_STATUS.SOLD_OUT
}

export function isProductOnSale(status) {
	return status === PRODUCT_STATUS.ON_SALE
}

/**
 * 展示排序权重：0-有库存 1-售罄 2-下架
 */
export function productDisplaySortRank(product) {
	const status = product?.status
	if (status === PRODUCT_STATUS.OFF_SHELF) return 2
	if (status === PRODUCT_STATUS.SOLD_OUT) return 1
	const stock = product?.stock
	if (stock != null && stock !== '' && Number(stock) === 0) return 1
	return 0
}

/** 分类内商品展示排序：有库存 → 售罄 → 下架 */
export function sortProductsForDisplay(products) {
	const list = Array.isArray(products) ? [...products] : []
	return list.sort((a, b) => {
		const rankDiff = productDisplaySortRank(a) - productDisplaySortRank(b)
		if (rankDiff !== 0) return rankDiff
		return (a?.productId ?? 0) - (b?.productId ?? 0)
	})
}
