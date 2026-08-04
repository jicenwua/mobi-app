<template>
	<view class="page" :class="[isDark ? 'page--dark' : 'page--light', { 'page--with-refund': showRefundButton }]">
		<view v-if="!detail" class="state-wrap">
			<text class="state-text">{{ errorMsg || '订单信息无效' }}</text>
		</view>
		<template v-else>
			<view class="detail-head">
				<text class="detail-title">订单详情</text>
				<text class="detail-status" :class="{ 'detail-status--muted': detail.status !== POINTS_LOG_STATUS.PENDING_USE }">{{ statusLabel(detail.status) }}</text>
				<text v-if="showOrderQrcode" class="detail-sub">请向店员出示订单码完成核销</text>
			</view>

			<view class="section-card">
				<view v-if="fromShop" class="summary-row">
					<text class="summary-label">会员</text>
					<text class="summary-value">{{ detail.nickname || '—' }}</text>
				</view>
				<view v-if="fromShop && detail.phone" class="summary-row">
					<text class="summary-label">手机</text>
					<text class="summary-value">{{ detail.phone }}</text>
				</view>
				<view v-if="!fromShop" class="summary-row">
					<text class="summary-label">店铺</text>
					<text class="summary-value">{{ detail.shopName || '—' }}</text>
				</view>
				<view class="summary-row">
					<text class="summary-label">下单时间</text>
					<text class="summary-value">{{ formatDateTime(detail.createTime) }}</text>
				</view>
				<view v-if="showConsumeTime" class="summary-row">
					<text class="summary-label">核销时间</text>
					<text class="summary-value">{{ formatDateTime(detail.consumeTime) }}</text>
				</view>
				<view class="summary-row">
					<text class="summary-label">{{ consumePointsLabel }}</text>
					<text class="summary-value" :class="consumePointsClass">{{ consumePointsDisplay }}</text>
				</view>
				<view v-if="detail.remark" class="summary-row">
					<text class="summary-label">备注</text>
					<text class="summary-value">{{ detail.remark }}</text>
				</view>
				<view class="summary-row">
					<text class="summary-label">剩余积分</text>
					<text class="summary-value">{{ formatPointsAmount(detail.remainingPoints) }} 积分</text>
				</view>
				<view v-if="detail.couponDiscountPoints" class="summary-row">
					<text class="summary-label">优惠券抵扣</text>
					<text class="summary-value">-{{ formatPointsAmount(detail.couponDiscountPoints) }} 积分</text>
				</view>
				<view class="goods-block">
					<text class="goods-title">商品明细</text>
					<view v-if="itemsLoading" class="goods-loading">
						<page-loading text="加载商品明细…" compact :color="isDark ? '#5ac8fa' : '#007aff'" />
					</view>
					<view v-else-if="detail.items && detail.items.length">
						<view v-for="item in detail.items" :key="item.receiptId || item.productId" class="goods-row">
							<view class="goods-name-wrap">
								<text class="goods-name">{{ item.productName }}</text>
								<text class="goods-count">×{{ item.count }}</text>
							</view>
							<text class="goods-price">{{ formatPointsAmount(item.linePoints) }}积分</text>
						</view>
					</view>
					<view v-else class="goods-empty">
						<text class="goods-empty-text">暂无商品明细</text>
					</view>
				</view>
			</view>

			<view v-if="showOrderQrcode" class="section-card qr-card">
				<text class="qr-title">订单核销码</text>
				<view v-if="qrLoading" class="qr-loading">
					<page-loading text="生成核销码…" :color="isDark ? '#ffb800' : '#ff9800'" />
				</view>
				<view v-else-if="qrContent" class="qr-wrap">
					<QrcodeCanvas :text="qrContent" :size="220" :canvas-id="orderQrCanvasId" />
					<text class="qr-hint">向店员出示此码完成核销</text>
				</view>
				<view v-else class="qr-error">
					<text class="qr-error-text">{{ qrError || '无法生成订单码' }}</text>
				</view>
			</view>

			<view v-if="showRefundButton" class="refund-bar">
				<button
					class="refund-btn"
					:class="{ 'refund-btn--loading': refunding }"
					:disabled="refunding"
					@click="onRefund"
				>
					{{ refunding ? '退款中…' : '申请退款' }}
				</button>
			</view>
		</template>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchConsumeLogDetail, refundOrder } from '@/api/modules/points.js'
import { generateOrderQrcode } from '@/api/modules/qrcode.js'
import { POINTS_LOG_STATUS, pointsLogStatusLabel, isPointsLogGain } from '@/utils/points-log-status.js'
import { formatPointsAmount } from '@/utils/points-format.js'
import { formatDateTime } from '@/utils/datetime-format.js'
import { invalidateMemberOrdersCache } from '@/utils/member-orders-cache.js'
import { useTheme } from '@/composables/use-theme.js'
import { usePolling } from '@/composables/use-polling.js'
import QrcodeCanvas from '@/components/common/qrcode-canvas.vue'
import PageLoading from '@/components/common/page-loading.vue'

