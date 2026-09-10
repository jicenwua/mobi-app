/** 商品目录可视区域高度（独立页等固定高度场景） */
export const PRODUCT_CATALOG_VIEWPORT_HEIGHT = 500

/** 固定像素高度（独立页） */
export function productCatalogViewportStyle(height = PRODUCT_CATALOG_VIEWPORT_HEIGHT) {
	return { height: `${height}px` }
}

/** 嵌入主屏/管理页：占满父级 flex 剩余空间 */
export function productCatalogFlexViewportStyle(minHeight = 280) {
	return { height: '100%', minHeight: `${minHeight}px` }
}
