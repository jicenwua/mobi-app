<template>
	<view class="page" :class="isDark ? 'page--dark' : 'page--light'">
		<pay-pin-pad
			v-if="showPinPad"
			title="请输入支付密码"
			:error-hint="pinError"
			:reset-key="pinResetKey"
			:busy="pinVerifying"
			busy-text="支付处理中…"
			@complete="onPinComplete"
			@cancel="onPinCancel"
		/>
		<view v-else-if="loading" class="state-wrap">
			<page-loading text="加载订单信息…" />
		</view>
		<template v-else-if="preview">
			<scroll-view class="scroll" scroll-y>
				<view class="shop-card">
					<text class="shop-name">{{ preview.shopName || '店铺' }}</text>
					<text v-if="preview.ratio" class="shop-ratio">1 元 = {{ preview.ratio }} 积分</text>
				</view>

				<view class="section-card">
					<text class="section-title">商品清单</text>
					<view v-for="item in preview.items" :key="item.productId" class="goods-row">
						<image
							v-if="item.imageUrl"
							class="goods-thumb"
							:src="item.imageUrl"
							mode="aspectFill"
						/>
						<view v-else class="goods-thumb goods-thumb--placeholder">
							<text class="goods-thumb-text">图</text>
						</view>
						<view class="goods-info">
							<text class="goods-name">{{ item.productName }}</text>
							<text class="goods-meta">× {{ item.count }}</text>
						</view>
						<text class="goods-points">{{ formatPointsAmount(item.linePoints) }} 积分</text>
					</view>
				</view>

				<view class="section-card">
					<view class="section-head">
						<text class="section-title">折扣券</text>
						<text v-if="applicableCoupons.length" class="section-sub">{{ applicableCoupons.length }} 张可用</text>
					</view>

					<view v-if="couponsLoading" class="coupon-loading">
						<page-loading text="加载优惠券…" compact />
					</view>
					<template v-else>
						<view
							class="coupon-row coupon-row--none"
							:class="{ 'coupon-row--active': !selectedCouponId }"
							@click="selectCoupon(null)"
						>
							<text class="coupon-row-name">不使用优惠券</text>
							<view v-if="!selectedCouponId" class="coupon-check">✓</view>
						</view>

						<view
							v-for="coupon in preview.coupons"
							:key="coupon.userCouponId"
							class="coupon-row"
							:class="{
								'coupon-row--active': selectedCouponId === coupon.userCouponId,
								'coupon-row--disabled': !coupon.applicable
							}"
							@click="selectCoupon(coupon)"
						>
							<image class="coupon-icon" :src="couponIconSrc(coupon.type)" mode="aspectFit" />
							<view class="coupon-body">
								<text class="coupon-name">{{ coupon.couponName || '优惠券' }}</text>
								<text class="coupon-desc">{{ formatCouponDesc(coupon) }}</text>
								<text v-if="!coupon.applicable && coupon.disabledReason" class="coupon-reason">
									{{ coupon.disabledReason }}
								</text>
							</view>
							<view v-if="selectedCouponId === coupon.userCouponId" class="coupon-check">✓</view>
						</view>

						<view v-if="!preview.coupons?.length" class="coupon-empty">
							<text class="coupon-empty-text">暂无可用折扣券</text>
						</view>
					</template>
				</view>

				<view class="section-card summary-card">
					<view class="summary-row">
						<text class="summary-label">商品合计</text>
						<text class="summary-value">{{ formatPointsAmount(preview.originalPoints) }} 积分</text>
					</view>
					<view v-if="Number(preview.discountPoints) > 0" class="summary-row summary-row--discount">
						<text class="summary-label">优惠券抵扣</text>
						<text class="summary-value">−{{ formatPointsAmount(preview.discountPoints) }} 积分</text>
					</view>
					<view class="summary-row summary-row--total">
						<text class="summary-label">应付积分</text>
						<text class="summary-total">{{ formatPointsAmount(preview.payablePoints) }}</text>
					</view>
				</view>
			</scroll-view>

			<view class="bottom-bar" :class="isDark ? 'bottom-bar--dark' : 'bottom-bar--light'">
				<view class="bottom-info">
					<text class="bottom-label">合计</text>
					<view class="bottom-points">
						<text class="bottom-num">{{ formatPointsAmount(preview.payablePoints) }}</text>
						<text class="bottom-unit">积分</text>
					</view>
				</view>
				<view
					class="bottom-btn"
					:class="{ 'bottom-btn--disabled': submitting || couponsLoading }"
					@click="submitPurchase"
				>
					<text class="bottom-btn-text">{{ submitting ? '提交中…' : '确认购买' }}</text>
				</view>
			</view>
		</template>
		<view v-else class="state-wrap">
			<text class="state-text">{{ errorMsg || '订单信息无效' }}</text>
		</view>

		<page-loading
			v-if="submitting"
			overlay
			:inline="false"
			text="正在提交订单…"
			:overlay-bg="isDark ? 'rgba(18, 18, 18, 0.78)' : 'rgba(255, 255, 255, 0.78)'"
			:color="isDark ? '#ffb800' : '#ff9800'"
		/>
	</view>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { purchaseProducts } from '@/api/modules/points.js'
