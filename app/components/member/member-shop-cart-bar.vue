<template>
	<view v-if="cartItemCount > 0">
		<view
			v-show="!sheetVisible"
			class="purchase-bar"
			:class="[
				isDark ? 'purchase-bar--dark' : 'purchase-bar--light',
				embedded ? 'purchase-bar--embedded' : ''
			]"
		>
			<view
				class="purchase-bar-cart"
				hover-class="tap-hover-opacity-strong"
				:hover-stay-time="70"
				@click="onOpenCartDetail"
			>
				<view class="purchase-bar-cart-icon">
					<uni-icons type="cart-filled" :size="22" color="#ffffff" />
					<view class="purchase-bar-cart-badge">
						<text class="purchase-bar-cart-badge-text">{{ cartItemCount > 99 ? '99+' : cartItemCount }}</text>
					</view>
				</view>
			</view>
			<view class="purchase-bar-info">
				<text class="purchase-bar-label">合计</text>
				<view class="purchase-bar-points">
					<text class="purchase-bar-num">{{ cartTotalPointsText }}</text>
					<text class="purchase-bar-unit">积分</text>
				</view>
			</view>
			<view
				class="purchase-bar-btn"
				:class="{ 'purchase-bar-btn--disabled': checkingOut }"
				:hover-class="checkingOut ? 'none' : 'tap-hover-opacity-strong'"
				:hover-stay-time="70"
				@click="onCheckout"
			>
				<uni-icons type="cart-filled" :size="16" color="#ffffff" />
				<text class="purchase-bar-btn-text">{{ checkingOut ? '处理中…' : '购买' }}</text>
			</view>
		</view>

		<view
			v-if="sheetVisible"
			class="modal-mask cart-sheet-mask"
			:class="embedded ? 'cart-sheet-mask--embedded' : ''"
			@click="sheetVisible = false"
		>
			<view
				class="cart-sheet"
				:class="[
					isDark ? 'cart-sheet--dark' : 'cart-sheet--light',
					embedded ? 'cart-sheet--embedded' : ''
				]"
				@click.stop
			>
				<view class="cart-sheet-header">
					<text class="cart-sheet-title">购物车</text>
					<text
						class="cart-sheet-close"
						:class="isDark ? 'cart-sheet-close--dark' : 'cart-sheet-close--light'"
						hover-class="tap-hover-opacity"
						:hover-stay-time="70"
						@click="sheetVisible = false"
					>×</text>
				</view>
				<scroll-view v-if="cartLines.length" class="cart-sheet-list" scroll-y>
					<view v-for="line in cartLines" :key="line.productId" class="cart-line">
						<view class="cart-line-info">
							<text class="cart-line-name">{{ line.productName }}</text>
							<text class="cart-line-points">{{ formatPointsAmount(line.linePoints) }} 积分</text>
						</view>
						<view class="cart-line-qty">
							<view
								class="cart-line-qty-btn"
								hover-class="tap-hover-opacity"
								:hover-stay-time="70"
								@click="emitUpdateQty(line.productId, -1)"
							>
								<text class="cart-line-qty-glyph">−</text>
							</view>
							<text class="cart-line-qty-num">{{ line.count }}</text>
							<view
								class="cart-line-qty-btn"
								:class="{ 'cart-line-qty-btn--disabled': !canIncreaseLine(line) }"
								:hover-class="canIncreaseLine(line) ? 'tap-hover-opacity' : 'none'"
								:hover-stay-time="70"
								@click="emitUpdateQty(line.productId, 1)"
							>
								<text class="cart-line-qty-glyph">+</text>
							</view>
						</view>
					</view>
				</scroll-view>
				<view v-else class="cart-sheet-empty">
					<text class="cart-sheet-empty-text">购物车是空的</text>
				</view>
				<view class="cart-sheet-footer">
					<view class="cart-sheet-total">
						<text class="cart-sheet-total-label">合计</text>
						<text class="cart-sheet-total-num">{{ cartTotalPointsText }} 积分</text>
					</view>
					<view
						class="cart-sheet-buy-btn"
						:class="{ 'cart-sheet-buy-btn--disabled': !cartLines.length || checkingOut }"
						@click="onCheckoutFromSheet"
					>
						<uni-icons type="cart-filled" :size="16" color="#ffffff" />
						<text class="cart-sheet-buy-text">{{ checkingOut ? '处理中…' : '购买' }}</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { formatPointsAmount } from '@/utils/points-format.js'
