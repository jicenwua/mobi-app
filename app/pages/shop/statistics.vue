<template>
	<view class="page">
		<scroll-view
			class="scroll"
			scroll-y
			:refresher-enabled="true"
			:refresher-triggered="refreshing"
			@refresherrefresh="onRefresh"
		>
			<view class="scroll-inner">
				<view v-if="loading" class="state-wrap">
					<text class="state-text">加载中…</text>
				</view>

				<template v-else>
					<view class="period-bar">
						<view class="period-track">
							<view
								v-for="opt in periodOptions"
								:key="opt.value"
								class="period-chip"
								:class="{ 'period-chip--active': periodPreset === opt.value }"
								@click="switchPeriod(opt.value)"
							>
								<text class="period-chip-text">{{ opt.label }}</text>
							</view>
						</view>
						<view v-if="periodPreset === 'custom'" class="custom-range-wrap">
							<uni-datetime-picker
								v-model="customDateRange"
								type="daterange"
								:end="todayStr"
								@change="onCustomDateChange"
							/>
						</view>
					</view>

					<view v-if="scopeOptions.length > 1" class="scope-bar">
						<view class="scope-track">
							<view
								v-for="opt in scopeOptions"
								:key="opt.value"
								class="scope-chip"
								:class="{ 'scope-chip--active': selectedScope === opt.value }"
								@click="switchScope(opt.value)"
							>
								<text class="scope-chip-text">{{ opt.label }}</text>
							</view>
						</view>
					</view>

					<view class="stats-grid">
						<view class="stat-card">
							<text class="stat-label">{{ statTitles.newUsers }}</text>
							<text class="stat-value">{{ formatNumber(stats.newUsers) }}</text>
						</view>
						<view class="stat-card">
							<text class="stat-label">{{ statTitles.orders }}</text>
							<text class="stat-value">{{ formatNumber(stats.orders) }}</text>
						</view>
						<view class="stat-card">
							<text class="stat-label">{{ statTitles.pointsUsed }}</text>
							<text class="stat-value">{{ formatNumber(stats.pointsUsed) }}</text>
						</view>
						<view class="stat-card">
							<text class="stat-label">{{ statTitles.rechargeAmount }}</text>
							<text class="stat-value stat-value--money">¥{{ formatMoney(stats.rechargeAmount) }}</text>
						</view>
					</view>

					<view class="chart-card">
						<text class="chart-title">{{ chartTitle }}</text>
						<canvas
							canvas-id="statsTrendCanvas"
							id="statsTrendCanvas"
							class="trend-canvas"
							:style="{ width: canvasWidth + 'px', height: canvasHeight + 'px' }"
						></canvas>
						<view class="legend-row">
							<view class="legend-item">
								<view class="legend-dot legend-dot--users"></view>
								<text>新用户</text>
							</view>
							<view class="legend-item">
								<view class="legend-dot legend-dot--orders"></view>
								<text>订单</text>
							</view>
							<view class="legend-item">
								<view class="legend-dot legend-dot--points"></view>
								<text>使用积分</text>
							</view>
						</view>
					</view>
				</template>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, ref } from 'vue'
import { onLoad, onReady } from '@dcloudio/uni-app'
import { fetchShopStatistics } from '@/api/modules/shop.js'
import { canManageShop } from '@/utils/wx-perm.js'
import { navigateBackDelayed } from '@/utils/navigation.js'

const shopId = ref('')
const loading = ref(true)
const refreshing = ref(false)
const statistics = ref(null)
const selectedScope = ref('all')
const periodPreset = ref('today')
const customDateRange = ref([])
const canvasWidth = ref(320)
const canvasHeight = ref(220)

const periodOptions = [
	{ label: '今天', value: 'today' },
	{ label: '近3天', value: '3d' },
	{ label: '近7天', value: '7d' },
	{ label: '近30天', value: '30d' },
	{ label: '自定义', value: 'custom' }
]

const stats = computed(() => ({
	newUsers: statistics.value?.todayNewUsers ?? 0,
	orders: statistics.value?.todayOrders ?? 0,
	pointsUsed: statistics.value?.totalPointsUsed ?? 0,
	rechargeAmount: statistics.value?.todayRechargeAmount ?? 0
}))

const todayStr = computed(() => formatDate(new Date()))

const isTodayPeriod = computed(() => {
	const { startDate, endDate } = resolveDateRange()
	return startDate === endDate && startDate === todayStr.value
})

