<template>
	<view class="page">
		<SegmentedTabs :model-value="activeTab" :tabs="tabs" class="records-tabs" @update:model-value="switchTab" />

		<view class="toolbar">
			<view class="search-bar">
				<uni-icons class="search-icon" type="search" :size="22" color="#999" />
				<input
					v-model="searchKeyword"
					class="search-input"
					type="text"
					confirm-type="search"
					:placeholder="searchPlaceholder"
					placeholder-class="search-placeholder"
					@confirm="onSearchConfirm"
					@input="onSearchInput"
				/>
				<view v-if="searchKeyword" class="search-clear" @click="clearSearch">
					<text class="search-clear-text">×</text>
				</view>
			</view>

			<view v-if="activeTab === 'account'" class="sort-bar">
				<view class="sort-track">
					<view
						v-for="opt in accountSortOptions"
						:key="opt.value"
						class="sort-item"
						:class="{ 'sort-item--active': accountSortType === opt.value }"
						@click="switchAccountSort(opt.value)"
					>
						<text class="sort-item-text">{{ opt.label }}</text>
					</view>
				</view>
			</view>
		</view>

		<scroll-view
			class="scroll"
			:class="{ 'scroll--cards': activeTab === 'account' }"
			scroll-y
			:refresher-enabled="true"
			:refresher-triggered="refreshing"
			@refresherrefresh="onRefresh"
			@scrolltolower="loadMore"
		>
			<view class="scroll-inner">
			<view v-if="listLoading && !rows.length" class="state-wrap">
				<text class="state-text">加载中…</text>
			</view>
			<view v-else-if="!rows.length" class="state-wrap">
				<text class="state-text">{{ emptyText }}</text>
			</view>

			<template v-else-if="activeTab === 'log'">
				<view
					v-for="item in rows"
					:key="item.logId"
					class="list-card"
					hover-class="list-card--hover"
					:hover-stay-time="70"
					@click="openLogDetail(item)"
				>
					<view class="card-head">
						<text class="card-name">{{ item.nickname || '会员' }}</text>
						<text class="card-status">{{ logStatusOrActionLabel(item) }}</text>
					</view>
					<view class="card-main">
						<text class="card-date">{{ formatDateTime(item.consumeTime || item.createTime) }}</text>
						<text class="card-points" :class="logPointsClass(item)">{{ formatLogPoints(item) }}</text>
					</view>
					<view v-if="item.phone" class="card-sub">
						<text class="card-meta card-meta--left">{{ item.phone }}</text>
						<text class="card-meta card-meta--right">余额 {{ formatPointsAmount(item.remainingPoints) }} 积分</text>
					</view>
					<view v-else class="card-sub">
						<text class="card-meta card-meta--left">余额 {{ formatPointsAmount(item.remainingPoints) }} 积分</text>
					</view>
					<text class="card-hint">查看详情 ›</text>
				</view>
			</template>

			<template v-else>
				<view v-for="item in accountList" :key="item.id" class="list-card list-card--account">
					<view class="account-head">
						<view class="account-info">
							<text class="account-name">{{ item.nickname }}</text>
							<text v-if="item.phone" class="account-phone">{{ item.phone }}</text>
						</view>
						<text class="account-recharge" @click.stop="openRecharge(item.source)">添加积分</text>
					</view>
					<view class="account-points">
						<text class="account-stat">
							剩余 <text class="account-num">{{ item.remainingText }}</text>
						</text>
						<text class="account-sep">·</text>
						<text class="account-stat">
							已用 <text class="account-num account-num--used">{{ item.usedText }}</text>
						</text>
						<text v-if="item.showBreakdown" class="account-breakdown">
							基础 {{ item.baseText }} · 赠送 {{ item.bonusText }}
						</text>
					</view>
				</view>
			</template>

			<view v-if="loadingMore" class="state-wrap state-wrap--inline">
				<text class="state-text">加载更多…</text>
			</view>
			<view v-else-if="finished && rows.length" class="state-wrap state-wrap--inline">
				<text class="state-text">没有更多了</text>
			</view>
			</view>
		</scroll-view>

		<view v-if="rechargeVisible" class="recharge-mask" @click="closeRecharge">
			<view class="recharge-sheet" @click.stop>
				<text class="recharge-title">添加积分</text>
				<text class="recharge-sub">{{ rechargeTarget?.nickname || '会员' }}</text>
				<view v-if="shopRatio" class="recharge-ratio">
					<text class="recharge-ratio-text">1 元 = {{ shopRatio }} 积分</text>
				</view>
				<view class="recharge-field">
					<text class="recharge-label">充值金额（元）</text>
					<input
						v-model="rechargeAmount"
						class="recharge-input"
						type="digit"
						placeholder="请输入顾客付款金额"
						placeholder-class="recharge-placeholder"
						@input="onRechargeAmountInput"
					/>
				</view>
				<view v-if="rechargePreview.basePoints > 0" class="recharge-preview">
					<view class="preview-row">
						<text class="preview-label">基础积分</text>
						<text class="preview-value">{{ formatPointsAmount(rechargePreview.basePoints) }}</text>
					</view>
					<view v-if="rechargePreview.giftPoints > 0" class="preview-row">
						<text class="preview-label">满赠积分</text>
						<text class="preview-value preview-value--gift">+{{ formatPointsAmount(rechargePreview.giftPoints) }}</text>
					</view>
					<view class="preview-row preview-row--total">
						<text class="preview-label">合计到账</text>
						<text class="preview-value preview-value--total">{{ formatPointsAmount(rechargePreview.totalPoints) }}</text>
					</view>
				</view>
				<view class="recharge-footer">
					<view class="recharge-footer-btn recharge-footer-btn--ghost" @click="closeRecharge">取消</view>
					<view
						class="recharge-footer-btn recharge-footer-btn--primary"
						:class="{ 'recharge-footer-btn--disabled': rechargeSubmitting }"
						@click="submitRecharge"
					>
						<text>{{ rechargeSubmitting ? '提交中…' : '确认充值' }}</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed, reactive, watch } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchShopConsumeLogList, fetchShopPointsAccountList, rechargeMemberPoints } from '@/api/modules/points.js'
