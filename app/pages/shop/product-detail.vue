<template>
	<view class="page" :class="{ 'page--member': memberMode && product, 'page--manager': isManager && product }">
		<view v-if="!product" class="state-wrap">
			<text class="state-text">商品不存在</text>
		</view>
		<template v-else>
			<scroll-view class="scroll" scroll-y>
				<view class="hero-wrap">
					<view class="hero-image-box">
						<image
							v-if="product.imageUrl"
							:src="product.imageUrl"
							mode="aspectFill"
							class="hero-image"
						/>
						<view v-else class="hero-image hero-image--placeholder">
							<text class="hero-placeholder-text">品</text>
						</view>
						<image
							v-if="statusMaskSrc"
							:src="statusMaskSrc"
							mode="aspectFill"
							class="hero-image-mask"
						/>
					</view>
				</view>

				<view class="info-card">
					<view class="price-banner">
						<view class="price-main">
							<text class="price-num">{{ formatPoints(product.price) }}</text>
							<text class="price-unit">积分</text>
						</view>
						<text
							class="status-tag"
							:class="{
								'status-tag--soldout': product.status === PRODUCT_STATUS.SOLD_OUT,
								'status-tag--offshelf': product.status === PRODUCT_STATUS.OFF_SHELF
							}"
						>{{ productStatusLabel(product.status) }}</text>
					</view>

					<text class="product-title">{{ product.productName || '—' }}</text>
					<view class="stats-row">
						<text class="stats-text">已售 {{ formatSold(product.soldCount) }}</text>
						<template v-if="!memberMode">
							<text class="stats-sep">·</text>
							<text class="stats-text">库存 {{ formatStock(product.stock) }}</text>
						</template>
					</view>
				</view>

				<view v-if="product.description" class="detail-section">
					<text class="detail-section-title">商品详情</text>
					<text class="detail-desc">{{ product.description }}</text>
				</view>
			</scroll-view>

			<view v-if="memberMode && product" class="member-buy-bar">
				<view
					v-if="!cartQty"
					class="member-cart-btn"
					:class="{ 'member-cart-btn--disabled': !canPurchase }"
					hover-class="tap-hover-opacity-strong"
					:hover-stay-time="70"
					@click="addToCart"
				>
					<uni-icons type="cart-filled" :size="16" color="#ffffff" />
					<text class="member-cart-btn-text">加入购物车</text>
				</view>
				<view v-else class="member-qty-row">
					<view class="member-qty-btn" @click="changeCartQty(-1)">
						<text class="member-qty-glyph">−</text>
					</view>
					<text class="member-qty-num">{{ cartQty }}</text>
					<view
						class="member-qty-btn"
						:class="{ 'member-qty-btn--disabled': !canIncrease }"
						@click="changeCartQty(1)"
					>
						<text class="member-qty-glyph">+</text>
					</view>
				</view>
			</view>

			<view v-if="isManager" class="manager-bar">
				<view class="manager-btn manager-btn--ghost" @click="onEdit">
					<text class="manager-btn-text">修改</text>
				</view>
				<view
					class="manager-btn"
					:class="isOffShelf ? 'manager-btn--primary' : 'manager-btn--warn'"
					@click="onToggleShelf"
				>
					<text class="manager-btn-text">{{ isOffShelf ? '上架' : '下架' }}</text>
				</view>
				<view class="manager-btn manager-btn--danger" @click="onDelete">
					<text class="manager-btn-text">删除</text>
				</view>
			</view>

			<ProductEditSheet
				v-model:visible="editVisible"
				:shop-id="shopId"
				:product="product"
				@saved="onProductSaved"
			/>
		</template>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { deleteManageProduct, updateManageProductStatus } from '@/api/modules/shop-manage.js'
import { isShopManager } from '@/utils/shop-role.js'
import { bindOpenerShop, getOpenerEventChannel } from '@/utils/shop-page-context.js'
import ProductEditSheet from '@/components/shop/product-edit-sheet.vue'
import {
	PRODUCT_STATUS,
	isProductOffShelf,
	isProductOnSale,
	productStatusLabel,
	productStatusMask
} from '@/utils/product-status.js'
import { changeShopCartQty, getShopCartQty } from '@/utils/shop-cart-cache.js'
import { navigateBackDelayed } from '@/utils/navigation.js'

const shopId = ref('')
const product = ref(null)
const shopInfo = ref(null)
const isManager = ref(false)
const memberMode = ref(false)
const editVisible = ref(false)
const shelfSubmitting = ref(false)
const cartQty = ref(0)