const ORDER_STATUS_POLL_MS = 3000

const { isDark, loadTheme } = useTheme()
const shopId = ref('')
const logId = ref('')
const fromShop = ref(false)
const detail = ref(null)
const errorMsg = ref('')
const itemsLoading = ref(false)
const qrContent = ref('')
const qrLoading = ref(false)
const qrError = ref('')
const refunding = ref(false)
let verifyNotified = false

const showOrderQrcode = computed(
	() => !fromShop.value && detail.value?.status === POINTS_LOG_STATUS.PENDING_USE
)

const showRefundButton = computed(
	() => !fromShop.value && detail.value?.status === POINTS_LOG_STATUS.PENDING_USE
)

const showConsumeTime = computed(() => {
	const d = detail.value
	if (!d || d.status === POINTS_LOG_STATUS.PENDING_USE) return false
	return !!d.consumeTime
})

const isPointsGain = computed(() => isPointsLogGain(detail.value))

const consumePointsLabel = computed(() => {
	const d = detail.value
	if (!d) return '积分变动'
	if (d.status === POINTS_LOG_STATUS.RECHARGE) return '充值积分'
	if (d.status === POINTS_LOG_STATUS.ADMIN_ADD) return '添加积分'
	if (d.status === POINTS_LOG_STATUS.ADMIN_DEDUCT) return '扣除积分'
	if (d.actionType === 1) return '增加积分'
	return '消费积分'
})

const consumePointsDisplay = computed(() => {
	const pts = detail.value?.consumePoints
	if (pts == null) return '—'
	const prefix = isPointsGain.value ? '+' : '-'
	return `${prefix}${formatPointsAmount(pts)} 积分`
})

const consumePointsClass = computed(() =>
	isPointsGain.value ? 'summary-value--gain' : 'summary-value--points'
)

const orderQrCanvasId = computed(() => `order-detail-qr-${logId.value || 'x'}`)

function statusLabel(status) {
	return pointsLogStatusLabel(status)
}

function applyOrderSnapshot(row) {
	if (!row) return
	detail.value = { ...row }
	if (row.status === POINTS_LOG_STATUS.PENDING_USE) {
		void loadOrderQrcode()
		orderPolling.start()
	} else {
		orderPolling.stop()
	}
	if (!row.items?.length) {
		void refreshDetailItems()
	}
}

function shouldPollOrderStatus() {
	return !fromShop.value && detail.value?.status === POINTS_LOG_STATUS.PENDING_USE
}

const orderPolling = usePolling(
	() => shouldPollOrderStatus(),
	() => pollOrderStatus(),
	ORDER_STATUS_POLL_MS
)

function handleOrderStatusSettled(nextStatus, data) {
	orderPolling.stop()
	detail.value = { ...detail.value, ...data, status: nextStatus }
	qrContent.value = ''
	invalidateMemberOrdersCache()
	if (nextStatus === POINTS_LOG_STATUS.VERIFIED && !verifyNotified) {
		verifyNotified = true
		uni.showModal({
			title: '核销成功',
			content: '订单已核销，感谢您的光临',
			showCancel: false
		})
	}
}

async function pollOrderStatus() {
	if (!logId.value || !shopId.value || !shouldPollOrderStatus()) {
		orderPolling.stop()
		return
	}
	const res = await fetchConsumeLogDetail(Number(logId.value), Number(shopId.value))
	if (!res.ok || !res.data) return
	const nextStatus = res.data.status
	if (nextStatus === POINTS_LOG_STATUS.PENDING_USE) return
	handleOrderStatusSettled(nextStatus, res.data)
}

async function loadOrderQrcode() {
	if (!logId.value || !shopId.value) return
	qrLoading.value = true
	qrError.value = ''
	qrContent.value = ''
	const res = await generateOrderQrcode({
		logId: Number(logId.value),
		shopId: Number(shopId.value)
	})
	qrLoading.value = false
	if (!res.ok || !res.data?.qrContent) {
		qrError.value = res.msg || '生成失败'
		return
	}
	qrContent.value = res.data.qrContent
}

function onRefund() {
	if (refunding.value || !detail.value) return
	uni.showModal({
		title: '确认退款',
		content: '退款后积分将原路退回，是否继续？',
		confirmText: '确认退款',
		success(res) {
			if (!res.confirm) return
			void doRefund()
		}
	})
}

async function doRefund() {
	if (!logId.value || !shopId.value) return
	refunding.value = true
	const res = await refundOrder({
		logId: Number(logId.value),
		shopId: Number(shopId.value)
	})
	refunding.value = false
	if (!res.ok) {
		uni.showToast({ title: res.msg || '退款失败', icon: 'none' })
		return
	}
	detail.value = {
		...detail.value,
		status: POINTS_LOG_STATUS.CANCELLED,
		remainingPoints: res.data ?? detail.value.remainingPoints
	}
	qrContent.value = ''
	orderPolling.stop()
	invalidateMemberOrdersCache()
	uni.showToast({ title: '退款成功', icon: 'success' })
	void refreshDetailItems()
}

