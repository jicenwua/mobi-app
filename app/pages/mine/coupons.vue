<template>
	<view class="page">
		<view class="tab-bar">
			<view
				v-for="tab in tabs"
				:key="tab.key"
				class="tab-item"
				:class="{ active: activeTab === tab.key }"
				@click="switchTab(tab.key)"
			>
				<text class="tab-text">{{ tab.label }}</text>
			</view>
			<view class="tab-slider" :style="tabSliderStyle" />
		</view>

		<scroll-view
			class="scroll"
			scroll-y
			:refresher-enabled="true"
			:refresher-triggered="refreshing"
			@refresherrefresh="onRefresh"
			@scrolltolower="loadMore"
		>
			<view v-if="listLoading && !rows.length" class="state-wrap">
				<text class="state-text">加载中…</text>
			</view>
			<view v-else-if="!rows.length" class="state-wrap">
				<text class="state-text">暂无{{ activeTabLabel }}优惠券</text>
			</view>

			<view
				v-for="item in rows"
				:key="item.userCouponId"
				class="coupon-card"
				:class="cardClass(item)"
			>
				<image class="coupon-icon" :src="couponIconSrc(item.type)" mode="aspectFit" />
				<view class="coupon-body">
					<view class="coupon-head">
						<text class="coupon-name">{{ item.couponName || '店铺优惠券' }}</text>
						<text class="coupon-type">{{ couponTypeLabel(item.type) }}</text>
					</view>
					<text class="coupon-desc">{{ formatCouponDesc(item, { emptyValueFallback: true }) }}</text>
					<text v-if="item.shopName" class="coupon-shop">{{ item.shopName }}</text>
					<text class="coupon-time">{{ formatCouponTime(item) }}</text>
				</view>
			</view>

			<view v-if="loadingMore" class="state-wrap state-wrap--inline">
				<text class="state-text">加载更多…</text>
			</view>
			<view v-else-if="finished && rows.length" class="state-wrap state-wrap--inline">
				<text class="state-text">没有更多了</text>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchMyCoupons } from '@/api/modules/coupon.js'
import { formatDateTime } from '@/utils/datetime-format.js'
import {
	couponIconSrc,
	couponTypeLabel,
	formatCouponDesc
} from '@/utils/coupon-display.js'
import { usePaginatedList } from '@/composables/use-paginated-list.js'

const tabs = [
	{ key: 'unused', label: '待使用', status: 0 },
	{ key: 'expired', label: '已过期', status: 2 },
	{ key: 'used', label: '已使用', status: 1 }
]

const activeTab = ref('unused')

const activeTabMeta = computed(() => tabs.find((t) => t.key === activeTab.value) || tabs[0])
const activeTabLabel = computed(() => activeTabMeta.value.label)

const {
	rows,
	listLoading,
	loadingMore,
	refreshing,
	finished,
	reloadList,
	onRefresh,
	loadMore
} = usePaginatedList({
	pageSize: 20,
	fetchPage: (pageNum, pageSize) =>
		fetchMyCoupons({
			status: activeTabMeta.value.status,
			pageNum,
			pageSize
		})
})

const tabSliderStyle = computed(() => {
	const idx = tabs.findIndex((t) => t.key === activeTab.value)
	const width = 100 / tabs.length
	return {
		width: `calc(${width}% - 3px)`,
		transform: `translateX(${idx * 100}%)`
	}
})

onLoad(() => {
	void reloadList()
})

function switchTab(key) {
	if (activeTab.value === key) return
	activeTab.value = key
	void reloadList()
}

function cardClass(item) {
	if (activeTab.value === 'unused') return ''
	if (activeTab.value === 'expired') return 'coupon-card--muted'
	return 'coupon-card--muted'
}

function formatCouponTime(item) {
	if (activeTab.value === 'used') {
		return item.usedTime ? `使用时间 ${formatDateTime(item.usedTime, { empty: '', maxLen: 16 })}` : ''
	}
	if (item.expireTime) {
		return activeTab.value === 'expired'
			? `已于 ${formatDateTime(item.expireTime, { empty: '', maxLen: 16 })} 过期`
			: `有效期至 ${formatDateTime(item.expireTime, { empty: '', maxLen: 16 })}`
	}
	return item.receiveTime ? `领取于 ${formatDateTime(item.receiveTime, { empty: '', maxLen: 16 })}` : ''
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	background: #f5f5f5;
	display: flex;
	flex-direction: column;
}

.tab-bar {
	display: flex;
	position: relative;
	margin: 12px 16px 0;
	background: #e8e8e8;
	border-radius: 8px;
	padding: 3px;
	flex-shrink: 0;
}

.tab-item {
	flex: 1;
	z-index: 1;
	padding: 8px 0;
	text-align: center;
}

.tab-text {
	font-size: 14px;
	color: #666;
}

.tab-item.active .tab-text {
	color: #fff;
	font-weight: 600;
}

.tab-slider {
	position: absolute;
	top: 3px;
	left: 3px;
	height: calc(100% - 6px);
	background: #007aff;
	border-radius: 6px;
	transition: transform 0.25s ease;
}

.scroll {
	flex: 1;
	height: 0;
	padding: 12px 16px 16px;
	box-sizing: border-box;
}

.coupon-card {
	display: flex;
	align-items: flex-start;
	gap: 12px;
	background: #fff;
	border-radius: 10px;
	padding: 14px;
	margin-bottom: 10px;
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.coupon-card--muted {
	opacity: 0.72;
}

.coupon-icon {
	width: 44px;
	height: 44px;
	flex-shrink: 0;
}

.coupon-body {
	flex: 1;
	min-width: 0;
}

.coupon-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 8px;
	margin-bottom: 4px;
}

.coupon-name {
	font-size: 15px;
	font-weight: 600;
	color: #222;
	flex: 1;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.coupon-type {
	font-size: 11px;
	color: #007aff;
	background: rgba(0, 122, 255, 0.1);
	padding: 2px 6px;
	border-radius: 4px;
	flex-shrink: 0;
}

.coupon-desc {
	display: block;
	font-size: 13px;
	color: #e65c00;
	margin-bottom: 4px;
}

.coupon-shop {
	display: block;
	font-size: 12px;
	color: #666;
	margin-bottom: 2px;
}

.coupon-time {
	display: block;
	font-size: 12px;
	color: #666666;
}

.state-wrap {
	padding: 48px 0;
	text-align: center;
}

.state-wrap--inline {
	padding: 16px 0;
}

.state-text {
	font-size: 14px;
	color: #666666;
}
</style>