import { fetchShopOngoingActivities, fetchShopFromUserList } from '@/api/modules/shop.js'
import { isShopClerkCapable } from '@/utils/shop-role.js'
import { canManageShop } from '@/utils/wx-perm.js'
import { POINTS_LOG_STATUS, pointsLogStatusLabel, isPointsLogGain } from '@/utils/points-log-status.js'
import { formatPointsAmount } from '@/utils/points-format.js'
import { formatDateTime } from '@/utils/datetime-format.js'
import { navigateBackDelayed } from '@/utils/navigation.js'
import { bindOpenerShop, rememberShopDetail } from '@/utils/shop-page-context.js'
import { useDebounceFn } from '@/composables/use-debounce-fn.js'
import { usePaginatedList } from '@/composables/use-paginated-list.js'
import { previewRechargePoints } from '@/utils/recharge-gift.js'
import { createRequestId } from '@/utils/request-id.js'
import SegmentedTabs from '@/components/common/segmented-tabs.vue'

const shopId = ref('')
const shopInfo = ref(null)
const shopRatio = computed(() => shopInfo.value?.ratio ?? null)
const activeTab = ref('account')
const pageSize = 20
const searchKeyword = ref('')
const accountSortType = ref(1)

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
	pageSize,
	fetchPage: (pageNum, size) => {
		const params = {
			shopId: Number(shopId.value),
			pageNum,
			pageSize: size
		}
		const keyword = searchKeyword.value.trim()
		if (keyword) params.keyword = keyword
		if (activeTab.value === 'account') params.sortType = accountSortType.value
		return activeTab.value === 'log'
			? fetchShopConsumeLogList(params)
			: fetchShopPointsAccountList(params)
	}
})

const { run: debouncedReloadList } = useDebounceFn(() => {
	void reloadList()
}, 350)

const tabs = [
	{ key: 'account', label: '会员积分' },
	{ key: 'log', label: '消费记录' }
]

const ongoingActivities = ref([])
const rechargeVisible = ref(false)
const rechargeTarget = ref(null)
const rechargeAmount = ref('')
const rechargeSubmitting = ref(false)
const rechargePreview = reactive({
	basePoints: 0,
	giftPoints: 0,
	totalPoints: 0
})
let rechargeRequestId = ''

const accountSortOptions = [
	{ value: 1, label: '剩余↓' },
	{ value: 2, label: '剩余↑' },
	{ value: 3, label: '已用↓' },
	{ value: 4, label: '已用↑' }
]

const accountList = computed(() =>
	rows.value.map((row) => ({
		id: `${row.userId}-${row.shopId}`,
		nickname: row.nickname || '会员',
		phone: row.phone ? String(row.phone) : '',
		remainingText: formatPointsAmount(row.remainingPoints),
		usedText: formatPointsAmount(row.totalUsedPoints),
		baseText: formatPointsAmount(row.basePoints ?? 0),
		bonusText: formatPointsAmount(row.bonusPoints ?? 0),
		showBreakdown: row.basePoints != null || row.bonusPoints != null,
		source: row
	}))
)