async function refreshDetailItems() {
	if (!logId.value || !shopId.value) return
	itemsLoading.value = true
	const res = await fetchConsumeLogDetail(Number(logId.value), Number(shopId.value))
	itemsLoading.value = false
	if (!res.ok || !res.data) {
		if (!detail.value) {
			errorMsg.value = res.msg || '加载订单失败'
		}
		return
	}
	detail.value = { ...detail.value, ...res.data }
}

onLoad((options) => {
	loadTheme()
	shopId.value = options?.shopId ? String(options.shopId) : ''
	logId.value = options?.logId ? String(options.logId) : ''
	fromShop.value = options?.from === 'shop'
	if (!shopId.value || !logId.value) {
		errorMsg.value = '订单信息无效'
		return
	}

	const pages = getCurrentPages()
	const page = pages[pages.length - 1]
	let snapshotApplied = false
	const channel = page?.getOpenerEventChannel?.()
	channel?.on?.('order', (data) => {
		snapshotApplied = true
		applyOrderSnapshot(data)
	})

	setTimeout(() => {
		if (snapshotApplied || detail.value) return
		void (async () => {
			itemsLoading.value = true
			const res = await fetchConsumeLogDetail(Number(logId.value), Number(shopId.value))
			itemsLoading.value = false
			if (!res.ok || !res.data) {
				errorMsg.value = res.msg || '加载订单失败'
				return
			}
			applyOrderSnapshot(res.data)
		})()
	}, 80)
})
</script>

<style scoped>
.page {
	min-height: 100vh;
	padding: 16px 16px calc(24px + env(safe-area-inset-bottom));
	box-sizing: border-box;
	background: #f5f5f5;
}

.page--with-refund {
	padding-bottom: calc(80px + env(safe-area-inset-bottom));
}

.page--dark {
	background: #121212;
	color: #e8e8e8;
}

.detail-head {
	padding: 8px 0 16px;
	text-align: center;
}

.detail-title {
	display: block;
	font-size: 20px;
	font-weight: 700;
	margin-bottom: 6px;
}

.detail-status {
	display: block;
	font-size: 14px;
	color: #007aff;
	font-weight: 600;
}

.page--dark .detail-status {
	color: #5ac8fa;
}

.detail-status--muted {
	color: #888 !important;
}

.detail-sub {
	display: block;
	margin-top: 8px;
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
	align-items: flex-start;
	padding: 8px 0;
	font-size: 14px;
	border-bottom: 1px solid rgba(128, 128, 128, 0.12);
}

.summary-row:last-of-type {
	border-bottom: none;
}

.summary-label {
	opacity: 0.55;
	flex-shrink: 0;
	margin-right: 12px;
}

.summary-value {
	text-align: right;
	flex: 1;
}

.summary-value--gain {
	color: #34c759;
	font-weight: 600;
}

.summary-value--points {
	color: #ff3b30;
	font-weight: 600;
}

.page--dark .summary-value--points {
	color: #ff6b6b;
}

.goods-block {
	margin-top: 12px;
	padding-top: 12px;
	border-top: 1px solid rgba(128, 128, 128, 0.12);
}

.goods-title {
	display: block;
	font-size: 14px;
	font-weight: 600;
	margin-bottom: 8px;
}

.goods-row {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 6px 0;
	font-size: 13px;
}

.goods-name-wrap {
	flex: 1;
	display: flex;
	align-items: center;
	gap: 6px;
}

.goods-count {
	opacity: 0.55;
}

.goods-empty,
.goods-loading {
	padding: 12px 0;
	text-align: center;
}

.goods-empty-text {
	font-size: 13px;
	opacity: 0.45;
}

.state-wrap {
	padding: 48px 16px;
	text-align: center;
}

.state-text {
	font-size: 14px;
	opacity: 0.55;
}

.qr-card {
	text-align: center;
}

.qr-title {
	display: block;
	font-size: 15px;
	font-weight: 600;
	margin-bottom: 12px;
}

.qr-wrap {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 10px;
}

.qr-hint {
	font-size: 12px;
	opacity: 0.55;
}

.qr-error-text {
	font-size: 13px;
	color: #ff3b30;
}

.refund-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
	background: rgba(255, 255, 255, 0.96);
	box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}

.page--dark .refund-bar {
	background: rgba(30, 30, 30, 0.96);
}

.refund-btn {
	width: 100%;
	height: 44px;
	line-height: 44px;
	border-radius: 10px;
	background: #ff3b30;
	color: #fff;
	font-size: 16px;
	border: none;
}

.refund-btn--loading {
	opacity: 0.7;
}
</style>