import { fetchMyCoupons } from '@/api/modules/coupon.js'
import { verifyPayPassword } from '@/api/modules/user.js'
import { getShopCart, clearShopCart } from '@/utils/shop-cart-cache.js'
import { invalidateMemberOrdersCache } from '@/utils/member-orders-cache.js'
import { getShopPurchaseContext } from '@/utils/shop-purchase-context.js'
import { WX_PERM } from '@/api/constants/customer.js'
import { assertPermission } from '@/utils/permissions.js'
import {
	formatPointsAmount,
	calculateCouponPayablePoints,
	calculateCouponDiscountPoints,
	calculateLinePoints,
	calculateUnitPoints,
	meetsCouponThreshold
} from '@/utils/points-format.js'
import { guardPayPassword } from '@/services/payment.js'
import { hasPayPasswordSet } from '@/services/user-security.js'
import PayPinPad from '@/components/pay/pay-pin-pad.vue'
import PageLoading from '@/components/common/page-loading.vue'
import { useTheme } from '@/composables/use-theme.js'
import { createRequestId } from '@/utils/request-id.js'
import { couponIconSrc, formatCouponDesc } from '@/utils/coupon-display.js'

const MAX_PIN_ERRORS = 3

const shopId = ref('')
const { isDark, loadTheme } = useTheme()
const loading = ref(true)
const couponsLoading = ref(false)
const submitting = ref(false)
const preview = ref(null)
const selectedCouponId = ref(null)
const errorMsg = ref('')
const applicableCoupons = ref([])
const remainingPoints = ref(0)
const showPinPad = ref(false)
const pinError = ref('')
const pinResetKey = ref(0)
const pinVerifying = ref(false)
let payLoadingVisible = false
let passwordGateDone = false
let skipPinOnce = false
let pinErrorCount = 0
/** 单次购买流程的幂等 requestId，网络重试时复用 */
let purchaseRequestId = ''
/** @type {import('vue').Ref<Record<string, object>>} */
const productMap = ref({})
let shopName = ''
let rechargeRatio = null

onLoad(async (options) => {
	if (!assertPermission(WX_PERM.USER, '无会员访问权限')) return
	loadTheme()
	shopId.value = options?.shopId ? String(options.shopId) : ''
	if (!shopId.value) {
		loading.value = false
		errorMsg.value = '店铺信息无效'
		return
	}
	const guard = await guardPayPassword()
	passwordGateDone = true
	if (!guard.ok) {
		if (guard.navigatedToSet) {
			skipPinOnce = true
		} else {
			uni.navigateBack()
		}
		return
	}
	await initOrderPage()
})

onShow(() => {
	if (!passwordGateDone || loading.value) return
	if (skipPinOnce && hasPayPasswordSet()) {
		skipPinOnce = false
		void initOrderPage()
	}
})

onUnload(() => {
	hidePayLoading()
})

function buildItemsFromCart() {
	const cart = getShopCart(shopId.value)
	return Object.entries(cart)
		.filter(([, count]) => Number(count) > 0)
		.map(([productId, count]) => ({ productId: Number(productId), count: Number(count) }))
}

function buildPreviewItems(cartItems) {
	return cartItems.map(({ productId, count }) => {
		const product = productMap.value[String(productId)] || {}
		const linePoints = calculateLinePoints(product.price, count)
		return {
			productId,
			productName: product.productName || '商品',
			imageUrl: product.imageUrl || '',
			count,
			unitPoints: calculateUnitPoints(product.price),
			linePoints
		}
	})
}

function mapCouponOptions(coupons, originalPoints, selectedId) {
	return (coupons || []).map((coupon) => {
		const applicable = meetsCouponThreshold(coupon.thresholdAmount, originalPoints)
		let disabledReason = null
		if (!applicable) {
			const threshold = coupon.thresholdAmount
			if (threshold != null && Number(threshold) > 0) {
				disabledReason = `未满 ${threshold} 积分`
			} else {
				disabledReason = '不可用'
			}
		}
		return {
			userCouponId: coupon.userCouponId,
			couponName: coupon.couponName,
			type: coupon.type,
			thresholdAmount: coupon.thresholdAmount,
			discountValue: coupon.discountValue,
			applicable,
			disabledReason
		}
	})
}