const searchPlaceholder = computed(() =>
	activeTab.value === 'log' ? '搜索会员昵称或手机号' : '搜索会员昵称或手机号'
)

const emptyText = computed(() => {
	const trimmed = searchKeyword.value.trim()
	if (trimmed) return '未找到匹配的会员'
	return '暂无数据'
})

onLoad((options) => {
	if (!canManageShop()) {
		uni.showToast({ title: '无店铺管理权限', icon: 'none' })
		navigateBackDelayed(800)
		return
	}
	shopId.value = options?.shopId ? String(options.shopId) : ''
	if (!shopId.value) {
		uni.showToast({ title: '缺少店铺 ID', icon: 'none' })
		return
	}
	bindOpenerShop((data) => {
		if (!isShopClerkCapable(data)) {
			uni.showToast({ title: '无店铺管理权限', icon: 'none' })
			navigateBackDelayed(800)
			return
		}
		shopInfo.value = data
	})
	void loadShopInfo()
	void loadOngoingActivities()
	void reloadList()
})

async function loadShopInfo() {
	if (shopInfo.value || !shopId.value) return
	const res = await fetchShopFromUserList(shopId.value)
	if (res.ok && res.data) {
		shopInfo.value = rememberShopDetail(res.data) || res.data
	}
}

async function loadOngoingActivities() {
	if (!shopId.value) return
	const res = await fetchShopOngoingActivities(Number(shopId.value))
	if (res.ok) {
		ongoingActivities.value = res.rows || []
	}
}

function updateRechargePreview() {
	const preview = previewRechargePoints(rechargeAmount.value, shopRatio.value, ongoingActivities.value)
	rechargePreview.basePoints = preview.basePoints
	rechargePreview.giftPoints = preview.giftPoints
	rechargePreview.totalPoints = preview.totalPoints
}

function onRechargeAmountInput() {
	updateRechargePreview()
}

function openRecharge(item) {
	rechargeTarget.value = item
	rechargeAmount.value = ''
	rechargeRequestId = ''
	updateRechargePreview()
	rechargeVisible.value = true
}

function closeRecharge() {
	if (rechargeSubmitting.value) return
	rechargeVisible.value = false
	rechargeTarget.value = null
	rechargeAmount.value = ''
	rechargeRequestId = ''
}

async function submitRecharge() {
	if (rechargeSubmitting.value || !rechargeTarget.value?.userId) return
	updateRechargePreview()
	if (rechargePreview.basePoints <= 0) {
		uni.showToast({ title: '请输入有效充值金额', icon: 'none' })
		return
	}
	const amountYuan = Number(rechargeAmount.value)
	if (Number.isNaN(amountYuan) || amountYuan <= 0) {
		uni.showToast({ title: '请输入有效充值金额', icon: 'none' })
		return
	}
	if (!rechargeRequestId) {
		rechargeRequestId = createRequestId()
	}
	rechargeSubmitting.value = true
	const res = await rechargeMemberPoints({
		shopId: Number(shopId.value),
		userId: rechargeTarget.value.userId,
		amountYuan,
		requestId: rechargeRequestId
	})
	rechargeSubmitting.value = false
	if (!res.ok) {
		uni.showToast({ title: res.msg || '充值失败', icon: 'none' })
		return
	}
	rechargeRequestId = ''
	const total = res.data?.totalPoints ?? rechargePreview.totalPoints
	uni.showToast({
		title: `充值成功，+${formatPointsAmount(total)} 积分`,
		icon: 'success'
	})
	closeRecharge()
	void reloadList()
}

function switchTab(key) {
	if (activeTab.value === key) return
	activeTab.value = key
	void reloadList()
}

function switchAccountSort(value) {
	if (accountSortType.value === value) return
	accountSortType.value = value
	void reloadList()
}

function onSearchConfirm() {
	void reloadList()
}

function onSearchInput() {
	debouncedReloadList()
}

function clearSearch() {
	searchKeyword.value = ''
	void reloadList()
}

function logActionLabel(item) {
	if (item.status === POINTS_LOG_STATUS.RECHARGE) return '积分充值'
	if (item.actionType === 1) return '积分增加'
	if (item.actionType === 2) return '积分消耗'
	return '—'
}

