<template>
	<view class="catalog" :class="{ 'catalog--dark': isDark }">
		<scroll-view
			class="catalog-sidebar"
			scroll-y
			enable-flex
			:show-scrollbar="false"
			:style="sidebarScrollStyle"
			:scroll-into-view="sidebarScrollIntoView"
		>
			<view
				v-for="cat in visibleCategories"
				:key="cat.categoryId"
				:id="`sidebar-${cat.categoryId}`"
				class="sidebar-item"
				:class="{ 'sidebar-item--active': activeCategoryId === cat.categoryId }"
				hover-class="tap-hover-row"
				:hover-stay-time="70"
				@click="jumpToCategory(cat)"
			>
				<text class="sidebar-item-text">{{ cat.categoryName }}</text>
			</view>
		</scroll-view>

		<scroll-view
			id="catalog-main-scroll"
			class="catalog-main"
			scroll-y
			enable-flex
			:show-scrollbar="false"
			:style="mainScrollStyle"
			:scroll-top="mainScrollTop"
			:scroll-with-animation="scrollWithAnimation"
			@scroll="onMainScroll"
		>
			<view v-if="!visibleCategories.length" class="catalog-empty">
				<text class="catalog-empty-text">暂无商品</text>
			</view>
			<view v-else class="catalog-main-inner">
				<view
					v-for="cat in visibleCategories"
					:key="cat.categoryId"
					:id="`cat-${cat.categoryId}`"
					class="catalog-section"
				>
				<text class="catalog-section-title">{{ cat.categoryName }}</text>
				<view
					v-for="product in cat.products"
					:key="product.productId"
					class="product-card"
					hover-class="tap-hover-opacity-mid"
					:hover-stay-time="70"
					@click="onProductClick(product)"
				>
					<view class="product-thumb-wrap">
						<image
							v-if="product.imageUrl"
							:src="product.imageUrl"
							mode="aspectFill"
							class="product-thumb"
							lazy-load
						/>
						<view v-else class="product-thumb product-thumb--placeholder">
							<text class="product-thumb-text">品</text>
						</view>
						<image
							v-if="statusMask(product.status)"
							:src="statusMask(product.status)"
							mode="aspectFill"
							class="product-thumb-mask"
						/>
					</view>

					<view class="product-body">
						<text class="product-name">{{ product.productName || '—' }}</text>
						<view class="product-meta-row">
							<text class="product-meta">已售 {{ formatSold(product.soldCount) }}</text>
							<template v-if="showStock">
								<text class="product-meta-sep">·</text>
								<text class="product-meta">库存 {{ formatStock(product.stock) }}</text>
							</template>
						</view>
						<text v-if="product.description" class="product-desc">{{ product.description }}</text>
					</view>

					<view class="product-action-col">
						<view class="product-points">
							<text class="product-points-num">{{ formatLinePoints(product) }}</text>
							<text class="product-points-unit">积分</text>
						</view>
						<view v-if="purchasable" class="product-buy-wrap" @click.stop>
							<view
								v-if="!cartQty(product)"
								class="product-cart-btn"
								:class="{ 'product-cart-btn--disabled': !canPurchase(product) }"
								:hover-class="canPurchase(product) ? 'tap-hover-scale' : 'none'"
								:hover-stay-time="70"
								@click="onBuy(product)"
							>
								<uni-icons type="cart-filled" :size="14" color="#ffffff" />
							</view>
							<view v-else class="product-qty">
								<view
									class="product-qty-btn"
									hover-class="tap-hover-opacity"
									:hover-stay-time="70"
									@click="onQtyChange(product, -1)"
								>
									<text class="product-qty-glyph">−</text>
								</view>
								<text class="product-qty-num">{{ cartQty(product) }}</text>
								<view
									class="product-qty-btn"
									:class="{ 'product-qty-btn--disabled': !canIncrease(product) }"
									:hover-class="canIncrease(product) ? 'tap-hover-opacity' : 'none'"
									:hover-stay-time="70"
									@click="onQtyChange(product, 1)"
								>
									<text class="product-qty-glyph">+</text>
								</view>
							</view>
						</view>
					</view>
				</view>
			</view>
			<view class="catalog-main-spacer" :style="{ height: `${bottomSpacerHeight}px` }" />
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, getCurrentInstance } from 'vue'
import { productStatusMask, PRODUCT_STATUS, sortProductsForDisplay } from '@/utils/product-status.js'

