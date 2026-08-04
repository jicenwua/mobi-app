<template>
	<view
		class="detail-root detail-root--page"
		:class="isDark ? 'detail-root--dark' : 'detail-root--light'"
	>
		<view v-if="loading" class="state-wrap">
			<text class="state-text">加载中…</text>
		</view>
		<template v-else-if="shop">
			<ShopImageSwiper :images="carouselImages" :is-dark="isDark" height="200px" />

			<view class="shop-title-bar">
				<text class="shop-title">{{ shop.shopName || '未命名店铺' }}</text>
				<view class="shop-title-actions">
					<text
						v-if="canShowQrcode"
						class="shop-action-link"
						hover-class="tap-hover-opacity"
						:hover-stay-time="70"
						@click="openShopQrcode"
					>店铺码</text>
					<text
						v-if="canCheckout"
						class="shop-action-link"
						hover-class="tap-hover-opacity"
						:hover-stay-time="70"
						@click="goScanCheckout"
					>扫码核销</text>
					<text
						v-if="showManageEntry"
						class="shop-action-link"
						hover-class="tap-hover-opacity"
						:hover-stay-time="70"
						@click="goManage"
					>管理</text>
				</view>
			</view>

			<view class="tab-bar">
				<view
					class="tab-item"
					:class="{ active: activeTab === 'points' }"
					@click="activeTab = 'points'"
				>
					<text class="tab-text">积分</text>
				</view>
				<view
					class="tab-item"
					:class="{ active: activeTab === 'log' }"
					@click="activeTab = 'log'"
				>
					<text class="tab-text">记录</text>
				</view>
				<view class="tab-slider" :class="activeTab === 'log' ? 'tab-slider--right' : ''" />
			</view>

			<view class="filter-bar">
				<input
					v-model="keywordNickname"
					class="filter-input"
					type="text"
					placeholder="昵称"
					confirm-type="search"
					@confirm="reloadList"
				/>
				<input
					v-model="keywordPhone"
					class="filter-input"
					type="text"
					placeholder="手机号"
					confirm-type="search"
					@confirm="reloadList"
				/>
				<view class="filter-btn" @click="reloadList">
					<text class="filter-btn-text">搜索</text>
				</view>
			</view>

			<view v-if="activeTab === 'points'" class="sort-row">
				<view
					class="sort-chip"
					:class="{ 'sort-chip--on': sortType === 1 }"
					@click="setSort(1)"
				>
					<text class="sort-chip-text">余额↓</text>
				</view>
				<view
					class="sort-chip"
					:class="{ 'sort-chip--on': sortType === 2 }"
					@click="setSort(2)"
				>
					<text class="sort-chip-text">余额↑</text>
				</view>
				<view
					class="sort-chip"
					:class="{ 'sort-chip--on': sortType === 3 }"
					@click="setSort(3)"
				>
					<text class="sort-chip-text">累计↓</text>
				</view>
				<view
					class="sort-chip"
					:class="{ 'sort-chip--on': sortType === 4 }"
					@click="setSort(4)"
				>
					<text class="sort-chip-text">累计↑</text>
				</view>
			</view>

			<scroll-view class="list-scroll" scroll-y @scrolltolower="loadMore">
				<view v-if="listLoading && !listRows.length" class="state-wrap">
					<text class="state-text">加载列表…</text>
				</view>
				<view v-else-if="!listRows.length" class="state-wrap">
					<text class="state-text">暂无数据</text>
				</view>
				<template v-else>
					<view v-if="activeTab === 'points'" v-for="row in listRows" :key="row.userId" class="list-card">
						<view class="row-main">
							<text class="row-name">{{ row.nickname || '—' }}</text>
							<text class="row-points">剩余 {{ row.remainingPoints ?? 0 }} 积分</text>
						</view>
						<view class="row-meta">
							<text class="meta-item">基础 {{ row.basePoints ?? 0 }}</text>
							<text class="meta-item">赠送 {{ row.bonusPoints ?? 0 }}</text>
							<text class="meta-item">累计已用 {{ row.totalUsedPoints ?? 0 }}</text>
						</view>
					</view>
					<view v-else v-for="row in listRows" :key="row.logId" class="list-card">
						<view class="row-main">
							<text class="row-date">{{ formatDate(row.consumeTime) }}</text>
							<text class="row-points">-{{ row.consumePoints ?? 0 }} 积分</text>
						</view>
						<view class="row-sub">
							<text class="row-name">{{ row.nickname || '—' }}</text>
							<text class="detail-btn" @click="openLogDetail(row)">详情</text>
						</view>
					</view>
					<view v-if="listLoadingMore" class="list-footer">
						<text class="list-footer-text">加载更多…</text>
					</view>
					<view v-else-if="listFinished" class="list-footer">
						<text class="list-footer-text">没有更多了</text>
					</view>
				</template>
			</scroll-view>
		</template>
		<view v-else class="state-wrap">
			<text class="state-text">{{ errorMsg || '店铺不存在' }}</text>
		</view>

		<view v-if="qrcodeVisible" class="modal-mask" @click="closeShopQrcode">
			<view class="modal-panel qrcode-panel" :class="isDark ? 'modal-panel--dark' : ''" @click.stop>
				<text class="modal-title">店铺邀请码</text>
				<text class="qrcode-hint">邀请码 2 分钟内有效，请及时扫码加入</text>
				<view v-if="qrcodeLoading" class="state-wrap">
					<text class="state-text">生成中…</text>
				</view>
				<view v-else-if="shopQrcodeImage || shopQrcodeFallback" class="qrcode-image-wrap">
					<image
						v-if="shopQrcodeImage"
						class="qrcode-image"
						:src="shopQrcodeImage"
						mode="aspectFit"
					/>
					<QrcodeCanvas
						v-else
						:text="shopQrcodeFallback"
						:size="200"
						canvas-id="shop-qrcode-fallback"
					/>
					<text v-if="qrcodeExpireHint" class="qrcode-expire">{{ qrcodeExpireHint }}</text>
				</view>
				<view class="modal-actions">
					<view class="modal-close" @click="closeShopQrcode">
						<text>关闭</text>
					</view>
				</view>
			</view>
		</view>

		<view v-if="detailVisible" class="modal-mask" @click="detailVisible = false">
			<view class="modal-panel" :class="isDark ? 'modal-panel--dark' : ''" @click.stop>
				<text class="modal-title">消费详情</text>
				<view v-if="logDetail" class="modal-body">
					<view class="detail-row">
						<text class="detail-label">消费时间</text>
						<text class="detail-value">{{ formatDateTime(logDetail.createTime) }}</text>
					</view>
					<view class="detail-row">
						<text class="detail-label">用户</text>
						<text class="detail-value">{{ logDetail.nickname }} {{ logDetail.phone || '' }}</text>
					</view>
					<view class="detail-row">
						<text class="detail-label">类型</text>
						<text class="detail-value">{{ actionTypeLabel(logDetail.actionType) }}</text>
					</view>
					<view class="detail-row">
						<text class="detail-label">状态</text>
						<text class="detail-value">{{ statusLabel(logDetail.status) }}</text>
					</view>
					<view class="detail-row">
						<text class="detail-label">本次消费</text>
						<text class="detail-value">-{{ logDetail.consumePoints ?? 0 }} 积分</text>
					</view>
					<view class="detail-row">
						<text class="detail-label">消费后剩余</text>
						<text class="detail-value">{{ logDetail.remainingPoints ?? 0 }} 积分</text>
					</view>
					<view v-if="logDetail.couponDiscountPoints" class="detail-row">
						<text class="detail-label">优惠券抵扣</text>
						<text class="detail-value">-{{ logDetail.couponDiscountPoints }} 积分（{{ logDetail.couponName || '优惠券' }}）</text>
					</view>
					<view v-if="logDetail.items && logDetail.items.length" class="detail-items">
						<text class="detail-label">购买商品</text>
						<view v-for="item in logDetail.items" :key="item.receiptId" class="detail-item-row">
							<text class="detail-item-name">{{ item.productName }}</text>
							<text class="detail-item-meta">×{{ item.count }} {{ item.linePoints ?? 0 }}积分</text>
						</view>
					</view>
				</view>
				<view class="modal-actions">
					<view class="modal-close" @click="detailVisible = false">
						<text>关闭</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useExpiringQrcode } from '@/composables/use-expiring-qrcode.js'
