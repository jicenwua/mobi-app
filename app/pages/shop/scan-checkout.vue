<template>
	<view class="page" :class="isDark ? 'page--dark' : 'page--light'">
		<view class="section">
			<text class="section-title">1. 添加购物清单</text>
			<view v-if="productsLoading" class="state-text">加载商品…</view>
			<view v-else-if="!products.length" class="state-text">暂无商品，请先在店铺管理中添加</view>
			<view v-else v-for="p in products" :key="p.productId" class="product-row">
				<view class="product-info">
					<text class="product-name">{{ p.productName }}</text>
					<text class="product-price">{{ formatProductPoints(p) }} 积分</text>
				</view>
				<view class="qty-control">
					<view class="qty-btn" @click="changeQty(p.productId, -1)"><text>−</text></view>
					<text class="qty-num">{{ cart[p.productId] || 0 }}</text>
					<view class="qty-btn" @click="changeQty(p.productId, 1)"><text>+</text></view>
				</view>
			</view>
		</view>

		<view class="section">
			<text class="section-title">2. 扫描会员付款码</text>
			<view v-if="memberInfo" class="member-card">
				<text class="member-name">{{ memberInfo.nickname || '会员' }}</text>
				<text class="member-phone">{{ memberInfo.phone || '' }}</text>
				<text class="member-points">剩余 {{ memberInfo.remainingPoints ?? 0 }} 积分</text>
			</view>
			<view v-else class="scan-btn" @click="scanMemberCode">
				<text class="scan-btn-text">点击扫码</text>
			</view>
		</view>

		<view class="footer">
			<text class="total-text">合计：{{ totalPoints }} 积分</text>
			<view
				class="submit-btn"
				:class="{ disabled: submitting || !canSubmit }"
				@click="submitConsume"
			>
				<text class="submit-text">{{ submitting ? '扣款中…' : (payToken ? '确认扣款' : '扫码扣款') }}</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchManageProducts } from '@/api/modules/shop-manage.js'
import { verifyPayQrcode } from '@/api/modules/qrcode.js'
import { consumePoints } from '@/api/modules/points.js'
import { parsePayTokenFromScan } from '@/utils/qrcode-scan.js'
import { canStaffVerifyAtShop } from '@/utils/wx-perm.js'
import { navigateBackDelayed } from '@/utils/navigation.js'
import { useTheme } from '@/composables/use-theme.js'
import { createRequestId } from '@/utils/request-id.js'

const { isDark, loadTheme } = useTheme()
const shopId = ref('')
const payToken = ref('')
const memberInfo = ref(null)
const products = ref([])
const productsLoading = ref(false)
const cart = reactive({})
const submitting = ref(false)
/** 单次扣款流程的幂等 requestId */
let consumeRequestId = ''

const totalPoints = computed(() => {
	let sum = 0
	for (const p of products.value) {
		const qty = cart[p.productId] || 0
		if (qty <= 0) continue
		const pts = calcProductPoints(p)
		if (pts != null) sum += pts * qty
	}
	return sum
})

const canSubmit = computed(() => totalPoints.value > 0)

onLoad((options) => {
	if (!options?.shopId) {
		uni.showToast({ title: '店铺信息无效', icon: 'none' })
		navigateBackDelayed(800)
		return
	}
		loadTheme()
	shopId.value = options?.shopId || ''
	if (shopId.value) loadProducts()
	const presetToken = options?.token ? decodeURIComponent(String(options.token)) : ''
	if (presetToken) {
		void applyPayToken(presetToken)
	}
})

async function loadProducts() {
	productsLoading.value = true
	const res = await fetchManageProducts(shopId.value, 1, 100)
	productsLoading.value = false
	if (!res.ok) {
		uni.showToast({ title: res.msg || '加载商品失败', icon: 'none' })
		return
	}
	products.value = res.rows || []
}

function calcProductPoints(product) {
	const price = product?.price
	if (price == null) return null
	const pts = Number(price)
	return Number.isNaN(pts) ? null : Math.round(pts)
}

function formatProductPoints(product) {
	const pts = calcProductPoints(product)
	return pts == null ? '—' : String(pts)
}

function changeQty(productId, delta) {
	const cur = cart[productId] || 0
	const next = Math.max(0, cur + delta)
	if (next === 0) {
		delete cart[productId]
	} else {
		cart[productId] = next
	}
}

async function applyPayToken(token) {
	const verify = await verifyPayQrcode({ shopId: shopId.value, token })
	if (!verify.ok || !verify.data) {
		uni.showToast({ title: verify.msg || '付款码无效', icon: 'none' })
		return
	}
	payToken.value = token
	memberInfo.value = verify.data
}