import { PRODUCT_STATUS } from '@/utils/product-status.js'

const props = defineProps({
	cartLines: { type: Array, default: () => [] },
	cartTotalPointsText: { type: String, default: '0' },
	cartItemCount: { type: Number, default: 0 },
	isDark: { type: Boolean, default: false },
	checkingOut: { type: Boolean, default: false },
	/** 嵌入主屏时相对容器定位，避免遮挡底部 Tab */
	embedded: { type: Boolean, default: false }
})

const emit = defineEmits(['checkout', 'update-qty', 'open-cart-detail'])

const sheetVisible = ref(false)

watch(
	() => props.cartItemCount,
	(count) => {
		if (count <= 0) {
			sheetVisible.value = false
		}
	}
)

function canIncreaseLine(line) {
	const product = line?.product
	if (!product) return false
	const status = product.status
	if (status === PRODUCT_STATUS.OFF_SHELF || status === PRODUCT_STATUS.SOLD_OUT) return false
	const stock = product.stock
	if (stock == null || stock === '' || Number(stock) === -1) return true
	return (Number(line.count) || 0) < Number(stock)
}

function onOpenCartDetail() {
	if (props.cartItemCount <= 0) {
		uni.showToast({ title: '购物车是空的', icon: 'none' })
		return
	}
	emit('open-cart-detail')
	sheetVisible.value = true
}

function emitUpdateQty(productId, delta) {
	if (productId == null) return
	if (delta > 0) {
		const line = props.cartLines.find((item) => item.productId === productId)
		if (line && !canIncreaseLine(line)) return
	}
	emit('update-qty', { productId, delta })
}

function onCheckout() {
	if (props.checkingOut) return
	emit('checkout')
}

function onCheckoutFromSheet() {
	if (props.checkingOut || !props.cartLines.length) return
	emit('checkout')
}
</script>

<style scoped>
.purchase-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 20;
	display: flex;
	align-items: center;
	gap: 12px;
	padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
	box-shadow: 0 -4px 20px rgba(15, 23, 42, 0.08);
}

.purchase-bar--light {
	background: #fff;
}

.purchase-bar--dark {
	background: #1c1c24;
	box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.28);
}

.purchase-bar--embedded {
	bottom: calc(48px + env(safe-area-inset-bottom));
	padding-bottom: 12px;
	z-index: 15;
}

.purchase-bar-cart {
	flex-shrink: 0;
}