import {
	fetchShopPointsAccountList,
	fetchShopConsumeLogList,
	fetchConsumeLogDetail
} from '@/api/modules/points.js'
import { getShopCarouselImages, fetchShopFromUserList, normalizeShopDetail } from '@/api/modules/shop.js'
import { fetchShopQrcode } from '@/api/modules/qrcode.js'
import ShopImageSwiper from '@/components/shop/shop-image-swiper.vue'
import { formatDateTime } from '@/utils/datetime-format.js'
import { pointsLogStatusLabel } from '@/utils/points-log-status.js'
import QrcodeCanvas from '@/components/common/qrcode-canvas.vue'
import { isShopClerkCapable } from '@/utils/shop-role.js'
import {
	canShowShopTab,
	canPreviewShopByCode,
	canManageShop as checkManageShop,
	canStaffVerifyAtShop
} from '@/utils/wx-perm.js'
import { scanStaffVerifyCode } from '@/utils/staff-scan.js'
import { rememberShopDetail, navigateWithShop } from '@/utils/shop-page-context.js'
import { getCachedShopDetail } from '@/utils/shop-detail-cache.js'

const props = defineProps({
	shopId: { type: [String, Number], default: '' },
	/** 列表项传入的店铺信息（从卡片跳转） */
	initialShop: { type: Object, default: null },
	isDark: { type: Boolean, default: false }
})