const statTitles = computed(() => {
	if (isTodayPeriod.value) {
		return {
			newUsers: '今日新用户',
			orders: '今日订单',
			pointsUsed: '今日使用积分',
			rechargeAmount: '今日充值金额'
		}
	}
	return {
		newUsers: '新用户',
		orders: '订单',
		pointsUsed: '使用积分',
		rechargeAmount: '充值金额'
	}
})

const chartTitle = computed(() => {
	const { startDate, endDate } = resolveDateRange()
	if (startDate === endDate) {
		return `${startDate} 走势`
	}
	return `${startDate} ~ ${endDate} 走势`
})

const scopeOptions = computed(() => {
	if (!statistics.value?.headShop) {
		return []
	}
	const options = [
		{ label: '全部汇总', value: 'all' },
		{ label: '总店本店', value: `self:${shopId.value}` }
	]
	for (const branch of statistics.value.branches || []) {
		options.push({
			label: branch.shopName || `分店#${branch.id}`,
			value: `shop:${branch.id}`
		})
	}
	return options
})

onLoad((options) => {
	if (!canManageShop()) {
		uni.showToast({ title: '无店铺管理权限', icon: 'none' })
		navigateBackDelayed(800)
		return
	}
	shopId.value = options?.shopId
		? String(options.shopId)
		: options?.id
			? String(options.id)
			: ''
	if (!shopId.value) {
		loading.value = false
		uni.showToast({ title: '缺少店铺 ID', icon: 'none' })
		return
	}
	void loadStatistics()
})

onReady(() => {
	const sys = uni.getSystemInfoSync()
	canvasWidth.value = Math.max(280, (sys.windowWidth || 375) - 48)
})

function formatDate(date) {
	const y = date.getFullYear()
	const m = String(date.getMonth() + 1).padStart(2, '0')
	const d = String(date.getDate()).padStart(2, '0')
	return `${y}-${m}-${d}`
}

function resolveDateRange() {
	const today = new Date()
	today.setHours(0, 0, 0, 0)

	if (periodPreset.value === 'custom') {
		if (Array.isArray(customDateRange.value) && customDateRange.value.length === 2) {
			return {
				startDate: customDateRange.value[0],
				endDate: customDateRange.value[1]
			}
		}
		return { startDate: todayStr.value, endDate: todayStr.value }
	}

	const dayMap = {
		today: 1,
		'3d': 3,
		'7d': 7,
		'30d': 30
	}
	const days = dayMap[periodPreset.value] || 1
	const start = new Date(today)
	start.setDate(start.getDate() - (days - 1))
	return {
		startDate: formatDate(start),
		endDate: formatDate(today)
	}
}

function buildFilterShopId() {
	if (selectedScope.value.startsWith('self:')) {
		return shopId.value
	}
	if (selectedScope.value.startsWith('shop:')) {
		return selectedScope.value.split(':')[1]
	}
	return null
}

async function loadStatistics() {
	const { startDate, endDate } = resolveDateRange()
	const res = await fetchShopStatistics({
		shopId: shopId.value,
		filterShopId: buildFilterShopId(),
		startDate,
		endDate
	})
	if (!res.ok) {
		uni.showToast({ title: res.msg || '加载失败', icon: 'none' })
		statistics.value = null
		loading.value = false
		refreshing.value = false
		return
	}
	statistics.value = res.data
	loading.value = false
	refreshing.value = false
	await nextTick()
	drawTrendChart()
}

function switchScope(value) {
	if (selectedScope.value === value) return
	selectedScope.value = value
	loading.value = true
	void loadStatistics()
}

function switchPeriod(value) {
	if (periodPreset.value === value) return
	periodPreset.value = value
	if (value !== 'custom') {
		customDateRange.value = []
		loading.value = true
		void loadStatistics()
	}
}

function onCustomDateChange(value) {
	if (Array.isArray(value) && value.length === 2) {
		loading.value = true
		void loadStatistics()
	}
}

function onRefresh() {
	refreshing.value = true
	void loadStatistics()
}

function formatNumber(value) {
	const num = Number(value ?? 0)
	if (Number.isNaN(num)) return '0'
	return Number.isInteger(num) ? String(num) : num.toFixed(0)
}

function formatMoney(value) {
	const num = Number(value ?? 0)
	if (Number.isNaN(num)) return '0.00'
	return num.toFixed(2)
}