function applyCouponSettlement(items, coupons, userCouponId) {
	const originalPoints = items.reduce((sum, item) => sum + Number(item.linePoints || 0), 0)
	if (!originalPoints) {
		return null
	}
	const couponOptions = mapCouponOptions(coupons, originalPoints, userCouponId)
	const selectedCoupon = userCouponId
		? couponOptions.find((c) => c.userCouponId === userCouponId)
		: null
	let payablePoints = originalPoints
	let discountPoints = 0
	if (selectedCoupon?.applicable) {
		payablePoints = calculateCouponPayablePoints(
			originalPoints,
			selectedCoupon.type,
			selectedCoupon.discountValue
		)
		discountPoints = calculateCouponDiscountPoints(originalPoints, payablePoints)
	}
	return {
		shopId: Number(shopId.value),
		shopName,
		ratio: rechargeRatio ?? preview.value?.ratio,
		items,
		originalPoints,
		discountPoints,
		payablePoints,
		selectedUserCouponId: selectedCoupon?.applicable ? userCouponId : null,
		coupons: couponOptions
	}
}

function refreshPreview(userCouponId = selectedCouponId.value) {
	const cartItems = buildItemsFromCart()
	if (!cartItems.length) {
		preview.value = null
		errorMsg.value = '购物车是空的'
		return false
	}
	const items = buildPreviewItems(cartItems)
	const coupons = preview.value?.coupons || []
	const next = applyCouponSettlement(items, coupons, userCouponId)
	if (!next) {
		preview.value = null
		errorMsg.value = '合计积分无效'
		return false
	}
	preview.value = next
	selectedCouponId.value = next.selectedUserCouponId || null
	applicableCoupons.value = (next.coupons || []).filter((c) => c.applicable)
	errorMsg.value = ''
	return true
}

async function loadShopCoupons() {
	couponsLoading.value = true
	const res = await fetchMyCoupons({
		shopId: Number(shopId.value),
		status: 0,
		pageNum: 1,
		pageSize: 100
	})
	couponsLoading.value = false
	if (!res.ok) {
		uni.showToast({ title: res.msg || '优惠券加载失败', icon: 'none' })
		return []
	}
	return res.rows || []
}

async function initOrderPage() {
	loading.value = true
	errorMsg.value = ''
	const ctx = getShopPurchaseContext(shopId.value)
	const cartItems = buildItemsFromCart()
	if (!cartItems.length) {
		loading.value = false
		errorMsg.value = '购物车是空的'
		preview.value = null
		return
	}
	if (!ctx || !Object.keys(ctx.products || {}).length) {
		loading.value = false
		errorMsg.value = '商品信息已失效，请返回店铺重新选择'
		preview.value = null
		return
	}
	productMap.value = ctx.products
	rechargeRatio = ctx.ratio
	shopName = ctx.shopName
	const balance = Number(ctx.remainingPoints)
	remainingPoints.value = Number.isNaN(balance) ? 0 : balance
	const items = buildPreviewItems(cartItems)
	const coupons = await loadShopCoupons()
	const settlement = applyCouponSettlement(items, coupons, selectedCouponId.value)
	loading.value = false
	if (!settlement) {
		errorMsg.value = '合计积分无效'
		preview.value = null
		return
	}
	preview.value = settlement
	selectedCouponId.value = settlement.selectedUserCouponId || null
	applicableCoupons.value = (settlement.coupons || []).filter((c) => c.applicable)
}

function selectCoupon(coupon) {
	if (coupon && !coupon.applicable) {
		uni.showToast({ title: coupon.disabledReason || '优惠券不可用', icon: 'none' })
		return
	}
	const nextId = coupon?.userCouponId || null
	if (selectedCouponId.value === nextId) return
	selectedCouponId.value = nextId
	refreshPreview(nextId)
}

async function submitPurchase() {
	if (submitting.value || couponsLoading.value || !preview.value) return
	if (showPinPad.value) return
	const payable = Number(preview.value.payablePoints)
	const available = remainingPoints.value
	if (!Number.isNaN(payable) && payable > available) {
		uni.showToast({
			title: `积分不足，当前剩余 ${formatPointsAmount(available)} 积分`,
			icon: 'none'
		})
		return
	}
	if (!purchaseRequestId) {
		purchaseRequestId = createRequestId()
	}
	showPinPad.value = true
	pinError.value = ''
	pinErrorCount = 0
}