const shop = ref(null)
const loading = ref(true)
const errorMsg = ref('')

const activeTab = ref('points')
const sortType = ref(1)
const keywordNickname = ref('')
const keywordPhone = ref('')

const listRows = ref([])
const listLoading = ref(false)
const listLoadingMore = ref(false)
const listFinished = ref(false)
const pageNum = ref(1)
const pageSize = 10

const detailVisible = ref(false)
const logDetail = ref(null)

const qrcodeVisible = ref(false)
const qrcodeLoading = ref(false)
const shopQrcodeImage = ref('')
const shopQrcodeFallback = ref('')
const shopQrcodeExpireAt = ref(0)

let shopLoadToken = 0
const pointsListLoaded = ref(false)
const logsListLoaded = ref(false)

const resolvedShopId = computed(() => {
	const fromProp = props.shopId != null && props.shopId !== '' ? String(props.shopId) : ''
	if (fromProp) return fromProp
	const fromShop = shop.value?.id
	return fromShop != null && fromShop !== '' ? String(fromShop) : ''
})

const qrcodeExpireHint = computed(() => {
	if (!qrcodeCountdownLeft.value) return ''
	return `${qrcodeCountdownLeft.value} 秒后自动刷新`
})

async function refreshShopQrcode() {
	const id = resolvedShopId.value
	if (!id || !qrcodeVisible.value) return
	qrcodeLoading.value = true
	shopQrcodeImage.value = ''
	shopQrcodeFallback.value = ''
	stopQrcodeCountdown()
	qrcodeCountdownLeft.value = 0
	const res = await fetchShopQrcode(id)
	qrcodeLoading.value = false
	if (!res.ok) {
		uni.showToast({ title: res.msg || '生成失败', icon: 'none' })
		return
	}
	if (res.data?.imageBase64) {
		shopQrcodeImage.value = res.data.imageBase64
	}
	if (res.data?.inviteContent) {
		shopQrcodeFallback.value = res.data.inviteContent
	}
	shopQrcodeExpireAt.value = res.data?.expireAt || Date.now() + 120000
	syncQrcodeCountdownFromExpireAt(shopQrcodeExpireAt.value)
}

const {
	countdownLeft: qrcodeCountdownLeft,
	stopCountdown: stopQrcodeCountdown,
	syncFromExpireAt: syncQrcodeCountdownFromExpireAt
} = useExpiringQrcode(() => {
	if (qrcodeVisible.value) {
		void refreshShopQrcode()
	}
})