function drawTrendChart() {
	const trend = statistics.value?.trend || []
	if (!trend.length) return
	const instance = getCurrentInstance()
	const ctx = uni.createCanvasContext('statsTrendCanvas', instance?.proxy)
	const width = canvasWidth.value
	const height = canvasHeight.value
	const padding = { top: 16, right: 12, bottom: 28, left: 36 }
	const plotW = width - padding.left - padding.right
	const plotH = height - padding.top - padding.bottom

	const users = trend.map((item) => Number(item.newUsers ?? 0))
	const orders = trend.map((item) => Number(item.orders ?? 0))
	const points = trend.map((item) => Number(item.pointsUsed ?? 0))
	const maxVal = Math.max(1, ...users, ...orders, ...points)

	ctx.clearRect(0, 0, width, height)
	ctx.setStrokeStyle('#e8edf3')
	ctx.setLineWidth(1)
	for (let i = 0; i <= 4; i++) {
		const y = padding.top + (plotH / 4) * i
		ctx.beginPath()
		ctx.moveTo(padding.left, y)
		ctx.lineTo(width - padding.right, y)
		ctx.stroke()
	}

	const xStep = trend.length > 1 ? plotW / (trend.length - 1) : 0
	const toY = (val) => padding.top + plotH - (val / maxVal) * plotH

	function drawLine(data, color) {
		ctx.setStrokeStyle(color)
		ctx.setLineWidth(2)
		ctx.beginPath()
		data.forEach((val, idx) => {
			const x = padding.left + xStep * idx
			const y = toY(val)
			if (idx === 0) ctx.moveTo(x, y)
			else ctx.lineTo(x, y)
		})
		ctx.stroke()
	}

	drawLine(users, '#007aff')
	drawLine(orders, '#34c759')
	drawLine(points, '#ff9500')

	ctx.setFillStyle('#8a94a6')
	ctx.setFontSize(10)
	trend.forEach((item, idx) => {
		const label = (item.date || '').slice(5)
		const x = padding.left + xStep * idx
		ctx.fillText(label, x - 12, height - 8)
	})

	ctx.draw()
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	background: #f5f5f5;
}

.scroll {
	height: 100vh;
}

.scroll-inner {
	padding: 12px 16px 24px;
	box-sizing: border-box;
}

.state-wrap {
	padding: 48px 16px;
	text-align: center;
}

.state-text {
	color: #8a94a6;
	font-size: 14px;
}

.period-bar {
	margin-bottom: 12px;
}

.period-track {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
}

.period-chip {
	padding: 6px 12px;
	border-radius: 16px;
	background: #fff;
}

.period-chip--active {
	background: rgba(0, 122, 255, 0.12);
}

.period-chip-text {
	font-size: 13px;
	color: #007aff;
}

.custom-range-wrap {
	margin-top: 10px;
	background: #fff;
	border-radius: 12px;
	padding: 8px 10px;
}

.scope-bar {
	margin-bottom: 12px;
}

.scope-track {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
}

.scope-chip {
	padding: 6px 12px;
	border-radius: 16px;
	background: #fff;
}

.scope-chip--active {
	background: rgba(0, 122, 255, 0.12);
}

.scope-chip-text {
	font-size: 13px;
	color: #007aff;
}

.stats-grid {
	display: flex;
	flex-wrap: wrap;
	margin: 0 -5px;
}

.stat-card {
	width: 50%;
	padding: 0 5px 10px;
	box-sizing: border-box;
}

.stat-card > .stat-label,
.stat-card > .stat-value {
	display: block;
	background: #fff;
	border-radius: 12px;
	padding-left: 12px;
	padding-right: 12px;
}

.stat-card > .stat-label {
	padding-top: 14px;
	padding-bottom: 0;
	font-size: 12px;
	color: #8a94a6;
	margin-bottom: 0;
}

.stat-card > .stat-value {
	padding-top: 8px;
	padding-bottom: 14px;
	font-size: 22px;
	font-weight: 700;
	color: #1f2a37;
}

.stat-value--money {
	font-size: 18px;
}

.chart-card {
	margin-top: 2px;
	background: #fff;
	border-radius: 12px;
	padding: 14px 12px 16px;
}

.chart-title {
	display: block;
	font-size: 15px;
	font-weight: 600;
	color: #1f2a37;
	margin-bottom: 8px;
}

.trend-canvas {
	display: block;
}

.legend-row {
	display: flex;
	flex-wrap: wrap;
	gap: 12px;
	margin-top: 8px;
}

.legend-item {
	display: flex;
	align-items: center;
	gap: 6px;
	font-size: 12px;
	color: #667085;
}

.legend-dot {
	width: 8px;
	height: 8px;
	border-radius: 50%;
	flex-shrink: 0;
}

.legend-dot--users {
	background: #007aff;
}

.legend-dot--orders {
	background: #34c759;
}

.legend-dot--points {
	background: #ff9500;
}
</style>