const isOffShelf = computed(() => isProductOffShelf(product.value?.status))
const statusMaskSrc = computed(() => productStatusMask(product.value?.status))

const canPurchase = computed(() => {
	const p = product.value
	if (!p || !isProductOnSale(p.status)) return false
	const stock = p.stock
	if (stock != null && stock !== '' && Number(stock) !== -1 && Number(stock) <= 0) return false
	return true
})

const canIncrease = computed(() => {
	if (!canPurchase.value) return false
	const stock = product.value?.stock
	if (stock == null || stock === '' || Number(stock) === -1) return true
	return cartQty.value < Number(stock)
})

onLoad((options) => {
	shopId.value = options?.shopId ? String(options.shopId) : ''
	memberMode.value = options?.member === '1' && options?.fromManage !== '1'
	const channel = getOpenerEventChannel()
	channel?.on('product', (data) => {
		if (data) product.value = data
		refreshCartQty()
	})
	bindOpenerShop((data) => {
		shopInfo.value = data
		isManager.value = isShopManager(data)
		if (isManager.value) memberMode.value = false
	})
	if (options?.fromManage === '1') {
		isManager.value = true
		memberMode.value = false
	}
	refreshCartQty()
})

onShow(() => {
	refreshCartQty()
})

function refreshCartQty() {
	cartQty.value = getShopCartQty(shopId.value, product.value?.productId)
}

function addToCart() {
	if (!canPurchase.value) {
		uni.showToast({ title: '该商品暂不可购买', icon: 'none' })
		return
	}
	changeCartQty(1)
}