const carouselImages = computed(() => getShopCarouselImages(shop.value))
const showManageEntry = computed(
	() => isShopClerkCapable(shop.value) && checkManageShop()
)
const canShowQrcode = computed(() => canPreviewShopByCode())
const canCheckout = computed(() => canStaffVerifyAtShop(shop.value))

function goManage() {
	const id = resolvedShopId.value
	if (!id) return
	navigateWithShop({
		url: `/pages/shop/manage?id=${id}`,
		shop: shop.value
	})
}

function goScanCheckout() {
	const id = resolvedShopId.value
	if (!id) return
	scanStaffVerifyCode({ shopId: id })
}

async function openShopQrcode() {
	const id = resolvedShopId.value
	if (!id) return
	qrcodeVisible.value = true
	await refreshShopQrcode()
}

function closeShopQrcode() {
	qrcodeVisible.value = false
	stopQrcodeCountdown()
	qrcodeCountdownLeft.value = 0
}

watch(
	() => [props.shopId, props.initialShop?.id],
	() => {
		syncShopFromProps()
	},
	{ immediate: true }
)

watch(activeTab, (tab) => {
	if (!shop.value) return
	if (tab === 'points' && !pointsListLoaded.value && !listLoading.value) {
		reloadList()
	}
	if (tab === 'log' && !logsListLoaded.value && !listLoading.value) {
		reloadList()
	}
})

function syncShopFromProps() {
	if (!canShowShopTab()) {
		errorMsg.value = '无店铺访问权限'
		loading.value = false
		shop.value = null
		return
	}
	const data = props.initialShop
	if (data?.id) {
		applyShop(data)
		return
	}
	const id = props.shopId != null && props.shopId !== '' ? String(props.shopId) : ''
	if (!id) {
		loading.value = false
		errorMsg.value = '店铺信息无效'
		shop.value = null
		return
	}
	const cached = getCachedShopDetail(id)
	if (cached) {
		applyShop(cached)
		return
	}
	loading.value = true
	errorMsg.value = ''
	shop.value = null
	loadShopFallback()
}

async function loadShopFallback() {
	const token = ++shopLoadToken
	const id = props.shopId != null && props.shopId !== '' ? String(props.shopId) : ''
	if (!id) return

	loading.value = true
	errorMsg.value = ''
	const res = await fetchShopFromUserList(id)
	if (token !== shopLoadToken) return

	if (!res.ok || !res.data) {
		loading.value = false
		errorMsg.value = res.msg || '加载失败'
		shop.value = null
		return
	}
	applyShop(res.data)
}

/** 由独立详情页 eventChannel 或外部调用 */
function applyShop(data) {
	shopLoadToken += 1
	const normalized = normalizeShopDetail(data)
	if (!normalized?.id) {
		errorMsg.value = '店铺信息无效'
		loading.value = false
		shop.value = null
		return
	}
	activeTab.value = 'points'
	keywordNickname.value = ''
	keywordPhone.value = ''
	sortType.value = 1
	shop.value = normalized
	rememberShopDetail(normalized)
	loading.value = false
	errorMsg.value = ''
	reloadList()
}

function setSort(type) {
	if (sortType.value === type) return
	sortType.value = type
	reloadList()
}

function reloadList() {
	pageNum.value = 1
	listRows.value = []
	listFinished.value = false
	if (activeTab.value === 'points') {
		pointsListLoaded.value = false
	} else {
		logsListLoaded.value = false
	}
	loadList(false)
}

