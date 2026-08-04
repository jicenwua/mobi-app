<template>
	<view v-if="listLoading" class="state-wrap state-wrap--inline">
		<text class="state-text">加载中…</text>
	</view>
	<view v-else-if="!coupons.length" class="state-wrap state-wrap--inline">
		<text class="state-text">暂无数据</text>
	</view>
	<template v-else>
		<view v-for="item in coupons" :key="item.templateId" class="list-card">
			<view class="row-main">
				<text class="row-name">{{ item.couponName }}</text>
				<text class="row-tag">{{ couponTypeLabel(item.type) }}</text>
			</view>
			<view class="row-sub">
				<text class="row-meta">{{ formatCouponRule(item) }}</text>
				<text class="row-status">{{ formatCouponIssued(item) }}</text>
			</view>
			<view class="row-sub">
				<text class="row-meta">有效期 {{ formatValidDays(item) }}</text>
				<view class="phase-tag" :class="couponPhaseTagClass(couponStatusCode(item))">
					<text class="phase-tag-text">{{ couponStatusLabel(item) }}</text>
				</view>
			</view>
			<view class="row-sub">
				<text class="row-meta">发放 {{ formatCouponDistributionPeriod(item.distributionStartTime, item.distributionEndTime) }}</text>
			</view>
			<view v-if="isManager" class="row-actions">
				<text v-if="canEditCoupon(item)" class="action-btn" @click="$emit('edit', item)">修改</text>
				<text v-if="canResumeCouponDistribution(item)" class="action-btn" @click="$emit('resume', item)">恢复发放</text>
				<text class="action-btn" @click="$emit('add-stock', item)">加库存</text>
				<text class="action-btn action-btn--danger" @click="$emit('delete', item)">删除</text>
			</view>
		</view>
	</template>
</template>

<script setup>
import {
	couponTypeLabel,
	formatCouponRule,
	formatCouponIssued,
	formatValidDays,
	couponStatusLabel,
	couponStatusCode,
	couponPhaseTagClass,
	formatCouponDistributionPeriod,
	canEditCoupon,
	canResumeCouponDistribution
} from '@/utils/coupon-manage-display.js'

defineProps({
	coupons: { type: Array, default: () => [] },
	listLoading: { type: Boolean, default: false },
	isManager: { type: Boolean, default: false }
})

defineEmits(['edit', 'resume', 'add-stock', 'delete'])
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

.row-meta,
.row-status {
	font-size: 12px;
	color: #888;
}

.phase-tag {
	flex-shrink: 0;
	align-self: flex-start;
	padding: 2px 8px;
	border-radius: 10px;
}

.phase-tag-text {
	font-size: 11px;
	font-weight: 500;
	line-height: 1.4;
}

.phase-tag--pending {
	background: rgba(0, 122, 255, 0.1);
}

.phase-tag--pending .phase-tag-text {
	color: #007aff;
}

.phase-tag--active {
	background: rgba(52, 199, 89, 0.12);
}

.phase-tag--active .phase-tag-text {
	color: #34c759;
}

.phase-tag--ended {
	background: #f0f0f0;
}

.phase-tag--ended .phase-tag-text {
	color: #999;
}

.phase-tag--stopped {
	background: rgba(255, 149, 0, 0.12);
}

.phase-tag--stopped .phase-tag-text {
	color: #ff9500;
}

.phase-tag--unknown {
	background: #f5f5f5;
}

.phase-tag--unknown .phase-tag-text {
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

.state-wrap {
	padding: 40px 16px;
	text-align: center;
}

.state-wrap--inline {
	padding: 20px 0;
}

.state-text {
	font-size: 14px;
	color: #999;
}
</style>