function changeCartQty(delta) {
	if (!product.value?.productId || !shopId.value) return
	if (delta > 0 && !canIncrease.value) {
		uni.showToast({ title: '库存不足', icon: 'none' })
		return
	}
	changeShopCartQty(shopId.value, product.value.productId, delta)
	refreshCartQty()
	if (delta > 0 && cartQty.value === 1) {
		uni.showToast({ title: '已加入购物车', icon: 'success', duration: 1200 })
	}
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

function onEdit() {
	if (!product.value?.productId || !shopId.value) return
	editVisible.value = true
}

function onProductSaved(patch) {
	if (!product.value) return
	product.value = { ...product.value, ...patch }
}

function onToggleShelf() {
	if (!product.value?.productId || shelfSubmitting.value) return
	const offShelf = isOffShelf.value
	const title = offShelf ? '上架商品' : '下架商品'
	const content = offShelf
		? `确定将「${product.value.productName}」重新上架吗？`
		: `确定将「${product.value.productName}」下架吗？下架后顾客将无法兑换。`
	uni.showModal({
		title,
		content,
		success: async (r) => {
			if (!r.confirm) return
			shelfSubmitting.value = true
			const nextStatus = offShelf ? PRODUCT_STATUS.ON_SALE : PRODUCT_STATUS.OFF_SHELF
			const res = await updateManageProductStatus(product.value.productId, nextStatus)
			shelfSubmitting.value = false
			if (!res.ok) {
				uni.showToast({ title: res.msg || '操作失败', icon: 'none' })
				return
			}
			product.value = { ...product.value, status: nextStatus }
			const pages = getCurrentPages()
			const page = pages[pages.length - 1]
			page?.getOpenerEventChannel?.()?.emit('product-updated', {
				productId: product.value.productId,
				status: nextStatus
			})
			uni.showToast({ title: offShelf ? '已上架' : '已下架', icon: 'success' })
		}
	})
}

function onDelete() {
	if (!product.value?.productId) return
	uni.showModal({
		title: '删除商品',
		content: `确定删除「${product.value.productName}」吗？`,
		success: async (r) => {
			if (!r.confirm) return
			const res = await deleteManageProduct(product.value.productId)
			if (!res.ok) {
				uni.showToast({ title: res.msg || '删除失败', icon: 'none' })
				return
			}
			uni.showToast({ title: '已删除', icon: 'success' })
			navigateBackDelayed(400)
		}
	})
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	background: #f5f5f5;
	display: flex;
	flex-direction: column;
}

.scroll {
	flex: 1;
	height: 0;
	box-sizing: border-box;
}

.page--member .scroll,
.page--manager .scroll {
	padding-bottom: calc(72px + env(safe-area-inset-bottom));
}

.hero-wrap {
	background: #fff;
}

.hero-image-box {
	position: relative;
	width: 100%;
}

.hero-image {
	width: 100%;
	height: 240px;
	display: block;
	background: #f0f3f7;
}

.hero-image-mask {
	position: absolute;
	inset: 0;
	width: 100%;
	height: 100%;
	z-index: 1;
	pointer-events: none;
}

.hero-image--placeholder {
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, #5ee0a0 0%, #34c759 100%);
}

.hero-placeholder-text {
	font-size: 48px;
	font-weight: 700;
	color: #fff;
}

.info-card {
	margin: -20px 12px 0;
	padding: 16px;
	background: #fff;
	border-radius: 12px;
	position: relative;
	z-index: 1;
	box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.price-banner {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 10px;
}

.price-main {
	display: flex;
	align-items: baseline;
}

.price-num {
	font-size: 28px;
	font-weight: 800;
	color: #e64340;
	line-height: 1;
}

.price-unit {
	margin-left: 4px;
	font-size: 14px;
	font-weight: 600;
	color: #e64340;
}

.status-tag {
	font-size: 12px;
	color: #34c759;
	background: rgba(52, 199, 89, 0.1);
	padding: 3px 8px;
	border-radius: 4px;
}

.status-tag--soldout {
	color: #666666;
	background: #f0f0f0;
}

.status-tag--offshelf {
	color: #b8860b;
	background: rgba(255, 149, 0, 0.12);
}

.product-title {
	display: block;
	font-size: 18px;
	font-weight: 700;
	color: #333;
	line-height: 1.4;
	margin-bottom: 8px;
}

.stats-row {
	display: flex;
	align-items: center;
	gap: 6px;
}

.stats-text {
	font-size: 13px;
	color: #666666;
}

.stats-sep {
	font-size: 13px;
	color: #999999;
}

.detail-section {
	margin: 12px;
	padding: 14px 16px;
	background: #fff;
	border-radius: 12px;
}

.detail-section-title {
	display: block;
	font-size: 15px;
	font-weight: 600;
	color: #333;
	margin-bottom: 10px;
}

.detail-desc {
	display: block;
	font-size: 14px;
	color: #666;
	line-height: 1.6;
	white-space: pre-wrap;
}

.manager-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	display: flex;
	gap: 12px;
	padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
	background: #fff;
	border-top: 1px solid #eee;
	box-sizing: border-box;
	z-index: 10;
}

.manager-btn {
	flex: 1;
	height: 44px;
	border-radius: 10px;
	display: flex;
	align-items: center;
	justify-content: center;
	min-width: 0;
}

.manager-btn--ghost {
	background: #f0f3f7;
}

.manager-btn--danger {
	background: #fff;
	border: 1px solid #e64340;
}

.manager-btn--warn {
	background: #fff7e6;
	border: 1px solid #ff9500;
}

.manager-btn--primary {
	background: #f0f3f7;
}

.manager-btn-text {
	font-size: 15px;
	font-weight: 600;
}

.manager-btn--ghost .manager-btn-text {
	color: #333;
}

.manager-btn--danger .manager-btn-text {
	color: #e64340;
}

.manager-btn--warn .manager-btn-text {
	color: #ff9500;
}

.state-wrap {
	padding: 80px 16px;
	text-align: center;
}

.state-text {
	font-size: 14px;
	color: #666666;
}

.member-buy-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 10;
	padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
	background: #fff;
	border-top: 1px solid #eee;
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: center;
}

.member-cart-btn {
	width: 100%;
	height: 44px;
	border-radius: 22px;
	background: linear-gradient(135deg, #f5c842 0%, #f0a818 100%);
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 8px;
	box-shadow: 0 3px 10px rgba(240, 168, 24, 0.35);
}

.member-cart-btn--disabled {
	opacity: 0.45;
	pointer-events: none;
}

.member-cart-btn-text {
	color: #fff;
	font-size: 15px;
	font-weight: 600;
}

.member-qty-row {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 20px;
	width: 100%;
}

.member-qty-btn {
	width: 36px;
	height: 36px;
	border-radius: 18px;
	background: rgba(240, 168, 24, 0.14);
	display: flex;
	align-items: center;
	justify-content: center;
}

.member-qty-btn--disabled {
	opacity: 0.35;
	pointer-events: none;
}

.member-qty-glyph {
	font-size: 18px;
	line-height: 1;
	color: #e09b10;
}

.member-qty-num {
	min-width: 24px;
	text-align: center;
	font-size: 18px;
	font-weight: 700;
	color: #333;
}
</style>