async function loadList(more) {
	const id = resolvedShopId.value
	if (!id || listLoading.value || listLoadingMore.value) return
	if (more && listFinished.value) return

	if (more) listLoadingMore.value = true
	else listLoading.value = true

	const params = {
		shopId: Number(id),
		pageNum: pageNum.value,
		pageSize,
		nickname: keywordNickname.value.trim() || undefined,
		phone: keywordPhone.value.trim() || undefined
	}
	if (activeTab.value === 'points') {
		params.sortType = sortType.value
	}

	const fetcher = activeTab.value === 'points' ? fetchShopPointsAccountList : fetchShopConsumeLogList
	const res = await fetcher(params)

	if (more) listLoadingMore.value = false
	else listLoading.value = false

	if (!res.ok) {
		uni.showToast({ title: res.msg || '加载失败', icon: 'none' })
		return
	}

	const rows = res.rows || []
	if (more) {
		listRows.value = listRows.value.concat(rows)
	} else {
		listRows.value = rows
		if (activeTab.value === 'points') {
			pointsListLoaded.value = true
		} else {
			logsListLoaded.value = true
		}
	}
	listFinished.value = rows.length < pageSize
}

function loadMore() {
	if (listFinished.value || listLoading.value || listLoadingMore.value) return
	pageNum.value += 1
	loadList(true)
}

function actionTypeLabel(type) {
	if (type === 1) return '增加'
	if (type === 2) return '消耗'
	return '—'
}

function statusLabel(status) {
	return pointsLogStatusLabel(status)
}

function formatDate(iso) {
	if (!iso) return '—'
	const s = String(iso).replace('T', ' ')
	return s.length >= 10 ? s.slice(0, 10) : s
}

async function openLogDetail(row) {
	const id = resolvedShopId.value
	if (!row?.logId || !id) return
	detailVisible.value = true
	logDetail.value = row
	const res = await fetchConsumeLogDetail(row.logId, Number(id))
	if (res.ok && res.data) {
		logDetail.value = res.data
	} else if (!res.ok) {
		uni.showToast({ title: res.msg || '加载详情失败', icon: 'none' })
	}
}

defineExpose({ applyShop })
</script>

<style scoped>
.detail-root {
	display: flex;
	flex-direction: column;
	box-sizing: border-box;
}

.detail-root--page {
	min-height: 100vh;
}

.detail-root--light {
	background-color: #f5f5f5;
	color: #333;
}

.detail-root--dark {
	background-color: #121212;
	color: #e8e8e8;
}

.shop-title-bar {
	padding: 14px 16px;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
}

.detail-root--light .shop-title-bar {
	background: #fff;
}

.detail-root--dark .shop-title-bar {
	background: #1e1e1e;
}

