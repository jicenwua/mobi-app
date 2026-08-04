<template>
	<view class="page">
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
				<text class="state-text">暂无工单，点击下方按钮新建</text>
			</view>

			<view
				v-for="item in rows"
				:key="item.ticketId"
				class="ticket-card"
				hover-class="tap-hover-row"
				:hover-stay-time="70"
				@click="goDetail(item.ticketId)"
			>
				<view class="ticket-head">
					<view class="title-wrap">
						<view v-if="item.unreadCount > 0" class="unread-badge">
							<text class="unread-badge-text">{{ formatUnreadCount(item.unreadCount) }}</text>
						</view>
						<text class="ticket-title">{{ item.title }}</text>
					</view>
					<text class="ticket-status" :class="statusClass(item.status)">{{ item.statusLabel }}</text>
				</view>
				<text class="ticket-desc">{{ item.description }}</text>
				<text class="ticket-time">{{ formatDateTime(item.createTime) }}</text>
			</view>

			<view v-if="loadingMore" class="state-wrap state-wrap--inline">
				<text class="state-text">加载更多…</text>
			</view>
			<view v-else-if="finished && rows.length" class="state-wrap state-wrap--inline">
				<text class="state-text">没有更多了</text>
			</view>
		</scroll-view>

		<view class="footer">
			<button class="create-btn" hover-class="tap-hover-opacity-light" :hover-stay-time="70" @click="goCreate">
				新建工单
			</button>
		</view>
	</view>
</template>

<script setup>
import { onLoad, onShow } from '@dcloudio/uni-app'
import { fetchMyTickets } from '@/api/modules/ticket.js'
import { connectNotifySocket } from '@/services/notify-socket.js'
import { usePaginatedList } from '@/composables/use-paginated-list.js'
import { useTicketNotify } from '@/composables/use-ticket-notify.js'
import { useDebounceFn } from '@/composables/use-debounce-fn.js'
import { formatDateTime } from '@/utils/datetime-format.js'
import { ticketStatusClass } from '@/utils/ticket-status.js'

const PAGE_SIZE = 20

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
	pageSize: PAGE_SIZE,
	fetchPage: (pageNum, pageSize) => fetchMyTickets({ pageNum, pageSize })
})

const { run: debouncedReloadList } = useDebounceFn(() => {
	void reloadList()
}, 300)

let pageReady = false
let needRefreshOnShow = false

const { subscribe: subscribeTicketNotify } = useTicketNotify((payload) => {
	if (
		payload?.type === 'unread_changed'
		|| payload?.type === 'ticket_message'
		|| payload?.type === 'ticket_status_changed'
	) {
		debouncedReloadList()
	}
})

function formatUnreadCount(count) {
	const value = Number(count) || 0
	if (value > 99) return '99+'
	return String(value)
}

function statusClass(status) {
	return ticketStatusClass(status)
}

onLoad(() => {
	subscribeTicketNotify()
	void reloadList().finally(() => {
		pageReady = true
	})
})

onShow(() => {
	connectNotifySocket()
	if (!pageReady) return
	if (needRefreshOnShow) {
		needRefreshOnShow = false
		void reloadList()
	}
})

function goCreate() {
	needRefreshOnShow = true
	uni.navigateTo({
		url: '/pages/mine/ticket-create',
		animationType: 'slide-in-right',
		animationDuration: 200
	})
}

function goDetail(ticketId) {
	needRefreshOnShow = true
	uni.navigateTo({
		url: `/pages/mine/ticket-detail?id=${ticketId}`,
		animationType: 'slide-in-right',
		animationDuration: 200
	})
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	background: #f5f5f5;
	display: flex;
	flex-direction: column;
	box-sizing: border-box;
	padding-bottom: calc(72px + env(safe-area-inset-bottom));
}

.scroll {
	flex: 1;
	height: calc(100vh - 72px - env(safe-area-inset-bottom));
	padding: 12px 16px 0;
	box-sizing: border-box;
}

.ticket-card {
	background: #fff;
	border-radius: 12px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.ticket-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
	margin-bottom: 8px;
}

.title-wrap {
	display: flex;
	align-items: center;
	gap: 8px;
	flex: 1;
	min-width: 0;
}

.unread-badge {
	min-width: 18px;
	height: 18px;
	padding: 0 5px;
	border-radius: 9px;
	background: #f56c6c;
	flex-shrink: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
}

.unread-badge-text {
	font-size: 11px;
	line-height: 1;
	color: #fff;
	font-weight: 600;
}

.ticket-title {
	font-size: 16px;
	font-weight: 600;
	flex: 1;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.ticket-status {
	font-size: 12px;
	padding: 2px 8px;
	border-radius: 999px;
	flex-shrink: 0;
}

.status--pending {
	color: #e6a23c;
	background: #fdf6ec;
}

.status--processing {
	color: #409eff;
	background: #ecf5ff;
}

.status--done {
	color: #909399;
	background: #f4f4f5;
}

.ticket-desc {
	font-size: 14px;
	color: #666;
	line-height: 1.5;
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
	overflow: hidden;
	margin-bottom: 8px;
}

.ticket-time {
	font-size: 12px;
	color: #999;
}

.state-wrap {
	padding: 48px 16px;
	text-align: center;
}

.state-wrap--inline {
	padding: 16px;
}

.state-text {
	font-size: 14px;
	color: #999;
}

.footer {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
	background: #f5f5f5;
	box-sizing: border-box;
}

.create-btn {
	width: 100%;
	height: 48px;
	line-height: 48px;
	background: #007aff;
	color: #fff;
	border-radius: 12px;
	font-size: 16px;
	border: none;
}

.create-btn::after {
	border: none;
}
</style>
