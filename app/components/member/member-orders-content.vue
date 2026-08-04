<template>
	<view class="orders-root" :class="isDark ? 'orders-root--dark' : 'orders-root--light'">
		<view class="orders-tab-bar">
			<view
				v-for="tab in tabs"
				:key="tab.key"
				class="orders-tab-item"
				:class="{ 'orders-tab-item--active': activeTab === tab.key }"
				@click="switchTab(tab.key)"
			>
				<text class="orders-tab-text">{{ tab.label }}</text>
			</view>
			<view class="orders-tab-indicator" :class="{ 'orders-tab-indicator--right': activeTab === 'verified' }" />
		</view>

		<view v-if="pullDistance > 0 || refreshing" class="pull-status-bar">
			<text class="pull-status-text">{{ pullStatusText }}</text>
		</view>

		<scroll-view
			class="orders-scroll"
			scroll-y
			:lower-threshold="80"
			:refresher-enabled="true"
			:refresher-threshold="REFRESH_PULL_THRESHOLD"
			:refresher-default-style="pullRefreshStyle"
			:refresher-triggered="refreshing"
			@refresherpulling="onRefresherPulling"
			@refresherrefresh="onRefresh"
			@refresherabort="onRefresherAbort"
			@refresherrestore="onRefresherRestore"
			@scrolltolower="loadMore"
		>
			<view v-if="listLoading && !rows.length" class="orders-state">
				<text class="orders-state-text">加载中…</text>
			</view>
			<view v-else-if="!rows.length" class="orders-state">
				<text class="orders-state-text">暂无{{ activeTabLabel }}订单</text>
			</view>
			<template v-else>
				<view
					v-for="row in rows"
					:key="row.logId"
					class="order-card"
					hover-class="tap-hover-opacity-strong"
					:hover-stay-time="70"
					@click="openDetail(row)"
				>
					<view class="order-card-head">
						<text class="order-shop">{{ row.shopName || '店铺' }}</text>
						<text
							class="order-status-tag"
							:class="`order-status-tag--${statusTagClass(row.status)}`"
						>{{ statusLabel(row.status) }}</text>
					</view>
					<view class="order-card-body">
						<view class="order-card-main">
							<text v-if="formatOrderItemsSummary(row)" class="order-products">{{ formatOrderItemsSummary(row) }}</text>
						</view>
						<text class="order-points">-{{ formatPointsAmount(row.consumePoints) }} 积分</text>
					</view>
					<view class="order-card-footer">
						<text class="order-date">{{ formatDateTime(row.createTime) }}</text>
						<text class="order-detail-hint">查看详情 ›</text>
					</view>
				</view>
				<view v-if="loadingMore" class="orders-state orders-state--inline">
					<text class="orders-state-text">加载更多…</text>
				</view>
				<view v-else-if="finished" class="orders-state orders-state--inline">
					<text class="orders-state-text">没有更多了</text>
				</view>
			</template>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchMyOrderList } from '@/api/modules/points.js'
import { POINTS_LOG_STATUS, pointsLogStatusLabel, pointsLogStatusTagClass } from '@/utils/points-log-status.js'
import { formatDateTime } from '@/utils/datetime-format.js'
import { formatPointsAmount } from '@/utils/points-format.js'
import { usePaginatedList } from '@/composables/use-paginated-list.js'
import {
	getMemberOrdersCache,
	setMemberOrdersCache,
	isMemberOrdersCacheReady,
	invalidateMemberOrdersCache,
	getMemberOrdersCacheEpoch
} from '@/utils/member-orders-cache.js'

defineProps({
	isDark: { type: Boolean, default: false }
})

const tabs = [
	{ key: 'unused', label: '待使用', status: POINTS_LOG_STATUS.PENDING_USE },
	{ key: 'verified', label: '已核销', excludeStatus: POINTS_LOG_STATUS.PENDING_USE }
]

const activeTab = ref('unused')
const pageSize = 10
const REFRESH_PULL_THRESHOLD = 72
const pullDistance = ref(0)
const refreshReady = ref(false)
let syncedEpoch = getMemberOrdersCacheEpoch()

const pullRefreshStyle = computed(() => (refreshing.value ? 'black' : 'none'))

