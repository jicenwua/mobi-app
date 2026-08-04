<template>
	<view class="page" :class="isDark ? 'page--dark' : 'page--light'">
		<view v-if="loading" class="state-wrap">
			<text class="state-text">加载中…</text>
		</view>
		<template v-else-if="detail">
			<view class="success-head">
				<view class="success-icon">✓</view>
				<text class="success-title">购买成功</text>
				<text class="success-sub">可在订单详情中查看核销码</text>
			</view>

			<view class="section-card">
				<view class="summary-row">
					<text class="summary-label">店铺</text>
					<text class="summary-value">{{ detail.shopName || '—' }}</text>
				</view>
				<view class="summary-row">
					<text class="summary-label">消费积分</text>
					<text class="summary-value summary-value--points">-{{ detail.consumePoints ?? 0 }} 积分</text>
				</view>
				<view class="summary-row">
					<text class="summary-label">剩余积分</text>
					<text class="summary-value">{{ detail.remainingPoints ?? 0 }} 积分</text>
				</view>
				<view v-if="detail.items && detail.items.length" class="goods-block">
					<text class="goods-title">商品明细</text>
					<view v-for="item in detail.items" :key="item.receiptId" class="goods-row">
						<text class="goods-name">{{ item.productName }}</text>
						<text class="goods-meta">×{{ item.count }} {{ item.linePoints ?? 0 }}积分</text>
					</view>
				</view>
			</view>

			<view class="actions">
				<view class="action-btn action-btn--primary" @click="goOrders">
					<text class="action-btn-text">查看订单</text>
				</view>
				<view class="action-btn" @click="goBackShop">
					<text class="action-btn-text action-btn-text--muted">返回店铺</text>
				</view>
			</view>
		</template>
		<view v-else class="state-wrap">
			<text class="state-text">{{ errorMsg || '订单信息无效' }}</text>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchConsumeLogDetail } from '@/api/modules/points.js'
import { invalidateMemberOrdersCache } from '@/utils/member-orders-cache.js'
import { useTheme } from '@/composables/use-theme.js'

const shopId = ref('')
const logId = ref('')
const { isDark, loadTheme } = useTheme()
const loading = ref(true)
const detail = ref(null)
const errorMsg = ref('')

onLoad((options) => {
	loadTheme()
	shopId.value = options?.shopId ? String(options.shopId) : ''
	logId.value = options?.logId ? String(options.logId) : ''
	if (!shopId.value || !logId.value) {
		loading.value = false
		errorMsg.value = '订单信息无效'
		return
	}
	invalidateMemberOrdersCache()
	void loadDetail()
})

async function loadDetail() {
	loading.value = true
	const res = await fetchConsumeLogDetail(Number(logId.value), Number(shopId.value))
	loading.value = false
	if (!res.ok || !res.data) {
		errorMsg.value = res.msg || '加载订单失败'
		detail.value = null
		return
	}
	detail.value = res.data
}

function goOrders() {
	const sid = shopId.value
	const lid = logId.value
	if (!sid || !lid) {
		uni.reLaunch({ url: '/pages/main/main?tab=orders' })
		return
	}
	uni.navigateTo({
		url: `/pages/member/order-detail?shopId=${sid}&logId=${lid}`,
		animationType: 'slide-in-right',
		animationDuration: 200
	})
}

function goBackShop() {
	const id = shopId.value
	if (!id) {
		uni.navigateBack()
		return
	}
	uni.navigateBack({
		delta: 1,
		fail: () => {
			uni.redirectTo({ url: `/pages/main/main?shopId=${id}` })
		}
	})
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	padding: 16px 16px calc(24px + env(safe-area-inset-bottom));
	box-sizing: border-box;
	background: #f5f5f5;
}

.page--dark {
	background: #121212;
	color: #e8e8e8;
}

.success-head {
	padding: 24px 0 16px;
	text-align: center;
}

.success-icon {
	width: 56px;
	height: 56px;
	margin: 0 auto 12px;
	border-radius: 28px;
	background: linear-gradient(135deg, #34c759, #30b350);
	color: #fff;
	font-size: 28px;
	line-height: 56px;
	font-weight: 700;
}

.success-title {
	display: block;
	font-size: 20px;
	font-weight: 700;
	margin-bottom: 6px;
}

.success-sub {
	display: block;
	font-size: 13px;
	opacity: 0.55;
}

.section-card {
	background: #fff;
	border-radius: 12px;
	padding: 14px 16px;
	margin-bottom: 12px;
}

.page--dark .section-card {
	background: #1e1e1e;
}

.summary-row {
	display: flex;
	justify-content: space-between;
	gap: 12px;
	padding: 8px 0;
	font-size: 14px;
}

.summary-label {
	opacity: 0.6;
}

.summary-value--points {
	color: #e64340;
	font-weight: 600;
}

.goods-block {
	margin-top: 8px;
	padding-top: 10px;
	border-top: 1px solid rgba(128, 128, 128, 0.15);
}

.goods-title {
	display: block;
	font-size: 13px;
	font-weight: 600;
	margin-bottom: 8px;
}

.goods-row {
	display: flex;
	justify-content: space-between;
	padding: 4px 0;
	font-size: 13px;
}

.goods-name {
	flex: 1;
	min-width: 0;
}

.goods-meta {
	opacity: 0.55;
	margin-left: 8px;
}

.actions {
	margin-top: 8px;
	display: flex;
	flex-direction: column;
	gap: 10px;
}

.action-btn {
	padding: 12px;
	border-radius: 10px;
	text-align: center;
	background: #fff;
}

.page--dark .action-btn {
	background: #1e1e1e;
}

.action-btn--primary {
	background: linear-gradient(135deg, #ffc107, #ff9800);
}

.action-btn-text {
	font-size: 15px;
	font-weight: 600;
	color: #fff;
}

.action-btn-text--muted {
	color: #666;
}

.page--dark .action-btn-text--muted {
	color: #ccc;
}

.state-wrap {
	padding: 48px 16px;
	text-align: center;
}

.state-text {
	font-size: 14px;
	opacity: 0.5;
}
</style>