.purchase-bar-cart-icon {
	position: relative;
	width: 44px;
	height: 44px;
	border-radius: 22px;
	background: linear-gradient(135deg, #f5c842 0%, #f0a818 100%);
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 3px 10px rgba(240, 168, 24, 0.35);
}

.purchase-bar-cart-badge {
	position: absolute;
	top: -4px;
	right: -4px;
	min-width: 18px;
	height: 18px;
	padding: 0 5px;
	border-radius: 9px;
	background: #e64340;
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	border: 2px solid #fff;
}

.purchase-bar--dark .purchase-bar-cart-badge {
	border-color: #1c1c24;
}

.purchase-bar-cart-badge-text {
	font-size: 10px;
	color: #fff;
	font-weight: 700;
	line-height: 1;
}

.purchase-bar-info {
	flex: 1;
	display: flex;
	align-items: baseline;
	gap: 8px;
	min-width: 0;
}

.purchase-bar-label {
	font-size: 14px;
	opacity: 0.7;
}

.purchase-bar-points {
	display: flex;
	align-items: baseline;
	gap: 4px;
}

.purchase-bar-num {
	font-size: 22px;
	font-weight: 800;
	color: #e64340;
	line-height: 1;
}

.purchase-bar-unit {
	font-size: 12px;
	color: #e64340;
}

.purchase-bar-btn {
	flex-shrink: 0;
	min-width: 108px;
	height: 40px;
	padding: 0 18px;
	border-radius: 20px;
	background: linear-gradient(135deg, #f5c842 0%, #f0a818 100%);
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 6px;
	box-shadow: 0 3px 10px rgba(240, 168, 24, 0.35);
}

.purchase-bar-btn--disabled {
	opacity: 0.55;
	pointer-events: none;
}

.purchase-bar-btn-text {
	color: #fff;
	font-size: 15px;
	font-weight: 600;
}

.modal-mask {
	position: fixed;
	inset: 0;
	background: rgba(0, 0, 0, 0.45);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 100;
	padding: 24px;
	box-sizing: border-box;
}

.cart-sheet-mask {
	align-items: flex-end;
	padding: 0;
}

.cart-sheet-mask--embedded {
	bottom: calc(48px + env(safe-area-inset-bottom));
}

.cart-sheet {
	width: 100%;
	max-height: 72vh;
	border-radius: 16px 16px 0 0;
	padding: 16px 16px calc(16px + env(safe-area-inset-bottom));
	box-sizing: border-box;
	display: flex;
	flex-direction: column;
}

.cart-sheet--light {
	background: #fff;
}

.cart-sheet--dark {
	background: #1c1c24;
	color: #ececf0;
}

.cart-sheet--embedded {
	padding-bottom: 16px;
}

.cart-sheet-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 12px;
}

.cart-sheet-title {
	font-size: 17px;
	font-weight: 600;
}

.cart-sheet-close {
	font-size: 24px;
	line-height: 1;
	padding: 4px 8px;
}

.cart-sheet-close--light {
	color: #666666;
}

.cart-sheet-close--dark {
	color: #a0a0a0;
}

.cart-sheet-list {
	flex: 1;
	max-height: 42vh;
	min-height: 120px;
}

.cart-line {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
	padding: 12px 0;
	border-bottom: 1px solid rgba(128, 128, 128, 0.12);
}

.cart-line-info {
	flex: 1;
	min-width: 0;
}

.cart-line-name {
	display: block;
	font-size: 15px;
	font-weight: 600;
	margin-bottom: 4px;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.cart-line-points {
	font-size: 13px;
	color: #e64340;
}

.cart-line-qty {
	display: flex;
	align-items: center;
	gap: 10px;
	flex-shrink: 0;
}

.cart-line-qty-btn {
	width: 28px;
	height: 28px;
	border-radius: 14px;
	background: rgba(240, 168, 24, 0.14);
	display: flex;
	align-items: center;
	justify-content: center;
}

.cart-line-qty-btn--disabled {
	opacity: 0.35;
	pointer-events: none;
}

.cart-line-qty-glyph {
	font-size: 16px;
	color: #e09b10;
	line-height: 1;
}

.cart-line-qty-num {
	min-width: 18px;
	text-align: center;
	font-size: 14px;
	font-weight: 600;
}

.cart-sheet-empty {
	padding: 40px 0;
	text-align: center;
}

.cart-sheet-empty-text {
	font-size: 14px;
	opacity: 0.5;
}

.cart-sheet-footer {
	margin-top: 14px;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
}

.cart-sheet-total {
	display: flex;
	align-items: baseline;
	gap: 6px;
}

.cart-sheet-total-label {
	font-size: 14px;
	opacity: 0.7;
}

.cart-sheet-total-num {
	font-size: 18px;
	font-weight: 700;
	color: #e64340;
}

.cart-sheet-buy-btn {
	flex-shrink: 0;
	min-width: 108px;
	height: 40px;
	padding: 0 18px;
	border-radius: 20px;
	background: linear-gradient(135deg, #f5c842 0%, #f0a818 100%);
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 6px;
}

.cart-sheet-buy-btn--disabled {
	opacity: 0.55;
	pointer-events: none;
}

.cart-sheet-buy-text {
	color: #fff;
	font-size: 15px;
	font-weight: 600;
}
</style>
