/** 商品目录可视区域高度（约可展示 5 个商品行 + 分类标题） */
export const PRODUCT_CATALOG_VIEWPORT_HEIGHT = 500

export function productCatalogViewportStyle(height = PRODUCT_CATALOG_VIEWPORT_HEIGHT) {
	return { height: `${height}px` }
}