const props = defineProps({
	categories: { type: Array, default: () => [] },
	isDark: { type: Boolean, default: false },
	/** 是否隐藏无商品的分类 */
	hideEmpty: { type: Boolean, default: true },
	/** 是否隐藏已下架商品（顾客端） */
	hideOffShelf: { type: Boolean, default: false },
	/** 是否展示库存（管理端） */
	showStock: { type: Boolean, default: false },
	/** 是否展示购买按钮与数量选择 */
	purchasable: { type: Boolean, default: false },
	/** 购物车数量映射 { [productId]: count } */
	cart: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['product-click', 'change-qty'])

const instance = getCurrentInstance()
const queryContext = computed(() => instance?.proxy || instance)

const activeCategoryId = ref(null)
const mainScrollTop = ref(0)
const sidebarScrollIntoView = ref('')
const scrollWithAnimation = ref(true)
const sectionOffsets = ref({})
const bottomSpacerHeight = ref(0)
const mainScrollHeight = ref(0)
const isProgrammaticScroll = ref(false)
let programmaticScrollTimer = null
let measureToken = 0

const sidebarScrollStyle = computed(() => ({ height: '100%' }))

const mainScrollStyle = computed(() => {
	if (mainScrollHeight.value > 0) {
		return { height: `${mainScrollHeight.value}px` }
	}
	return { height: '100%' }
})

const visibleCategories = computed(() => {
	const list = Array.isArray(props.categories) ? props.categories : []
	const sorted = [...list].sort((a, b) => {
		const sa = a?.sortOrder ?? 0
		const sb = b?.sortOrder ?? 0
		if (sa !== sb) return sa - sb
		return (a?.categoryId ?? 0) - (b?.categoryId ?? 0)
	})
	const mapped = sorted.map((cat) => {
		let products = Array.isArray(cat?.products) ? cat.products : []
		if (props.hideOffShelf) {
			products = products.filter((p) => p?.status !== PRODUCT_STATUS.OFF_SHELF)
		}
		products = sortProductsForDisplay(products)
		return { ...cat, products }
	})
	if (!props.hideEmpty) return mapped
	return mapped.filter((cat) => cat.products.length > 0)
})

watch(
	visibleCategories,
	(list) => {
		if (!list.length) {
			activeCategoryId.value = null
			sectionOffsets.value = {}
			bottomSpacerHeight.value = 0
			return
		}
		if (!list.some((c) => c.categoryId === activeCategoryId.value)) {
			activeCategoryId.value = list[0].categoryId
		}
		scheduleMeasureLayout()
	},
	{ immediate: true, deep: true }
)

onMounted(() => {
	scheduleMeasureLayout()
})

function scheduleMeasureLayout() {
	return measureLayoutWithRetry()
}

function measureLayoutWithRetry(delays = [0, 80, 180]) {
	const token = ++measureToken
	const run = async () => {
		for (const delay of delays) {
			if (token !== measureToken) return false
			if (delay > 0) {
				await new Promise((resolve) => setTimeout(resolve, delay))
			}
			await nextTick()
			const ok = await measureLayout()
			if (ok) {
				await nextTick()
				await new Promise((resolve) => setTimeout(resolve, 60))
				if (token !== measureToken) return false
				await measureLayout()
				return true
			}
		}
		return false
	}
	return run()
}

function measureLayout() {
	return new Promise((resolve) => {
		const cats = visibleCategories.value
		const context = queryContext.value
		if (!cats.length || !context) {
			resolve(false)
			return
		}

		const query = uni.createSelectorQuery().in(context)
		query.select('.catalog').boundingClientRect()
		query.select('.catalog-main-inner').boundingClientRect()
		cats.forEach((cat) => {
			query.select(`#cat-${cat.categoryId}`).boundingClientRect()
		})
		query.exec((res) => {
			const catalogRect = res?.[0]
			const innerRect = res?.[1]
			if (!catalogRect?.height || !innerRect) {
				resolve(false)
				return
			}

			mainScrollHeight.value = Math.ceil(catalogRect.height)

			const offsets = {}
			cats.forEach((cat, index) => {
				const rect = res[index + 2]
				if (rect) {
					offsets[cat.categoryId] = Math.max(0, Math.round(rect.top - innerRect.top))
				}
			})
			sectionOffsets.value = offsets

			const lastRect = res[cats.length + 1]
			const lastSectionHeight = lastRect?.height || 0
			bottomSpacerHeight.value = Math.max(0, Math.ceil(catalogRect.height - lastSectionHeight))
			resolve(Object.keys(offsets).length > 0)
		})
	})
}

function setMainScrollTop(top, animated = true) {
	const nextTop = Math.max(0, Math.round(top))
	scrollWithAnimation.value = animated
	isProgrammaticScroll.value = true
	if (programmaticScrollTimer) {
		clearTimeout(programmaticScrollTimer)
	}

	if (mainScrollTop.value === nextTop) {
		mainScrollTop.value = nextTop + 1
		nextTick(() => {
			mainScrollTop.value = nextTop
		})
	} else {
		mainScrollTop.value = nextTop
	}

	programmaticScrollTimer = setTimeout(() => {
		isProgrammaticScroll.value = false
	}, animated ? 360 : 80)
}

async function jumpToCategory(cat) {
	if (!cat?.categoryId && cat?.categoryId !== 0) return
	activeCategoryId.value = cat.categoryId

	await measureLayoutWithRetry([0, 60, 140])
	const offset = sectionOffsets.value[cat.categoryId] ?? 0
	setMainScrollTop(offset)

	sidebarScrollIntoView.value = ''
	nextTick(() => {
		sidebarScrollIntoView.value = `sidebar-${cat.categoryId}`
	})
}

function onMainScroll(e) {
	if (isProgrammaticScroll.value) return

	const scrollTop = e?.detail?.scrollTop ?? 0
	const cats = visibleCategories.value
	if (!cats.length) return

	let currentId = cats[0].categoryId
	for (let i = cats.length - 1; i >= 0; i--) {
		const cat = cats[i]
		const offset = sectionOffsets.value[cat.categoryId]
		if (offset != null && scrollTop >= offset - 8) {
			currentId = cat.categoryId
			break
		}
	}

	if (currentId === activeCategoryId.value) return
	activeCategoryId.value = currentId
	sidebarScrollIntoView.value = ''
	nextTick(() => {
		sidebarScrollIntoView.value = `sidebar-${currentId}`
	})
}

function onProductClick(product) {
	emit('product-click', product)
}

function statusMask(status) {
	return productStatusMask(status)
}

function formatStock(stock) {
	if (stock == null || stock === '') return '—'
	if (Number(stock) === -1) return '不限'
	return String(stock)
}

function formatSold(sold) {
	if (sold == null || sold === '') return '0'
	return String(sold)
}

function formatPoints(price) {
	if (price == null || price === '') return '—'
	const n = Number(price)
	if (Number.isNaN(n)) return String(price)
	return Number.isInteger(n) ? String(n) : String(Math.round(n * 100) / 100)
}

function formatLinePoints(product) {
	const price = product?.price
	if (price == null || price === '') return '—'
	return formatPoints(price)
}

function cartQty(product) {
	const id = product?.productId
	if (id == null) return 0
	return Number(props.cart?.[id]) || 0
}

function canPurchase(product) {
	const status = product?.status
	if (status === PRODUCT_STATUS.OFF_SHELF || status === PRODUCT_STATUS.SOLD_OUT) return false
	const stock = product?.stock
	if (stock != null && stock !== '' && Number(stock) !== -1 && Number(stock) <= 0) return false
	return true
}

function canIncrease(product) {
	if (!canPurchase(product)) return false
	const stock = product?.stock
	if (stock == null || stock === '' || Number(stock) === -1) return true
	return cartQty(product) < Number(stock)
}

function onBuy(product) {
	if (!canPurchase(product)) {
		uni.showToast({ title: '该商品暂不可购买', icon: 'none' })
		return
	}
	emit('change-qty', { product, delta: 1 })
}

function onQtyChange(product, delta) {
	if (delta > 0 && !canIncrease(product)) {
		uni.showToast({ title: '库存不足', icon: 'none' })
		return
	}
	const next = cartQty(product) + delta
	if (next < 0) return
	emit('change-qty', { product, delta })
}
</script>

<style scoped>
.catalog {
	display: flex;
	flex: 1;
	width: 100%;
	height: 100%;
	min-height: 0;
	align-self: stretch;
	background: #fff;
	border-radius: 14px;
	overflow: hidden;
	box-shadow: 0 2px 12px rgba(15, 23, 42, 0.05);
	border: 1px solid rgba(15, 23, 42, 0.04);
}

.catalog--dark {
	background: #1c1c24;
	box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
	border-color: rgba(255, 255, 255, 0.06);
}

.catalog-sidebar {
	width: 88px;
	flex-shrink: 0;
	align-self: stretch;
	height: 100%;
	background: #f5f6f8;
}

.catalog--dark .catalog-sidebar {
	background: #14141a;
}

.sidebar-item {
	padding: 14px 8px;
	text-align: center;
	position: relative;
}

.sidebar-item--active {
	background: #fff;
}

.catalog--dark .sidebar-item--active {
	background: #1c1c24;
}

.sidebar-item--active::before {
	content: '';
	position: absolute;
	left: 0;
	top: 50%;
	transform: translateY(-50%);
	width: 3px;
	height: 18px;
	border-radius: 0 2px 2px 0;
	background: #007aff;
}

.sidebar-item-text {
	font-size: 13px;
	line-height: 1.35;
	color: #666;
	display: -webkit-box;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	overflow: hidden;
}

.sidebar-item--active .sidebar-item-text {
	color: #333;
	font-weight: 600;
}

.catalog--dark .sidebar-item-text {
	color: #a8a8a8;
}

.catalog--dark .sidebar-item--active .sidebar-item-text {
	color: #ececf0;
}

.catalog-main {
	flex: 1;
	min-width: 0;
	min-height: 0;
	height: 100%;
	background: #fff;
}

.catalog--dark .catalog-main {
	background: #1c1c24;
}

.catalog-main-inner {
	box-sizing: border-box;
}

.catalog-main-spacer {
	width: 100%;
	flex-shrink: 0;
}

.catalog-empty {
	padding: 48px 16px;
	text-align: center;
}

.catalog-empty-text {
	font-size: 14px;
	color: #666666;
}

.catalog-section {
	padding: 0 12px 8px;
}

.catalog-section-title {
	display: block;
	padding: 12px 0 8px;
	font-size: 13px;
	font-weight: 600;
	color: #666666;
}

.catalog--dark .catalog-section-title {
	color: #a8a8a8;
}

.product-card {
	display: flex;
	align-items: flex-start;
	gap: 10px;
	padding: 10px 0;
	border-bottom: 1px solid #f0f0f0;
}

.catalog--dark .product-card {
	border-bottom-color: rgba(255, 255, 255, 0.06);
}

.product-thumb-wrap {
	position: relative;
	width: 56px;
	height: 56px;
	flex-shrink: 0;
}

.product-thumb-mask {
	position: absolute;
	inset: 0;
	width: 100%;
	height: 100%;
	border-radius: 8px;
	z-index: 1;
	pointer-events: none;
}

.product-thumb {
	width: 56px;
	height: 56px;
	border-radius: 8px;
	flex-shrink: 0;
	background: #f0f3f7;
}

.product-thumb--placeholder {
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, #5ee0a0 0%, #34c759 100%);
}

.product-thumb-text {
	font-size: 18px;
	font-weight: 700;
	color: #fff;
}

.product-body {
	flex: 1;
	min-width: 0;
}

.product-name {
	display: block;
	font-size: 15px;
	font-weight: 600;
	color: #333;
	line-height: 1.35;
	margin-bottom: 4px;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.catalog--dark .product-name {
	color: #ececf0;
}

.product-meta-row {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 4px;
	margin-bottom: 4px;
}

.product-meta {
	font-size: 11px;
	color: #666666;
}

.product-meta-sep {
	font-size: 11px;
	color: #999999;
}

.product-desc {
	display: block;
	font-size: 12px;
	color: #666666;
	line-height: 1.4;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.catalog--dark .product-desc {
	color: rgba(255, 255, 255, 0.35);
}

.product-action-col {
	flex-shrink: 0;
	display: flex;
	flex-direction: column;
	align-items: flex-end;
	gap: 8px;
	min-width: 72px;
}

.product-points {
	display: flex;
	flex-direction: column;
	align-items: flex-end;
	padding-top: 2px;
}

.product-buy-wrap {
	display: flex;
	justify-content: flex-end;
}

.product-cart-btn {
	width: 26px;
	height: 26px;
	border-radius: 13px;
	background: linear-gradient(135deg, #f5c842 0%, #f0a818 100%);
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 1px 6px rgba(240, 168, 24, 0.3);
}

.product-cart-btn--disabled {
	opacity: 0.4;
	pointer-events: none;
	box-shadow: none;
}

.product-qty {
	display: flex;
	align-items: center;
	gap: 8px;
}

.product-qty-btn {
	width: 22px;
	height: 22px;
	border-radius: 11px;
	background: rgba(240, 168, 24, 0.14);
	display: flex;
	align-items: center;
	justify-content: center;
}

.catalog--dark .product-qty-btn {
	background: rgba(245, 200, 66, 0.18);
}

.product-qty-btn--disabled {
	opacity: 0.35;
	pointer-events: none;
}

.product-qty-glyph {
	font-size: 13px;
	line-height: 1;
	color: #e09b10;
}

.catalog--dark .product-qty-glyph {
	color: #f5c842;
}

.product-qty-num {
	min-width: 14px;
	text-align: center;
	font-size: 13px;
	font-weight: 600;
	color: #333;
}

.catalog--dark .product-qty-num {
	color: #ececf0;
}

.product-points-num {
	font-size: 16px;
	font-weight: 700;
	color: #e64340;
	line-height: 1.1;
}

.product-points-unit {
	font-size: 11px;
	color: #e64340;
	margin-top: 2px;
}
</style>