.shop-title {
	flex: 1;
	min-width: 0;
	font-size: 18px;
	font-weight: 600;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.shop-manage-link {
	flex-shrink: 0;
	font-size: 13px;
	color: #007aff;
}

.shop-title-actions {
	display: flex;
	flex-shrink: 0;
	align-items: center;
	gap: 10px;
}

.shop-action-link {
	font-size: 13px;
	color: #007aff;
}

.qrcode-hint {
	display: block;
	font-size: 12px;
	margin-bottom: 12px;
	text-align: center;
}

.detail-root--light .qrcode-hint {
	color: #666666;
}

.detail-root--dark .qrcode-hint {
	color: #a0a0a0;
}

.qrcode-image-wrap,
.fallback-qr {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 10px;
}

.qrcode-image {
	width: 220px;
	height: 220px;
}

.qrcode-code,
.qrcode-expire {
	font-size: 12px;
	opacity: 0.6;
}

.tab-bar {
	display: flex;
	position: relative;
	margin: 12px 16px 0;
	border-radius: 8px;
	padding: 3px;
}

.detail-root--light .tab-bar {
	background: #e8e8e8;
}

.detail-root--dark .tab-bar {
	background: #2a2a2a;
}

.tab-item {
	flex: 1;
	z-index: 1;
	padding: 8px 0;
	text-align: center;
}

.tab-text {
	font-size: 14px;
	opacity: 0.65;
}

.tab-item.active .tab-text {
	color: #fff;
	font-weight: 600;
	opacity: 1;
}

.tab-slider {
	position: absolute;
	top: 3px;
	left: 3px;
	width: calc(50% - 3px);
	height: calc(100% - 6px);
	background: #007aff;
	border-radius: 6px;
	transition: transform 0.25s ease;
}

.tab-slider--right {
	transform: translateX(100%);
}

.filter-bar {
	display: flex;
	gap: 8px;
	padding: 12px 16px;
	align-items: center;
}

.filter-input {
	flex: 1;
	height: 34px;
	padding: 0 10px;
	border-radius: 6px;
	font-size: 13px;
}

.detail-root--light .filter-input {
	background: #fff;
	color: #333;
}

.detail-root--dark .filter-input {
	background: #1e1e1e;
	color: #e8e8e8;
}

.filter-btn {
	padding: 0 12px;
	height: 34px;
	line-height: 34px;
	background: #007aff;
	border-radius: 6px;
}

.filter-btn-text {
	color: #fff;
	font-size: 13px;
}

.sort-row {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
	padding: 0 16px 8px;
}

.sort-chip {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	padding: 6px 12px;
	border-radius: 14px;
	box-sizing: border-box;
}

.sort-chip-text {
	font-size: 12px;
	line-height: 1.2;
}

.detail-root--light .sort-chip:not(.sort-chip--on) {
	background: #fff;
	color: #666;
}

.detail-root--dark .sort-chip:not(.sort-chip--on) {
	background: #1e1e1e;
	color: #b0b0b0;
}

.detail-root--light .sort-chip--on,
.detail-root--dark .sort-chip--on {
	background: #007aff;
}

.detail-root--light .sort-chip--on .sort-chip-text,
.detail-root--dark .sort-chip--on .sort-chip-text {
	color: #fff;
}

.list-scroll {
	flex: 1;
	height: 0;
	padding: 0 16px 24px;
	box-sizing: border-box;
}

.list-card {
	border-radius: 10px;
	padding: 12px 14px;
	margin-bottom: 10px;
}

.detail-root--light .list-card {
	background: #fff;
}

.detail-root--dark .list-card {
	background: #1e1e1e;
}

.row-main {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 6px;
}

.row-name {
	font-size: 15px;
	font-weight: 500;
}

.row-date {
	font-size: 13px;
	opacity: 0.65;
}

.row-points {
	font-size: 14px;
	color: #e64340;
	font-weight: 600;
}

.row-meta,
.row-sub {
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.meta-item {
	font-size: 12px;
	opacity: 0.55;
}

.detail-btn {
	font-size: 13px;
	color: #007aff;
}

.state-wrap {
	padding: 40px 16px;
	text-align: center;
}

.state-text {
	font-size: 14px;
	opacity: 0.5;
}

.list-footer {
	padding: 12px;
	text-align: center;
}

.list-footer-text {
	font-size: 12px;
	opacity: 0.45;
}

.modal-mask {
	position: fixed;
	inset: 0;
	background: rgba(0, 0, 0, 0.45);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 100;
	padding: 24px;
	box-sizing: border-box;
}

.modal-panel {
	width: 100%;
	max-width: 340px;
	background: #fff;
	border-radius: 12px;
	padding: 16px;
}

.modal-panel--dark {
	background: #1e1e1e;
	color: #e8e8e8;
}

.modal-title {
	font-size: 17px;
	font-weight: 600;
	margin-bottom: 12px;
	display: block;
}

.detail-row {
	display: flex;
	justify-content: space-between;
	padding: 8px 0;
	border-bottom: 1px solid rgba(128, 128, 128, 0.2);
	font-size: 13px;
}

.detail-label {
	opacity: 0.55;
}

.detail-value {
	max-width: 60%;
	text-align: right;
}

.detail-items {
	padding: 8px 0;
	border-bottom: 1px solid rgba(128, 128, 128, 0.2);
}

.detail-item-row {
	display: flex;
	justify-content: space-between;
	padding: 4px 0 4px 12px;
	font-size: 13px;
}

.detail-item-name {
	flex: 1;
}

.detail-item-meta {
	opacity: 0.55;
	margin-left: 8px;
}

.modal-actions {
	margin-top: 16px;
}

.modal-close {
	text-align: center;
	padding: 10px;
	border-radius: 8px;
	font-size: 14px;
}

.detail-root--light .modal-close {
	background: #f5f5f5;
}

.detail-root--dark .modal-close {
	background: #2a2a2a;
}
</style>