function showPayLoading() {
	if (payLoadingVisible) return
	payLoadingVisible = true
	uni.showLoading({ title: '支付处理中…', mask: true })
}

function hidePayLoading() {
	if (!payLoadingVisible) return
	payLoadingVisible = false
	uni.hideLoading()
}

function onPinCancel() {
	hidePayLoading()
	pinVerifying.value = false
	showPinPad.value = false
}

function recordPinError(msg) {
	pinErrorCount += 1
	if (pinErrorCount >= MAX_PIN_ERRORS) {
		showPinPad.value = false
		uni.showModal({
			title: '提示',
			content: '错误次数过多，请稍后再试',
			showCancel: false
		})
		return
	}
	const left = MAX_PIN_ERRORS - pinErrorCount
	pinResetKey.value += 1
	pinError.value = `${msg}（还可尝试 ${left} 次）`
}

async function onPinComplete(digits) {
	if (!/^\d{6}$/.test(digits)) {
		recordPinError('请输入 6 位数字')
		return
	}
	pinVerifying.value = true
	await nextTick()
	showPayLoading()
	let navigated = false
	try {
		const verify = await verifyPayPassword(digits)
		if (!verify.ok) {
			if (verify.type === 'forbidden') {
				showPinPad.value = false
				uni.showModal({
					title: '无法校验密码',
					content: verify.msg || '当前账号无权限校验支付密码，请联系管理员',
					showCancel: false
				})
				return
			}
			if (verify.type === 'network') {
				showPinPad.value = false
				uni.showToast({ title: verify.msg || '网络错误', icon: 'none' })
				return
			}
			recordPinError(verify.msg || '支付密码错误')
			return
		}
		navigated = await doPurchase()
	} finally {
		if (!navigated) {
			pinVerifying.value = false
			hidePayLoading()
		}
	}
}

function goPurchaseSuccessPage(logId) {
	hidePayLoading()
	const url = `/pages/member/purchase-success?shopId=${shopId.value}&logId=${logId}`
	return new Promise((resolve) => {
		uni.redirectTo({
			url,
			animationType: 'slide-in-right',
			animationDuration: 200,
			success: () => resolve(true),
			fail: () => {
				uni.navigateTo({
					url,
					animationType: 'slide-in-right',
					animationDuration: 200,
					success: () => resolve(true),
					fail: () => {
						showPinPad.value = false
						hidePayLoading()
						uni.showToast({ title: '打开成功页失败，请从订单列表查看', icon: 'none' })
						resolve(false)
					}
				})
			}
		})
	})
}

async function doPurchase() {
	if (submitting.value || couponsLoading.value || !preview.value) return false
	const items = buildItemsFromCart()
	if (!items.length) {
		uni.showToast({ title: '购物车是空的', icon: 'none' })
		return false
	}
	const usePinOverlay = showPinPad.value
	if (!usePinOverlay) {
		submitting.value = true
	}
	const payload = {
		shopId: Number(shopId.value),
		items,
		requestId: purchaseRequestId || createRequestId()
	}
	if (selectedCouponId.value) payload.userCouponId = selectedCouponId.value
	let res
	try {
		res = await purchaseProducts(payload)
	} finally {
		if (!usePinOverlay) {
			submitting.value = false
		}
	}
	if (!res.ok) {
		uni.showToast({ title: res.msg || '购买失败', icon: 'none' })
		return false
	}
	purchaseRequestId = ''
	clearShopCart(shopId.value)
	invalidateMemberOrdersCache()
	const logId = res.data?.logId
	if (logId) {
		await nextTick()
		return goPurchaseSuccessPage(logId)
	}
	showPinPad.value = false
	const consumed = res.data?.consumePoints ?? preview.value.payablePoints
	uni.showToast({ title: `购买成功，-${formatPointsAmount(consumed)} 积分`, icon: 'success' })
	setTimeout(() => {
		uni.navigateBack()
	}, 600)
	return false
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	background: #f5f5f5;
}

.page--dark {
	background: #121212;
}

.scroll {
	flex: 1;
	height: 0;
	padding: 12px 16px 100px;
	box-sizing: border-box;
}

.shop-card,
.section-card {
	background: #fff;
	border-radius: 12px;
	padding: 14px 16px;
	margin-bottom: 12px;
}

.page--dark .shop-card,
.page--dark .section-card {
	background: #1e1e1e;
}

.shop-name {
	display: block;
	font-size: 16px;
	font-weight: 600;
	color: #222;
	margin-bottom: 4px;
}

.page--dark .shop-name {
	color: #f0f0f0;
}

.shop-ratio {
	font-size: 12px;
	color: #888;
}