function scanMemberCode() {
	uni.scanCode({
		onlyFromCamera: false,
		success: async (res) => {
			const token = parsePayTokenFromScan(res.result)
			if (!token) {
				uni.showToast({ title: '无效的付款码', icon: 'none' })
				return
			}
			await applyPayToken(token)
		},
		fail: () => {
			uni.showToast({ title: '扫码取消', icon: 'none' })
		}
	})
}

async function doConsume() {
	const items = Object.entries(cart)
		.filter(([, count]) => count > 0)
		.map(([productId, count]) => ({ productId: Number(productId), count }))
	if (!items.length) {
		uni.showToast({ title: '请选择商品', icon: 'none' })
		return
	}
	if (!payToken.value) {
		uni.showToast({ title: '请先扫描会员付款码', icon: 'none' })
		return
	}
	submitting.value = true
	if (!consumeRequestId) {
		consumeRequestId = createRequestId()
	}
	const res = await consumePoints({
		shopId: Number(shopId.value),
		token: payToken.value,
		items,
		requestId: consumeRequestId
	})
	submitting.value = false
	if (!res.ok) {
		uni.showToast({ title: res.msg || '扣款失败', icon: 'none' })
		return
	}
	consumeRequestId = ''
	uni.showToast({ title: `扣款成功，-${res.data ?? totalPoints.value} 积分`, icon: 'success' })
	navigateBackDelayed(800)
}

async function submitConsume() {
	if (!canSubmit.value || submitting.value) return
	if (!payToken.value) {
		uni.scanCode({
			onlyFromCamera: false,
			success: async (res) => {
				const token = parsePayTokenFromScan(res.result)
				if (!token) {
					uni.showToast({ title: '无效的付款码', icon: 'none' })
					return
				}
				await applyPayToken(token)
				await doConsume()
			},
			fail: () => {
				uni.showToast({ title: '扫码取消', icon: 'none' })
			}
		})
		return
	}
	await doConsume()
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	padding: 16px 16px 100px;
	box-sizing: border-box;
}

.page--light {
	background: #f5f5f5;
	color: #333;
}

.page--dark {
	background: #121212;
	color: #e8e8e8;
}

.section {
	border-radius: 10px;
	padding: 14px;
	margin-bottom: 12px;
}

.page--light .section,
.page--light .member-card,
.page--light .product-row {
	background: #fff;
}

.page--dark .section,
.page--dark .member-card,
.page--dark .product-row {
	background: #1e1e1e;
}

.section-title {
	font-size: 15px;
	font-weight: 600;
	display: block;
	margin-bottom: 12px;
}

.scan-btn {
	padding: 20px;
	border-radius: 8px;
	background: #007aff;
	text-align: center;
}

.scan-btn-text {
	color: #fff;
	font-size: 15px;
}

.member-card {
	border-radius: 8px;
	padding: 12px;
}

.member-name {
	font-size: 16px;
	font-weight: 600;
	display: block;
}

.member-phone {
	font-size: 13px;
	opacity: 0.6;
	display: block;
	margin-top: 4px;
}

.member-points {
	font-size: 14px;
	color: #c9a227;
	display: block;
	margin-top: 8px;
	font-weight: 600;
}

.product-row {
	border-radius: 8px;
	padding: 12px;
	margin-bottom: 8px;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
}

.product-info {
	flex: 1;
	min-width: 0;
}

.product-name {
	font-size: 14px;
	display: block;
}

.product-price {
	font-size: 12px;
	opacity: 0.55;
	margin-top: 4px;
	display: block;
}

.qty-control {
	display: flex;
	align-items: center;
	gap: 10px;
	flex-shrink: 0;
}

.qty-btn {
	width: 28px;
	height: 28px;
	border-radius: 14px;
	background: rgba(0, 122, 255, 0.12);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 16px;
	color: #007aff;
}

.qty-num {
	min-width: 20px;
	text-align: center;
	font-size: 14px;
}

.state-text {
	font-size: 13px;
	opacity: 0.5;
	padding: 8px 0;
}

.footer {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
	box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
}

.page--light .footer {
	background: #fff;
}

.page--dark .footer {
	background: #1e1e1e;
}

.total-text {
	font-size: 15px;
	font-weight: 600;
}

.submit-btn {
	padding: 10px 20px;
	border-radius: 8px;
	background: #007aff;
}

.submit-btn.disabled {
	opacity: 0.5;
	pointer-events: none;
}

.submit-text {
	color: #fff;
	font-size: 14px;
}
</style>
