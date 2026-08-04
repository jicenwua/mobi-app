<template>
	<view v-if="listLoading" class="state-wrap state-wrap--inline">
		<text class="state-text">加载中…</text>
	</view>
	<view v-else-if="!activities.length" class="state-wrap state-wrap--inline">
		<text class="state-text">暂无数据</text>
	</view>
	<template v-else>
		<view
			v-for="item in activities"
			:key="item.activityId"
			class="list-card"
			:class="{ 'list-card--dark': isDark }"
		>
			<view class="row-main">
				<text class="row-name">{{ item.activityName }}</text>
				<text class="row-tag">{{ activityTypeLabel(item) }}</text>
			</view>
			<view class="row-sub">
				<text class="row-meta">{{ formatActivityPeriod(item.startTime, item.endTime) }}</text>
				<view class="status-tag" :class="statusTagClass(activityStatusCode(item))">
					<text class="status-tag-text">{{ activityStatusLabel(item.status) }}</text>
				</view>
			</view>
			<view v-if="isManager" class="row-actions">
				<text class="action-btn" @click="emit('edit', item)">修改</text>
				<text
					v-if="item.status === 0 || item.status === 1"
					class="action-btn action-btn--warn"
					@click="emit('stop', item)"
				>停止</text>
				<text
					v-if="item.status === 3"
					class="action-btn action-btn--resume"
					@click="emit('enable', item)"
				>启用</text>
				<text class="action-btn action-btn--danger" @click="emit('delete', item)">删除</text>
			</view>
		</view>
	</template>
</template>

<script setup>
import { formatDateTime } from '@/utils/datetime-format.js'

defineProps({
	activities: { type: Array, default: () => [] },
	listLoading: { type: Boolean, default: false },
	isManager: { type: Boolean, default: false },
	isDark: { type: Boolean, default: false }
})

const emit = defineEmits(['edit', 'delete', 'add', 'stop', 'enable'])

function activityTypeLabel(item) {
	if (item?.kind === 2) return '公告'
	if (item?.activityType === 1) return '充值满赠'
	if (item?.activityType === 2) return '消费满赠'
	return '—'
}

function formatActivityPeriod(startTime, endTime) {
	const start = startTime ? formatDateTime(startTime, { maxLen: 10 }) : '立即开始'
	const end = endTime ? formatDateTime(endTime, { maxLen: 10 }) : '永久'
	return `${start} ~ ${end}`
}

function activityStatusLabel(status) {
	if (status === 0) return '未开始'
	if (status === 1) return '进行中'
	if (status === 2) return '已结束'
	if (status === 3) return '手动停止'
	return '—'
}

function activityStatusCode(item) {
	const status = item?.status
	if (status === 0 || status === 1 || status === 2 || status === 3) return status
	return null
}

function statusTagClass(code) {
	if (code === 0) return 'status-tag--pending'
	if (code === 1) return 'status-tag--active'
	if (code === 2) return 'status-tag--ended'
	if (code === 3) return 'status-tag--stopped'
	return 'status-tag--unknown'
}
</script>

<style scoped>
.list-card {
	border-top: 1px solid #f0f0f0;
	padding: 12px 0;
}

.list-card:first-of-type {
	border-top: none;
	padding-top: 4px;
}

.row-main {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 4px;
}

.row-name {
	font-size: 15px;
	font-weight: 500;
	color: #333;
	flex: 1;
	min-width: 0;
}

.row-tag {
	font-size: 12px;
	color: #007aff;
	margin-left: 8px;
}

.row-sub {
	display: flex;
	justify-content: space-between;
	margin-bottom: 8px;
}

.row-meta {
	font-size: 12px;
	color: #888;
}

.status-tag {
	flex-shrink: 0;
	align-self: flex-start;
	padding: 2px 8px;
	border-radius: 10px;
}

.status-tag-text {
	font-size: 11px;
	font-weight: 500;
	line-height: 1.4;
}

.status-tag--pending {
	background: rgba(0, 122, 255, 0.1);
}

.status-tag--pending .status-tag-text {
	color: #007aff;
}

.status-tag--active {
	background: rgba(52, 199, 89, 0.12);
}

.status-tag--active .status-tag-text {
	color: #34c759;
}

.status-tag--ended {
	background: #f0f0f0;
}

.status-tag--ended .status-tag-text {
	color: #999;
}

.status-tag--stopped {
	background: rgba(255, 149, 0, 0.12);
}

.status-tag--stopped .status-tag-text {
	color: #ff9500;
}

.status-tag--unknown {
	background: #f5f5f5;
}

.status-tag--unknown .status-tag-text {
	color: #bbb;
}

.row-actions {
	display: flex;
	gap: 16px;
}

.action-btn {
	font-size: 13px;
	color: #007aff;
}

.action-btn--danger {
	color: #e64340;
}

.action-btn--warn {
	color: #ff9500;
}

.action-btn--resume {
	color: #34c759;
}

.state-wrap--inline {
	padding: 20px 0;
	text-align: center;
}

.state-text {
	font-size: 14px;
	color: #999;
}
</style>