const activeTabMeta = computed(() => tabs.find((t) => t.key === activeTab.value) || tabs[0])
const activeTabLabel = computed(() => activeTabMeta.value.label)

const pullStatusText = computed(() => {
	if (refreshing.value) return '刷新中…'
	if (refreshReady.value) return '松手刷新'
	return '继续下拉刷新'
})

const {
	rows,
	listLoading,
	loadingMore,
	refreshing,
	finished,
	pageNum,
	reloadList: fetchList,
	loadMore
} = usePaginatedList({
	pageSize,
	fetchPage: (num, size) => {
		const tabMeta = activeTabMeta.value
		const query = {
			pageNum: num,
			pageSize: size,
			actionType: 2
		}
		if (tabMeta.status != null) query.status = tabMeta.status
		if (tabMeta.excludeStatus != null) query.excludeStatus = tabMeta.excludeStatus
		return fetchMyOrderList(query)
	}
})

function restoreFromCache(tabKey) {
	const cached = getMemberOrdersCache(tabKey)
	if (!cached) return false
	rows.value = cached.rows
	finished.value = cached.finished
	pageNum.value = cached.pageNum
	return true
}

function saveToCache(tabKey) {
	setMemberOrdersCache(tabKey, {
		rows: rows.value,
		finished: finished.value,
		pageNum: pageNum.value
	})
}

function statusLabel(status) {
	return pointsLogStatusLabel(status)
}

function statusTagClass(status) {
	return pointsLogStatusTagClass(status)
}

function formatOrderItemsSummary(row) {
	const items = row?.items
	if (!items?.length) return ''
	return items
		.map((item) => {
			const name = item.productName?.trim() || '商品'
			const count = item.count != null && item.count > 0 ? item.count : 1
			return `${name}×${count}`
		})
		.join('、')
}

async function reloadList(force = false) {
	const tabKey = activeTab.value
	if (!force && isMemberOrdersCacheReady(tabKey) && restoreFromCache(tabKey)) {
		syncedEpoch = getMemberOrdersCacheEpoch()
		return
	}
	await fetchList()
	saveToCache(tabKey)
	syncedEpoch = getMemberOrdersCacheEpoch()
}

async function refreshIfStale() {
	if (syncedEpoch === getMemberOrdersCacheEpoch()) return
	await reloadList(true)
}

function resetPullRefreshState() {
	pullDistance.value = 0
	refreshReady.value = false
}

function onRefresherPulling(e) {
	const dy = Number(e?.detail?.dy ?? 0)
	pullDistance.value = Math.min(dy, REFRESH_PULL_THRESHOLD)
	refreshReady.value = dy >= REFRESH_PULL_THRESHOLD
}

function onRefresherAbort() {
	resetPullRefreshState()
	refreshing.value = false
}

function onRefresherRestore() {
	resetPullRefreshState()
}

async function onRefresh() {
	if (!refreshReady.value) {
		refreshing.value = false
		resetPullRefreshState()
		return
	}
	refreshing.value = true
	invalidateMemberOrdersCache(activeTab.value)
	try {
		await reloadList(true)
	} finally {
		refreshing.value = false
		resetPullRefreshState()
	}
}

function switchTab(key) {
	if (activeTab.value === key) return
	activeTab.value = key
	if (getMemberOrdersCacheEpoch() !== syncedEpoch) {
		void reloadList(true)
		return
	}
	if (isMemberOrdersCacheReady(key) && restoreFromCache(key)) {
		return
	}
	void reloadList(false)
}

function openDetail(row) {
	if (!row?.logId || !row?.shopId) return
	uni.navigateTo({
		url: `/pages/member/order-detail?shopId=${row.shopId}&logId=${row.logId}`,
		animationType: 'slide-in-right',
		animationDuration: 200,
		success(res) {
			res.eventChannel?.emit('order', row)
		}
	})
}

defineExpose({ reload: reloadList, refreshIfStale })

onMounted(() => {
	void reloadList()
})
</script>

<style scoped>
.orders-root {
	flex: 1;
	min-height: 0;
	width: 100%;
	height: 100%;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}

.orders-tab-bar {
	position: relative;
	display: flex;
	margin-bottom: 12px;
	border-radius: 12px;
	overflow: hidden;
	flex-shrink: 0;
}

