<template>
	<view
		class="shop-card"
		:class="isDark ? 'shop-card--dark' : 'shop-card--light'"
		:hover-class="showDetailLink ? 'tap-hover-opacity' : ''"
		:hover-stay-time="70"
		@click="goDetail"
	>
		<view class="shop-header">
			<view class="shop-header-left">
				<view class="shop-title-group">
					<text class="shop-name">{{ displayShopName }}</text>
					<text
						v-if="roleLabel"
						class="shop-role-tag"
						:class="roleTagClass"
					>{{ roleLabel }}</text>
				</view>
			</view>
			<view class="shop-header-actions" @click.stop>
				<view
					v-if="showScanVerifyBtn"
					class="shop-qrcode-btn"
					hover-class="tap-hover-opacity"
					:hover-stay-time="70"
					@click.stop="scanForVerify"
				>
					<uni-icons type="scan" :size="17" :color="actionIconColor" />
				</view>
				<text
					v-if="showInviteQrcodeBtn"
					class="shop-invite-link"
					hover-class="tap-hover-opacity"
					:hover-stay-time="70"
					@click.stop="openShopQrcode"
				>店铺码</text>
				<text
					v-if="showManageLink"
					class="shop-manage-link"
					hover-class="tap-hover-opacity"
					:hover-stay-time="70"
					@click.stop="goManage"
				>管理</text>
				<text
					v-if="showDetailLink"
					class="shop-detail-link"
					hover-class="tap-hover-opacity"
					:hover-stay-time="70"
					@click.stop="goDetail"
				>{{ '详情 >>' }}</text>
			</view>
		</view>

		<view class="shop-main">
			<view class="shop-media" @click.stop>
				<ShopImageSwiper
					:images="images"
					:is-dark="isDark"
					height="100%"
					compact
					autoplay
				/>
			</view>

			<view class="shop-info">
				<view class="shop-info-row shop-info-row--code" hover-class="tap-hover-opacity" :hover-stay-time="70" @click.stop="onCopyCode">
					<text class="shop-info-label">店铺代码</text>
					<view class="shop-info-value-wrap">
						<text class="shop-info-code">{{ codeMaskText }}</text>
						<text class="shop-info-hint">复制</text>
					</view>
				</view>

				<view class="shop-info-row shop-info-row--address">
					<text class="shop-info-label">地址</text>
					<text class="shop-info-address">{{ addressText }}</text>
				</view>

				<view v-if="showPoints" class="shop-info-row shop-info-row--points">
					<text class="shop-info-label">剩余积分</text>
					<text class="shop-info-points">{{ remainingPointsText }}</text>
				</view>

				<view v-if="auditLabel" class="shop-info-audit">
					<text class="shop-audit-tag" :class="auditClass">{{ auditLabel }}</text>
				</view>
			</view>
		</view>

		<view v-if="qrcodeVisible" class="modal-mask" @click.stop="closeShopQrcode">
			<view class="modal-panel qrcode-panel" :class="isDark ? 'modal-panel--dark' : ''" @click.stop>
				<text class="modal-title">店铺邀请码</text>
				<text class="qrcode-hint">邀请码 2 分钟内有效，请及时扫码加入</text>
				<view v-if="qrcodeLoading" class="qrcode-state">
					<text class="qrcode-state-text">生成中…</text>
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
						:canvas-id="qrcodeCanvasId"
					/>
					<text v-if="qrcodeExpireHint" class="qrcode-expire">{{ qrcodeExpireHint }}</text>
				</view>
				<view class="modal-actions">
					<view class="modal-close" :class="isDark ? 'modal-close--dark' : ''" @click="closeShopQrcode">
						<text>关闭</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useExpiringQrcode } from '@/composables/use-expiring-qrcode.js'
import { formatShopAddress, getShopCarouselImages, reportShopEnterTimeAsync } from '@/api/modules/shop.js'
import { formatPointsAmount } from '@/utils/points-format.js'
import { fetchShopQrcode } from '@/api/modules/qrcode.js'
import ShopImageSwiper from '@/components/shop/shop-image-swiper.vue'
import QrcodeCanvas from '@/components/common/qrcode-canvas.vue'
import { canShowMemberTab, canManageShop, canPreviewShopByCode, canStaffVerifyAtShop } from '@/utils/wx-perm.js'
import { scanStaffVerifyCode } from '@/utils/staff-scan.js'
import { navigateWithShop } from '@/utils/shop-page-context.js'
import {
	getShopRoleLabel,
	getShopRoleTagType,
	isShopClerkCapable
} from '@/utils/shop-role.js'

/** 店铺名最大展示字数，超出以省略号显示 */
const SHOP_NAME_MAX_LEN = 10

const props = defineProps({
	shop: { type: Object, required: true },
	isDark: { type: Boolean, default: false },
	/** 会员页展示剩余积分 */
	showPoints: { type: Boolean, default: false },
	/** 主屏内嵌详情，不跳转子页面 */
	embedDetail: { type: Boolean, default: false }
})

const emit = defineEmits(['open-detail'])