function logStatusOrActionLabel(item) {
	const label = pointsLogStatusLabel(item.status)
	return label !== '—' ? label : logActionLabel(item)
}

function formatLogPoints(item) {
	const pts = item.consumePoints
	if (pts == null) return '—'
	const prefix = isPointsLogGain(item) ? '+' : '-'
	return `${prefix}${formatPointsAmount(pts)} 积分`
}

function logPointsClass(item) {
	if (isPointsLogGain(item)) return 'card-points--gain'
	return ''
}

function openLogDetail(item) {
	if (!item?.logId) return
	uni.navigateTo({
		url: `/pages/member/order-detail?shopId=${shopId.value}&logId=${item.logId}&from=shop`,
		animationType: 'slide-in-right',
		animationDuration: 200,
		success(res) {
			res.eventChannel?.emit('order', item)
		}
	})
}
</script>

<style scoped>
.records-tabs {
	margin: 12px 0 0;
	flex-shrink: 0;
}

.page {
	min-height: 100vh;
	background: #f5f5f5;
	display: flex;
	flex-direction: column;
	padding: 0 16px;
	box-sizing: border-box;
}

.tab-bar {
	display: flex;
	position: relative;
	margin: 12px 0 0;
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

.toolbar {
	margin: 10px 0 0;
	flex-shrink: 0;
}

.search-bar {
	display: flex;
	align-items: center;
	gap: 8px;
	background: #fff;
	border-radius: 10px;
	padding: 0 12px;
	height: 40px;
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.search-icon {
	flex-shrink: 0;
}

.search-input {
	flex: 1;
	min-width: 0;
	font-size: 14px;
	color: #333;
	height: 40px;
}

.search-placeholder {
	color: #bbb;
	font-size: 14px;
}

.search-clear {
	width: 22px;
	height: 22px;
	border-radius: 11px;
	background: #e8e8e8;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.search-clear-text {
	font-size: 16px;
	color: #888;
	line-height: 1;
}

.sort-bar {
	margin-top: 8px;
}

.sort-track {
	display: flex;
	background: #fff;
	border-radius: 6px;
	overflow: hidden;
	border: 1px solid #e8e8e8;
}

.sort-item {
	flex: 1;
	min-width: 0;
	padding: 7px 0;
	text-align: center;
	border-right: 1px solid #ececec;
}

.sort-item:last-child {
	border-right: none;
}

.sort-item--active {
	background: #f5f9ff;
}

.sort-item-text {
	font-size: 12px;
	color: #888;
	line-height: 1.2;
}

.sort-item--active .sort-item-text {
	color: #007aff;
	font-weight: 600;
}

.scroll {
	flex: 1;
	height: 0;
	min-width: 0;
	margin: 10px 0 0;
	background: #fff;
	border-radius: 10px;
	padding-bottom: 16px;
	box-sizing: border-box;
}

.scroll--cards {
	background: transparent;
	border-radius: 0;
	padding-bottom: 8px;
}

.scroll-inner {
	padding: 0 14px;
	box-sizing: border-box;
}

.scroll--cards .scroll-inner {
	padding: 0;
}

.list-card {
	width: 100%;
	box-sizing: border-box;
	overflow: hidden;
	border-top: 1px solid #f0f0f0;
	padding: 12px 0;
}

.list-card--hover {
	opacity: 0.72;
}

.list-card:first-of-type {
	border-top: none;
	padding-top: 8px;
}

.list-card--account {
	border-top: none;
	margin-bottom: 8px;
	padding: 12px 14px;
	background: #fff;
	border-radius: 10px;
	border: 1px solid #e6e6e6;
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.list-card--account:last-of-type {
	margin-bottom: 0;
}

.scroll--cards .state-wrap {
	background: #fff;
	border-radius: 10px;
	border: 1px solid #e6e6e6;
}

.account-head {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 10px;
	margin-bottom: 4px;
}

.account-info {
	flex: 1;
	min-width: 0;
}

.account-name {
	display: block;
	font-size: 15px;
	font-weight: 500;
	color: #333;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.account-phone {
	display: block;
	margin-top: 2px;
	font-size: 12px;
	color: #999;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.account-recharge {
	flex-shrink: 0;
	font-size: 12px;
	color: #007aff;
	padding: 2px 0;
	line-height: 1.4;
}

.account-points {
	display: flex;
	flex-wrap: wrap;
	align-items: baseline;
	gap: 4px 6px;
	line-height: 1.35;
}

.account-stat {
	font-size: 12px;
	color: #888;
}

.account-num {
	font-size: 14px;
	font-weight: 600;
	color: #333;
}

.account-num--used {
	color: #e64340;
}

.account-sep {
	font-size: 12px;
	color: #d0d0d0;
}

.account-breakdown {
	font-size: 11px;
	color: #aaa;
}

.card-head,
.card-main,
.card-sub {
	display: flex;
	justify-content: space-between;
	align-items: center;
	gap: 8px;
	width: 100%;
	min-width: 0;
	box-sizing: border-box;
}

.card-head {
	margin-bottom: 6px;
}

.recharge-mask {
	position: fixed;
	inset: 0;
	z-index: 999;
	background: rgba(15, 23, 42, 0.52);
	display: flex;
	align-items: flex-end;
	justify-content: center;
}

.recharge-sheet {
	width: 100%;
	background: #fff;
	border-radius: 20px 20px 0 0;
	padding: 20px 16px calc(16px + env(safe-area-inset-bottom));
	box-sizing: border-box;
}

.recharge-title {
	display: block;
	font-size: 18px;
	font-weight: 700;
	color: #1a1a1a;
	text-align: center;
}

.recharge-sub {
	display: block;
	margin-top: 4px;
	font-size: 13px;
	color: #888;
	text-align: center;
}

.recharge-ratio {
	margin-top: 12px;
	text-align: center;
}

.recharge-ratio-text {
	font-size: 12px;
	color: #007aff;
}

.recharge-field {
	margin-top: 16px;
}

.recharge-label {
	display: block;
	font-size: 14px;
	font-weight: 600;
	color: #333;
	margin-bottom: 8px;
}

.recharge-input {
	width: 100%;
	height: 44px;
	padding: 0 14px;
	box-sizing: border-box;
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
	font-size: 16px;
	color: #1a1a1a;
}

.recharge-placeholder {
	color: #b8c0cc;
	font-size: 15px;
}

.recharge-preview {
	margin-top: 14px;
	padding: 12px 14px;
	background: #f7f8fa;
	border-radius: 10px;
}

.preview-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 6px;
}

.preview-row:last-child {
	margin-bottom: 0;
}

.preview-row--total {
	margin-top: 4px;
	padding-top: 8px;
	border-top: 1px solid #e8e8e8;
}

.preview-label {
	font-size: 13px;
	color: #666;
}

.preview-value {
	font-size: 14px;
	font-weight: 600;
	color: #333;
}

.preview-value--gift {
	color: #34c759;
}

.preview-value--total {
	font-size: 16px;
	color: #e64340;
}

.recharge-footer {
	display: flex;
	gap: 12px;
	margin-top: 18px;
}

.recharge-footer-btn {
	flex: 1;
	height: 44px;
	line-height: 44px;
	text-align: center;
	border-radius: 12px;
	font-size: 15px;
	font-weight: 600;
	box-sizing: border-box;
}

.recharge-footer-btn--ghost {
	background: #f5f5f5;
	color: #666;
}

.recharge-footer-btn--primary {
	background: linear-gradient(135deg, #6eb5ff 0%, #007aff 100%);
	color: #fff;
}

.recharge-footer-btn--disabled {
	opacity: 0.55;
	pointer-events: none;
}

.card-main {
	margin-bottom: 4px;
}

.card-name {
	font-size: 15px;
	font-weight: 500;
	color: #333;
	flex: 1;
	min-width: 0;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.card-status {
	font-size: 12px;
	color: #007aff;
	flex-shrink: 0;
	max-width: 36%;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	text-align: right;
}

.card-date {
	font-size: 12px;
	color: #888;
	flex: 1;
	min-width: 0;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.card-points {
	font-size: 13px;
	color: #e64340;
	font-weight: 600;
	flex-shrink: 1;
	min-width: 0;
	max-width: 46%;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	text-align: right;
}

.card-points--gain {
	color: #34c759;
}

.card-meta {
	font-size: 12px;
	color: #888;
	flex: 1;
	min-width: 0;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	text-align: right;
}

.card-meta--left {
	flex: 1;
	min-width: 0;
	max-width: 58%;
	text-align: left;
}

.card-meta--right {
	flex-shrink: 1;
	max-width: 42%;
}

.card-hint {
	display: block;
	width: 100%;
	box-sizing: border-box;
	margin-top: 6px;
	font-size: 12px;
	color: #aaa;
	text-align: right;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.state-wrap {
	padding: 40px 0;
	text-align: center;
}

.state-wrap--inline {
	padding: 16px 0;
}

.state-text {
	font-size: 14px;
	color: #999;
}
</style>