.orders-root--light .orders-tab-bar {
	background: #fff;
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.orders-root--dark .orders-tab-bar {
	background: #1e1e1e;
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.25);
}

.orders-tab-item {
	flex: 1;
	padding: 12px 0;
	text-align: center;
	position: relative;
	z-index: 1;
}

.orders-tab-text {
	font-size: 14px;
}

.orders-root--light .orders-tab-text {
	color: #666666;
}

.orders-root--dark .orders-tab-text {
	color: #a8a8a8;
}

.orders-tab-item--active .orders-tab-text {
	opacity: 1;
	font-weight: 600;
	color: #007aff;
}

.orders-root--dark .orders-tab-item--active .orders-tab-text {
	color: #5ac8fa;
}

.orders-tab-indicator {
	position: absolute;
	left: 0;
	bottom: 0;
	width: 50%;
	height: 2px;
	background: #007aff;
	transition: transform 0.2s ease;
}

.orders-root--dark .orders-tab-indicator {
	background: #5ac8fa;
}

.orders-tab-indicator--right {
	transform: translateX(100%);
}

.pull-status-bar {
	flex-shrink: 0;
	padding: 6px 0 10px;
	text-align: center;
}

.pull-status-text {
	font-size: 12px;
	opacity: 0.55;
}

.orders-scroll {
	flex: 1;
	height: 0;
	width: 100%;
	overflow: hidden;
}

.orders-state {
	padding: 48px 16px;
	text-align: center;
}

.orders-state--inline {
	padding: 16px;
}

.orders-state-text {
	font-size: 14px;
}

.orders-root--light .orders-state-text {
	color: #666666;
}

.orders-root--dark .orders-state-text {
	color: #a0a0a0;
}

.order-card {
	border-radius: 12px;
	padding: 14px 16px;
	margin-bottom: 10px;
}

.orders-root--light .order-card {
	background: #fff;
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.orders-root--dark .order-card {
	background: #1e1e1e;
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.25);
}

.order-card-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 8px;
	margin-bottom: 6px;
}

.order-shop {
	font-size: 15px;
	font-weight: 600;
	flex: 1;
	min-width: 0;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.order-status-tag {
	font-size: 11px;
	font-weight: 600;
	padding: 2px 8px;
	border-radius: 10px;
	flex-shrink: 0;
	line-height: 1.5;
}

.order-status-tag--pending {
	color: #007aff;
	background: rgba(0, 122, 255, 0.12);
}

.order-status-tag--verified {
	color: #34c759;
	background: rgba(52, 199, 89, 0.12);
}

.order-status-tag--cancelled {
	color: #ff9500;
	background: rgba(255, 149, 0, 0.14);
}

.order-status-tag--expired,
.order-status-tag--unknown {
	color: #8e8e93;
	background: rgba(142, 142, 147, 0.16);
}

.order-status-tag--recharge {
	color: #5856d6;
	background: rgba(88, 86, 214, 0.12);
}

.orders-root--dark .order-status-tag--pending {
	color: #5ac8fa;
	background: rgba(90, 200, 250, 0.16);
}

.orders-root--dark .order-status-tag--verified {
	color: #30d158;
	background: rgba(48, 209, 88, 0.16);
}

.orders-root--dark .order-status-tag--cancelled {
	color: #ffb340;
	background: rgba(255, 179, 64, 0.18);
}

.orders-root--dark .order-status-tag--expired,
.orders-root--dark .order-status-tag--unknown {
	color: #a0a0a0;
	background: rgba(160, 160, 160, 0.18);
}

.orders-root--dark .order-status-tag--recharge {
	color: #9d9aff;
	background: rgba(157, 154, 255, 0.18);
}

.order-card-body {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 12px;
}

.order-card-main {
	flex: 1;
	min-width: 0;
}

.order-products {
	display: block;
	font-size: 13px;
	opacity: 0.75;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.order-card-footer {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 8px;
	margin-top: 6px;
}

.order-date {
	font-size: 12px;
	opacity: 0.55;
	flex: 1;
	min-width: 0;
}

.order-points {
	font-size: 15px;
	font-weight: 700;
	color: #e64340;
	flex-shrink: 0;
	line-height: 1.4;
}

.order-detail-hint {
	font-size: 12px;
	opacity: 0.45;
	flex-shrink: 0;
}
</style>