const qrcodeVisible = ref(false)
const qrcodeLoading = ref(false)
const shopQrcodeImage = ref('')
const shopQrcodeFallback = ref('')
const shopQrcodeExpireAt = ref(0)

async function refreshShopQrcode() {
	const id = props.shop?.id
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

const displayShopName = computed(() => {
	const raw = (props.shop?.shopName || '').trim() || '未命名店铺'
	if (raw.length <= SHOP_NAME_MAX_LEN) return raw
	return `${raw.slice(0, SHOP_NAME_MAX_LEN)}…`
})

const roleLabel = computed(() => getShopRoleLabel(props.shop))

const roleTagClass = computed(() => {
	const type = getShopRoleTagType(props.shop)
	return type ? `shop-role-tag--${type}` : ''
})

const actionIconColor = computed(() => (props.isDark ? '#5ac8fa' : '#007aff'))

const remainingPointsText = computed(() => {
	const n = props.shop?.remainingPoints
	if (n == null || n === '') return '0.00'
	return formatPointsAmount(n)
})

const images = computed(() => getShopCarouselImages(props.shop))

const showDetailLink = computed(() => canShowMemberTab())

const showManageLink = computed(
	() => isShopClerkCapable(props.shop) && canManageShop()
)

const showScanVerifyBtn = computed(() => canStaffVerifyAtShop(props.shop))

const showInviteQrcodeBtn = computed(() => canPreviewShopByCode())

const qrcodeCanvasId = computed(() => `shop-qrcode-card-${props.shop?.id ?? 'x'}`)

const qrcodeExpireHint = computed(() => {
	if (!qrcodeCountdownLeft.value) return ''
	return `${qrcodeCountdownLeft.value} 秒后自动刷新`
})

const addressText = computed(() => formatShopAddress(props.shop) || '地址未填写')

const codeMaskText = computed(() => {
	const code = (props.shop?.shopCode || '').trim()
	if (!code) return '••••••••'
	if (code.length <= 4) return '••••'
	return `${code.slice(0, 2)}${'•'.repeat(Math.min(6, code.length - 4))}${code.slice(-2)}`
})

const auditLabel = computed(() => {
	const s = props.shop?.auditStatus
	if (s === 0) return '待审核'
	if (s === 1) return '已通过'
	if (s === 2) return '已驳回'
	return ''
})

const auditClass = computed(() => {
	const s = props.shop?.auditStatus
	if (s === 0) return 'shop-audit-tag--pending'
	if (s === 1) return 'shop-audit-tag--ok'
	if (s === 2) return 'shop-audit-tag--reject'
	return ''
})

async function openShopQrcode() {
	const id = props.shop?.id
	if (!id) {
		uni.showToast({ title: '店铺信息无效', icon: 'none' })
		return
	}
	qrcodeVisible.value = true
	await refreshShopQrcode()
}

function scanForVerify() {
	const id = props.shop?.id
	if (!id || !showScanVerifyBtn.value) return
	scanStaffVerifyCode({ shopId: id })
}

function closeShopQrcode() {
	qrcodeVisible.value = false
	stopQrcodeCountdown()
	qrcodeCountdownLeft.value = 0
}

function goDetail() {
	if (!showDetailLink.value) {
		uni.showToast({ title: '无查看详情权限', icon: 'none' })
		return
	}
	const item = props.shop
	const id = item?.id
	if (!id) {
		uni.showToast({ title: '店铺信息无效', icon: 'none' })
		return
	}
	reportShopEnterTimeAsync(id)
	if (props.embedDetail) {
		emit('open-detail', item)
		return
	}
	navigateWithShop({
		url: `/pages/member/shop-detail?id=${id}`,
		shop: item
	})
}

function goManage() {
	if (!showManageLink.value) {
		uni.showToast({ title: '无店铺管理权限', icon: 'none' })
		return
	}
	const item = props.shop
	const id = item?.id
	if (!id) {
		uni.showToast({ title: '店铺信息无效', icon: 'none' })
		return
	}
	navigateWithShop({
		url: `/pages/shop/manage?id=${id}`,
		shop: item
	})
}

function onCopyCode() {
	const code = (props.shop?.shopCode || '').trim()
	if (!code) {
		uni.showToast({ title: '暂无店铺代码', icon: 'none' })
		return
	}
	uni.setClipboardData({
		data: code,
		success: () => {
			uni.showToast({ title: '店铺代码已复制', icon: 'success' })
		},
		fail: () => {
			uni.showToast({ title: '复制失败', icon: 'none' })
		}
	})
}
</script>

<style scoped>
.shop-card {
	border-radius: 12px;
	overflow: hidden;
	margin-bottom: 10px;
	position: relative;
}

.shop-card--light {
	background-color: #ffffff;
	box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
}

.shop-card--dark {
	background-color: #1e1e1e;
	box-shadow: 0 1px 6px rgba(0, 0, 0, 0.28);
}

.shop-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 8px;
	padding: 10px 12px 8px;
}

.shop-header-left {
	flex: 1;
	min-width: 0;
	overflow: hidden;
}