.section-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 10px;
}

.section-title {
	font-size: 15px;
	font-weight: 600;
	color: #222;
}

.page--dark .section-title {
	color: #f0f0f0;
}

.section-sub {
	font-size: 12px;
	color: #888;
}

.goods-row {
	display: flex;
	align-items: center;
	gap: 10px;
	padding: 10px 0;
	border-bottom: 1px solid #f0f0f0;
}

.page--dark .goods-row {
	border-bottom-color: #2a2a2a;
}

.goods-row:last-child {
	border-bottom: none;
}

.goods-thumb {
	width: 48px;
	height: 48px;
	border-radius: 8px;
	flex-shrink: 0;
	background: #f3f4f6;
}

.page--dark .goods-thumb {
	background: #2a2a2a;
}

.goods-thumb--placeholder {
	display: flex;
	align-items: center;
	justify-content: center;
}

.goods-thumb-text {
	font-size: 12px;
	color: #bbb;
}

.goods-info {
	flex: 1;
	min-width: 0;
}

.goods-name {
	display: block;
	font-size: 14px;
	color: #333;
}

.page--dark .goods-name {
	color: #ddd;
}

.goods-meta {
	font-size: 12px;
	color: #999;
}

.goods-points {
	font-size: 14px;
	color: #e65c00;
	font-weight: 600;
	flex-shrink: 0;
}

.coupon-loading {
	padding: 8px 0 4px;
}

.coupon-row {
	display: flex;
	align-items: center;
	gap: 10px;
	padding: 12px;
	border-radius: 10px;
	border: 1px solid #eee;
	margin-bottom: 8px;
}

.page--dark .coupon-row {
	border-color: #333;
}

.coupon-row--active {
	border-color: #ffb800;
	background: rgba(255, 184, 0, 0.08);
}

.coupon-row--disabled {
	opacity: 0.55;
}

.coupon-row--none {
	padding-left: 14px;
}

.coupon-icon {
	width: 36px;
	height: 36px;
	flex-shrink: 0;
}

.coupon-body {
	flex: 1;
	min-width: 0;
}

.coupon-name {
	display: block;
	font-size: 14px;
	font-weight: 600;
	color: #333;
}

.page--dark .coupon-name {
	color: #eee;
}

.coupon-desc {
	display: block;
	font-size: 12px;
	color: #e65c00;
	margin-top: 2px;
}

.coupon-reason {
	display: block;
	font-size: 11px;
	color: #999;
	margin-top: 2px;
}

.coupon-check {
	width: 20px;
	height: 20px;
	border-radius: 50%;
	background: #ffb800;
	color: #fff;
	font-size: 12px;
	line-height: 20px;
	text-align: center;
	flex-shrink: 0;
}

.coupon-empty {
	padding: 12px 0 4px;
	text-align: center;
}

.coupon-empty-text {
	font-size: 13px;
	color: #999;
}

.summary-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 6px 0;
}

.summary-label {
	font-size: 14px;
	color: #666;
}

.page--dark .summary-label {
	color: #aaa;
}

.summary-value {
	font-size: 14px;
	color: #333;
}

.page--dark .summary-value {
	color: #ddd;
}

.summary-row--discount .summary-value {
	color: #e65c00;
}

.summary-row--total {
	margin-top: 8px;
	padding-top: 10px;
	border-top: 1px dashed #eee;
}

.page--dark .summary-row--total {
	border-top-color: #333;
}

.summary-total {
	font-size: 22px;
	font-weight: 700;
	color: #e65c00;
}

.bottom-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	display: flex;
	align-items: center;
	gap: 12px;
	padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
	background: #fff;
	box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
}

.bottom-bar--dark {
	background: #1a1a1a;
	box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.3);
}

.bottom-info {
	flex: 1;
}

.bottom-label {
	display: block;
	font-size: 12px;
	color: #888;
}

.bottom-points {
	display: flex;
	align-items: baseline;
	gap: 4px;
}

.bottom-num {
	font-size: 22px;
	font-weight: 700;
	color: #e65c00;
}

.bottom-unit {
	font-size: 12px;
	color: #e65c00;
}

.bottom-btn {
	min-width: 120px;
	height: 44px;
	border-radius: 22px;
	background: linear-gradient(135deg, #ffc107, #ff9800);
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 0 20px;
}

.bottom-btn--disabled {
	opacity: 0.6;
}

.bottom-btn-text {
	font-size: 15px;
	font-weight: 600;
	color: #fff;
}

.state-wrap {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 48px 16px;
}

.state-text {
	font-size: 14px;
	color: #999;
}
</style>