.shop-title-group {
	display: inline-flex;
	align-items: center;
	gap: 6px;
	max-width: 100%;
}

.shop-name {
	flex-shrink: 1;
	min-width: 0;
	font-size: 15px;
	font-weight: 600;
	line-height: 1.35;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.shop-role-tag {
	flex-shrink: 0;
	font-size: 10px;
	padding: 1px 6px;
	border-radius: 4px;
	line-height: 1.45;
}

.shop-role-tag--manager {
	background: #fff7e6;
	color: #d48806;
}

.shop-role-tag--clerk {
	background: #e6f4ff;
	color: #1677ff;
}

.shop-role-tag--customer {
	background: #f6ffed;
	color: #389e0d;
}

.shop-card--dark .shop-role-tag--manager {
	background: #3d3520;
	color: #ffc53d;
}

.shop-card--dark .shop-role-tag--clerk {
	background: #1a2a3a;
	color: #5ac8fa;
}

.shop-card--dark .shop-role-tag--customer {
	background: #1f2e1a;
	color: #95de64;
}

.shop-header-actions {
	flex-shrink: 0;
	display: flex;
	align-items: center;
	gap: 8px;
}

.shop-qrcode-btn {
	width: 28px;
	height: 28px;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 14px;
}

.shop-card--light .shop-qrcode-btn {
	background-color: #f0f2f5;
}

.shop-card--dark .shop-qrcode-btn {
	background-color: #2a2a2a;
}

.shop-manage-link,
.shop-detail-link,
.shop-invite-link {
	flex-shrink: 0;
	font-size: 12px;
	color: #007aff;
	line-height: 1.35;
}

.shop-card--dark .shop-manage-link,
.shop-card--dark .shop-detail-link,
.shop-card--dark .shop-invite-link {
	color: #5ac8fa;
}

.shop-main {
	display: flex;
	align-items: stretch;
	gap: 10px;
	padding: 0 12px 12px;
	min-height: 96px;
}

.shop-media {
	flex-shrink: 0;
	width: 96px;
	height: 96px;
	border-radius: 8px;
	overflow: hidden;
}

.shop-info {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	justify-content: center;
	gap: 6px;
}

.shop-info-row {
	display: flex;
	align-items: flex-start;
	gap: 6px;
	min-width: 0;
}

.shop-info-label {
	flex-shrink: 0;
	width: 52px;
	font-size: 11px;
	line-height: 1.45;
	opacity: 0.5;
}

.shop-info-value-wrap {
	flex: 1;
	min-width: 0;
	display: flex;
	align-items: center;
	gap: 6px;
}

.shop-info-code {
	font-size: 13px;
	font-family: monospace;
	letter-spacing: 0.5px;
	line-height: 1.45;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.shop-info-hint {
	flex-shrink: 0;
	font-size: 10px;
	color: #007aff;
	line-height: 1.45;
}

.shop-card--dark .shop-info-hint {
	color: #5ac8fa;
}

.shop-info-address {
	flex: 1;
	min-width: 0;
	font-size: 12px;
	line-height: 1.45;
	opacity: 0.78;
	display: -webkit-box;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	overflow: hidden;
}

.shop-info-row--points {
	align-items: center;
}

.shop-info-points {
	flex: 1;
	font-size: 15px;
	font-weight: 600;
	color: #007aff;
	line-height: 1.2;
	text-align: right;
}

.shop-card--dark .shop-info-points {
	color: #5ac8fa;
}

.shop-info-audit {
	margin-top: 2px;
}

.shop-audit-tag {
	font-size: 10px;
	padding: 2px 6px;
	border-radius: 4px;
	display: inline-block;
}

.shop-audit-tag--pending {
	background: #fff7e6;
	color: #d48806;
}

.shop-audit-tag--ok {
	background: #f6ffed;
	color: #389e0d;
}

.shop-audit-tag--reject {
	background: #fff2f0;
	color: #cf1322;
}

.shop-card--dark .shop-audit-tag--pending {
	background: #3d3520;
	color: #ffc53d;
}

.shop-card--dark .shop-audit-tag--ok {
	background: #1f2e1a;
	color: #95de64;
}

.shop-card--dark .shop-audit-tag--reject {
	background: #3a2020;
	color: #ff7875;
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

.qrcode-hint {
	display: block;
	font-size: 12px;
	opacity: 0.55;
	margin-bottom: 12px;
	text-align: center;
}

.qrcode-state {
	padding: 24px 0;
	text-align: center;
}

.qrcode-state-text {
	font-size: 14px;
	opacity: 0.6;
}

.qrcode-image-wrap {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 10px;
}

.qrcode-image {
	width: 220px;
	height: 220px;
}

.qrcode-expire {
	font-size: 12px;
	opacity: 0.6;
}

.modal-actions {
	margin-top: 16px;
}

.modal-close {
	text-align: center;
	padding: 10px;
	border-radius: 8px;
	font-size: 14px;
	background: #f5f5f5;
}

.modal-close--dark {
	background: #2a2a2a;
}
</style>
