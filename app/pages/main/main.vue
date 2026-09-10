<template>
	<view class="page" :class="isDark ? 'theme-dark' : 'theme-light'">
		<view class="nav-bar" :style="navBarStyle">
			<view class="nav-bar-inner" :style="navInnerStyle">
				<view class="nav-left-actions">
					<view
						v-if="activeTab === 'member' && memberDetailShopId && canReturnToMemberList"
						class="nav-back-btn"
						hover-class="tap-hover-opacity-light"
						:hover-stay-time="70"
						@click="closeMemberDetail"
					>
						<text class="nav-back-glyph">‹</text>
					</view>
					<view
						v-else
						class="theme-toggle"
						hover-class="tap-hover-opacity-light"
						:hover-stay-time="70"
						@click="toggleTheme"
					>
						<text
							class="theme-glyph"
							:class="isDark ? 'theme-glyph--moon' : 'theme-glyph--sun'"
						>{{ isDark ? '☽' : '☀' }}</text>
					</view>
				</view>
				<text class="nav-title">尚品发艺会员助手</text>
			</view>
		</view>

		<view
			class="body"
			:class="{
				'body--member-detail': activeTab === 'member' && memberDetailShopId,
				'body--orders': activeTab === 'orders'
			}"
			:style="bodyPadStyle"
		>
			<view v-if="activeTab === 'member'" class="shop-panel member-panel">
				<view v-if="!memberDetailShopId && (canMemberSearch || canMemberJoin || showAddShopBtn)" class="shop-toolbar">
					<view
						v-if="showAddShopBtn && !memberSearchOpen"
						class="shop-add-btn"
						hover-class="tap-hover-opacity"
						:hover-stay-time="70"
						@click="goAddShop"
					>
						<text class="shop-add-btn-text">添加</text>
					</view>
					<view class="shop-toolbar-end">
						<view v-if="canMemberSearch && memberSearchOpen" class="shop-search-bar">
							<uni-icons class="shop-search-bar-icon" type="search" :size="18" :color="searchIconColor" />
							<input
								v-model="memberSearchKeyword"
								class="shop-search-input"
								type="text"
								confirm-type="search"
								placeholder="完整店铺代码"
								placeholder-class="shop-search-placeholder"
								:focus="memberSearchFocus"
								@confirm="onMemberSearchConfirm"
							/>
							<view
								class="shop-search-clear"
								hover-class="tap-hover-opacity"
								:hover-stay-time="70"
								@click="closeMemberSearch"
							>
								<text class="shop-search-clear-text">×</text>
							</view>
						</view>
						<view
							v-else-if="canMemberSearch"
							class="shop-search-trigger"
							hover-class="tap-hover-opacity"
							:hover-stay-time="70"
							@click="openMemberSearch"
						>
							<uni-icons type="search" :size="22" :color="searchIconColor" />
						</view>
						<view
							v-if="canMemberJoin && !memberSearchOpen"
							class="shop-search-trigger"
							hover-class="tap-hover-opacity"
							:hover-stay-time="70"
							@click="scanShopCode"
						>
							<uni-icons type="scan" :size="22" :color="searchIconColor" />
						</view>
					</view>
				</view>
				<view v-if="memberDetailShopId" class="member-detail-panel">
					<MemberShopDetailContent
						ref="memberDetailRef"
						:shop-id="memberDetailShopId"
						:is-dark="isDark"
						embedded
					/>
				</view>
				<scroll-view
					v-else
					:class="['shop-scroll', (canMemberSearch || canMemberJoin || showAddShopBtn) ? 'shop-scroll--with-toolbar' : '']"
					scroll-y
					:lower-threshold="80"
					:refresher-enabled="true"
					:refresher-triggered="memberRefreshing"
					@refresherrefresh="onMemberRefresh"
					@scrolltolower="loadMoreMemberShops"
				>
					<PageLoading
						v-if="memberSearchLoading"
						text="查询店铺中…"
						:color="loadingColor"
					/>
					<view v-else-if="memberEnterPreview" class="member-enter-preview">
						<ShopImageSwiper :images="memberPreviewImages" :is-dark="isDark" />
						<view class="member-preview-body">
							<text class="member-preview-name">{{ memberEnterPreview.shopName || '未命名店铺' }}</text>
							<view class="member-preview-row">
								<text class="member-preview-label">联系电话</text>
								<text class="member-preview-value">{{ memberEnterPreview.phone || '未填写' }}</text>
							</view>
							<view v-if="memberEnterPreview.ratio != null" class="member-preview-row">
								<text class="member-preview-label">积分比例</text>
								<text class="member-preview-value">1 元 = {{ memberEnterPreview.ratio }} 积分</text>
							</view>
							<text class="member-preview-address">{{ memberEnterPreview.address || '地址未填写' }}</text>
							<view v-if="memberEnterPreview.products?.length" class="member-product-block">
								<text class="member-product-title">商品</text>
								<view
									v-for="p in memberEnterPreview.products"
									:key="p.productId"
									class="member-product-row"
								>
									<text class="member-product-name">{{ p.productName }}</text>
									<view class="member-product-points">
										<text class="member-product-points-num">{{ formatPreviewProductPoints(p) }}</text>
										<text class="member-product-points-unit">积分</text>
									</view>
								</view>
							</view>
							<view
								v-if="canMemberJoin"
								class="member-join-btn"
								:class="{ 'member-join-btn--disabled': memberAlreadyJoined || memberJoining }"
								:hover-class="memberAlreadyJoined || memberJoining ? 'none' : 'tap-hover-opacity-mid'"
								:hover-stay-time="70"
								@click="onJoinShop"
							>
								<text class="member-join-btn-text">{{ memberJoinBtnText }}</text>
							</view>
						</view>
					</view>
					<template v-else>
						<PageLoading
							v-if="memberLoading && !memberList.length"
							text="加载店铺中…"
							:color="loadingColor"
						/>
						<view v-else-if="!memberList.length" class="shop-state">
							<text class="shop-state-text">{{ memberEmptyText }}</text>
						</view>
						<template v-else>
							<ShopCard
								v-for="item in memberList"
								:key="item.id"
								:shop="item"
								:is-dark="isDark"
								show-points
								embed-detail
								@open-detail="openMemberDetail"
							/>
							<PageLoading
								v-if="memberLoadingMore"
								text="加载更多…"
								compact
								:color="loadingColor"
							/>
						</template>
					</template>
				</scroll-view>
			</view>

			<view v-if="activeTab === 'orders'" class="orders-panel">
				<MemberOrdersContent ref="memberOrdersRef" class="orders-panel-content" :is-dark="isDark" />
			</view>

			<view v-if="activeTab === 'mine'" class="mine-panel">
				<MainMinePanel
					class="mine-panel-component"
					:user-profile="userProfile"
					:display-nickname="displayNickname"
					:profile-sub-text="profileSubText"
					:coupon-count="couponCount"
					:pay-password-label="payPasswordLabel"
					:ticket-unread-count="ticketUnreadCount"
					:login-error="loginError"
					:is-dark="isDark"
					@profile="goProfile"
					@coupons="goCoupons"
					@pay-qrcode="goPayQrcode"
					@set-password="goSetPassword"
					@tickets="goTickets"
				/>
			</view>
		</view>

		<view class="tab-bar" :style="tabBarSafeStyle">
			<view
				v-for="item in visibleTabs"
				:key="item.key"
				class="tab-item"
				:class="{ active: activeTab === item.key }"
				hover-class="tap-hover-opacity-light"
				:hover-stay-time="70"
				@click="onTabClick(item.key)"
			>
				<text class="tab-label">{{ item.label }}</text>
			</view>
		</view>

		<PageLoading
			v-if="sessionBooting"
			text="正在进入…"
			overlay
			:color="loadingColor"
			:overlay-bg="isDark ? 'rgba(18, 18, 18, 0.78)' : 'rgba(255, 255, 255, 0.72)'"
		/>
	</view>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { getToken, fetchCurrentUserInfo } from '@/api/modules/auth.js'
import { bootstrapAppSession } from '@/services/app-session.js'
import { ensureAuthenticated } from '@/services/auth-relogin.js'
import {
	getUserProfile,
	hasCompletedProfileSetup
} from '@/services/user-profile.js'
import { fetchUnusedCouponCount } from '@/api/modules/coupon.js'
import { fetchTicketUnreadCount } from '@/api/modules/ticket.js'
import { connectNotifySocket } from '@/services/notify-socket.js'
import { useTicketNotify } from '@/composables/use-ticket-notify.js'
import { hasPayPasswordSet } from '@/services/user-security.js'
import { setProfileEditCache } from '@/services/profile-cache.js'
import { getPermRevision } from '@/utils/permissions.js'
import {
	canAddShop,
	canShowMemberTab,
	canPreviewShopByCode,
	canJoinShop,
	canGeneratePayQrcode
} from '@/utils/wx-perm.js'
import {
	parseLaunchShopInvite,
	savePendingShopCode,
	savePendingShopInvite
} from '@/utils/qrcode-scan.js'
import ShopCard from '@/components/shop/shop-card.vue'
import ShopImageSwiper from '@/components/shop/shop-image-swiper.vue'
import MemberShopDetailContent from '@/components/member/member-shop-detail-content.vue'
import MemberOrdersContent from '@/components/member/member-orders-content.vue'
import MainMinePanel from '@/components/main/main-mine-panel.vue'
import PageLoading from '@/components/common/page-loading.vue'
import { useTheme } from '@/composables/use-theme.js'
import { useMainMemberShops } from '@/composables/use-main-member-shops.js'

const NAV_INNER_HEIGHT = 44

const { isDark, loadTheme, toggleTheme } = useTheme()

const ALL_TABS = [
	{ key: 'member', label: '会员', visible: () => canShowMemberTab() },
	{ key: 'orders', label: '订单', visible: () => canShowMemberTab() },
	{ key: 'mine', label: '我的', visible: () => true }
]

const permRevision = getPermRevision()

const visibleTabs = computed(() => {
	permRevision.value
	return ALL_TABS.filter((t) => t.visible())
})

const canMemberSearch = computed(() => {
	permRevision.value
	return canPreviewShopByCode()
})
const showAddShopBtn = computed(() => {
	permRevision.value
	return canAddShop()
})
const canMemberJoin = computed(() => {
	permRevision.value
	return canJoinShop()
})

const activeTab = ref('member')
const memberOrdersRef = ref(null)
const memberDetailRef = ref(null)
/** 启动参数：购买成功等场景跳回指定店铺详情 */
let launchShopId = ''
const statusBarHeight = ref(0)
/** 导航栏右侧留白，避免与微信胶囊按钮重叠 */
const navPaddingRight = ref(16)
const loginError = ref('')
const sessionBooting = ref(false)
const userProfile = ref({ avatar: '', nickname: '' })
const couponCount = ref(0)
const ticketUnreadCount = ref(0)
const payPasswordSet = ref(false)

const {
	memberList,
	memberLoading,
	memberLoadingMore,
	memberLoadError,
	memberSearchOpen,
	memberSearchKeyword,
	memberSearchFocus,
	memberSearchLoading,
	memberEnterPreview,
	memberJoining,
	memberRefreshing,
	memberDetailShopId,
	memberDetailShop,
	memberEmptyText,
	memberPreviewImages,
	memberAlreadyJoined,
	memberJoinBtnText,
	canReturnToMemberList,
	invalidateMemberListCache,
	reloadMemberList,
	onMemberRefresh,
	loadMoreMemberShops,
	goAddShop,
	openMemberSearch,
	closeMemberSearch,
	onMemberSearchConfirm,
	scanShopCode,
	handlePendingShopCode,
	formatPreviewProductPoints,
	onJoinShop,
	openMemberDetail,
	closeMemberDetail,
	tryAutoEnterSingleShop,
	openMemberDetailById
} = useMainMemberShops({ activeTab, canMemberSearch, showAddShopBtn, canMemberJoin })

const searchIconColor = computed(() => (isDark.value ? '#b8b8b8' : '#666666'))
const loadingColor = computed(() => (isDark.value ? '#5ac8fa' : '#007aff'))

const displayNickname = computed(() => {
	const name = (userProfile.value.nickname || '').trim()
	return name || '加载中…'
})

const profileSubText = computed(() => {
	if (userProfile.value.isDefault) return '默认昵称，点击可修改'
	return '点击编辑资料'
})

const payPasswordLabel = computed(() => (payPasswordSet.value ? '已设置' : '未设置'))

const navBarStyle = computed(() => ({
	paddingTop: `${statusBarHeight.value || 0}px`
}))

const navInnerStyle = computed(() => ({
	paddingRight: `${navPaddingRight.value}px`
}))

const bodyPadStyle = computed(() => ({
	paddingTop: `${(statusBarHeight.value || 0) + NAV_INNER_HEIGHT}px`
}))

const tabBarSafeStyle = computed(() => {
	const bottom = uni.getSystemInfoSync().safeAreaInsets?.bottom || 0
	return { paddingBottom: `${bottom}px` }
})

/** 是否已应用启动默认 Tab（避免从子页返回时反复切回会员） */
let defaultTabApplied = false

/** 在可见 Tab 中优先选中会员页 */
function resolveDefaultTab(keys) {
	if (keys.includes('member')) return 'member'
	return keys[0] || 'mine'
}

/** 当前 Tab 无权限时切到默认 Tab */
function ensureActiveTabValid() {
	const keys = visibleTabs.value.map((t) => t.key)
	if (!keys.includes(activeTab.value)) {
		activeTab.value = resolveDefaultTab(keys)
	}
}

/** 启动时若有会员 Tab 则默认进入会员页（仅执行一次） */
let launchTabFromQuery = false

function applyDefaultTabOnLaunch() {
	if (defaultTabApplied) return
	defaultTabApplied = true
	if (launchTabFromQuery) return
	const keys = visibleTabs.value.map((t) => t.key)
	if (keys.includes('member')) {
		activeTab.value = 'member'
	} else if (!keys.includes(activeTab.value)) {
		activeTab.value = resolveDefaultTab(keys)
	}
}

function onTabClick(key) {
	activeTab.value = key
	if (key === 'orders') {
		// v-if 挂载后 ref 才可用，下一帧再检查缓存是否过期
		setTimeout(() => memberOrdersRef.value?.refreshIfStale?.(), 0)
	}
}

/** 刷新「我的」页展示数据 */
async function refreshMineData() {
	userProfile.value = getUserProfile()
	payPasswordSet.value = hasPayPasswordSet()
	ensureActiveTabValid()
	const couponRes = await fetchUnusedCouponCount()
	if (couponRes.ok) couponCount.value = couponRes.count
	const ticketRes = await fetchTicketUnreadCount()
	if (ticketRes.ok) ticketUnreadCount.value = ticketRes.count
}

watch(visibleTabs, () => {
	ensureActiveTabValid()
})

watch(permRevision, () => {
	ensureActiveTabValid()
	invalidateMemberListCache()
	if (activeTab.value === 'member' && canShowMemberTab()) {
		reloadMemberList(true).then(() => {
			if (!memberDetailShopId.value) tryAutoEnterSingleShop()
		})
	}
})

watch(memberDetailShopId, async (id) => {
	if (!id) return
	await nextTick()
	const shop = memberDetailShop.value
	if (shop) {
		memberDetailRef.value?.applyShop?.(shop)
	}
})

watch(activeTab, (tab) => {
	if (tab !== 'member') {
		closeMemberSearch()
	}
})

const LOGIN_PAGE = '/pages/login/login'
const PROFILE_SETUP_PAGE = '/pages/login/profile-setup'

function goLoginPage() {
	uni.redirectTo({ url: LOGIN_PAGE })
}

function goProfileSetupPage() {
	uni.redirectTo({ url: PROFILE_SETUP_PAGE })
}

/** 启动会话：未登录则跳转登录页 */
async function initSession() {
	loginError.value = ''
	sessionBooting.value = true
	try {
		const { loginResult, needLogin } = await bootstrapAppSession()
		if (needLogin) {
			goLoginPage()
			return
		}
		if (!hasCompletedProfileSetup()) {
			goProfileSetupPage()
			return
		}
		if (!loginResult.ok) {
			loginError.value = loginResult.msg || '登录失败，请稍后重试'
		}
		refreshMineData()
		applyDefaultTabOnLaunch()
		if (canShowMemberTab()) {
			await reloadMemberList()
			if (launchShopId) {
				openMemberDetailById(launchShopId)
				launchShopId = ''
			} else {
				tryAutoEnterSingleShop()
			}
		}
		if (canAddShop()) {
			// #ifdef MP-WEIXIN
			try {
				uni.preloadPage({ url: '/pages/shop/add' })
			} catch {
				/* 忽略 */
			}
			// #endif
		}
		await handlePendingShopCode()
		ensureActiveTabValid()
	} finally {
		sessionBooting.value = false
	}
}

async function refreshTicketUnreadOnly() {
	const ticketRes = await fetchTicketUnreadCount()
	if (ticketRes.ok) ticketUnreadCount.value = ticketRes.count
}

const { subscribe: subscribeTicketNotify } = useTicketNotify((payload) => {
	if (payload?.type === 'unread_changed') {
		void refreshTicketUnreadOnly()
	}
})

onLoad((options) => {
	const launchInvite = parseLaunchShopInvite(options)
	if (launchInvite?.token) {
		savePendingShopInvite(launchInvite.token)
	} else if (launchInvite?.shopCode) {
		savePendingShopCode(launchInvite.shopCode)
	}
	if (options?.tab === 'orders' && canShowMemberTab()) {
		activeTab.value = 'orders'
		launchTabFromQuery = true
	} else if (options?.tab === 'mine') {
		activeTab.value = 'mine'
		launchTabFromQuery = true
	}
	if (options?.shopId) {
		launchShopId = String(options.shopId)
	}
	const sys = uni.getSystemInfoSync()
	statusBarHeight.value = sys.statusBarHeight || 20
	// #ifdef MP-WEIXIN
	try {
		const menu = uni.getMenuButtonBoundingClientRect()
		if (menu && menu.left > 0) {
			navPaddingRight.value = Math.max(16, sys.windowWidth - menu.left + 8)
		}
	} catch {
		navPaddingRight.value = 96
	}
	// #endif
	loadTheme()
	initSession()
	subscribeTicketNotify()
})

onShow(async () => {
	if (!getToken()) {
		const auth = await ensureAuthenticated()
		if (!auth.ok) return
	}
	connectNotifySocket()
	userProfile.value = getUserProfile()
	await fetchCurrentUserInfo()
	payPasswordSet.value = hasPayPasswordSet()
	ensureActiveTabValid()
	if (activeTab.value === 'mine') {
		const couponRes = await fetchUnusedCouponCount()
		if (couponRes.ok) couponCount.value = couponRes.count
	}
	if (activeTab.value === 'orders') {
		memberOrdersRef.value?.refreshIfStale?.()
	}
	if (activeTab.value === 'member' && memberDetailShopId.value) {
		memberDetailRef.value?.syncCartFromCache?.()
	}
})

/** 跳转个人资料（微信小程序不支持 uni.preloadPage） */
function goProfile() {
	setProfileEditCache(getUserProfile())
	uni.navigateTo({
		url: '/pages/mine/profile',
		animationType: 'slide-in-right',
		animationDuration: 200
	})
}

/** 跳转设置支付密码 */
function goSetPassword() {
	uni.navigateTo({
		url: '/pages/mine/set-password',
		animationType: 'slide-in-right',
		animationDuration: 200
	})
}

/** 跳转折扣券页 */
function goCoupons() {
	uni.navigateTo({
		url: '/pages/mine/coupons',
		animationType: 'slide-in-right',
		animationDuration: 200
	})
}

/** 跳转客服工单页 */
function goTickets() {
	uni.navigateTo({
		url: '/pages/mine/tickets',
		animationType: 'slide-in-right',
		animationDuration: 200
	})
}

/** 跳转付款码页 */
function goPayQrcode() {
	if (!canGeneratePayQrcode()) {
		uni.showToast({ title: '无付款码权限', icon: 'none' })
		return
	}
	uni.navigateTo({
		url: '/pages/member/pay-qrcode',
		animationType: 'slide-in-right',
		animationDuration: 200
	})
}
</script>

<style scoped>
.page {
	height: 100vh;
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	box-sizing: border-box;
	overflow: hidden;
}

.theme-light {
	background-color: #f5f5f5;
	color: #333333;
}

.theme-dark {
	background-color: #121212;
	color: #e8e8e8;
}

.nav-bar {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	z-index: 10;
	box-sizing: border-box;
}

.nav-bar-inner {
	position: relative;
	height: 44px;
	padding-left: 12px;
	display: flex;
	align-items: center;
}

.nav-left-actions {
	position: relative;
	z-index: 2;
	display: flex;
	align-items: center;
	gap: 4px;
	flex-shrink: 0;
}

.theme-toggle {
	width: 36px;
	height: 36px;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.nav-back-btn {
	width: 36px;
	height: 36px;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.nav-back-glyph {
	font-size: 28px;
	font-weight: 300;
	line-height: 1;
	margin-top: -2px;
}

.theme-light .nav-back-glyph {
	color: #007aff;
}

.theme-dark .nav-back-glyph {
	color: #5ac8fa;
}

.theme-light .nav-bar {
	background-color: #ffffff;
	border-bottom: 1px solid #eeeeee;
}

.theme-dark .nav-bar {
	background-color: #1e1e1e;
	border-bottom: 1px solid #2a2a2a;
}

.nav-title {
	position: absolute;
	left: 0;
	right: 0;
	text-align: center;
	font-size: 17px;
	font-weight: 600;
	line-height: 44px;
	pointer-events: none;
}

.theme-glyph {
	font-size: 17px;
	line-height: 1;
}

.theme-glyph--sun {
	color: #f5a623;
}

.theme-glyph--moon {
	color: #8ab4f8;
}

.body {
	flex: 1;
	padding: 12px 16px 100px;
	display: flex;
	flex-direction: column;
	align-items: center;
	box-sizing: border-box;
	min-height: 0;
}

.body--member-detail {
	padding: 0 0 100px;
	align-items: stretch;
}

.body--member-detail .shop-panel {
	max-width: none;
}

.body--orders {
	overflow: hidden;
}

.orders-panel {
	width: 100%;
	max-width: 480px;
	align-self: stretch;
	flex: 1;
	min-height: 0;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}

.orders-panel-content {
	flex: 1;
	min-height: 0;
	width: 100%;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}

.mine-panel {
	width: 100%;
	max-width: 400px;
	align-self: stretch;
	display: flex;
	flex-direction: column;
	gap: 16px;
}

.mine-panel-component {
	width: 100%;
	display: block;
}

.shop-panel,
.member-panel {
	width: 100%;
	flex: 1;
	min-height: 0;
	display: flex;
	flex-direction: column;
}

.member-detail-panel {
	width: 100%;
	flex: 1;
	min-height: 0;
	display: flex;
	flex-direction: column;
	align-self: stretch;
}

.member-enter-preview {
	width: 100%;
}

.member-preview-body {
	padding: 12px 4px 8px;
	display: flex;
	flex-direction: column;
	gap: 10px;
}

.member-preview-name {
	font-size: 18px;
	font-weight: 600;
	line-height: 1.35;
}

.member-preview-row {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 8px;
	padding: 8px 10px;
	border-radius: 8px;
}

.theme-light .member-preview-row {
	background-color: #f5f7fa;
}

.theme-dark .member-preview-row {
	background-color: #2a2a2a;
}

.member-preview-label {
	font-size: 12px;
	opacity: 0.55;
}

.member-preview-value {
	font-size: 14px;
	flex: 1;
}

.member-preview-address {
	font-size: 13px;
	line-height: 1.5;
	opacity: 0.72;
}

.member-product-block {
	display: flex;
	flex-direction: column;
	gap: 8px;
}

.member-product-title {
	font-size: 14px;
	font-weight: 600;
	opacity: 0.85;
}

.member-product-row {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 8px 10px;
	border-radius: 8px;
}

.theme-light .member-product-row {
	background-color: #f5f7fa;
}

.theme-dark .member-product-row {
	background-color: #2a2a2a;
}

.member-product-name {
	font-size: 14px;
	flex: 1;
	min-width: 0;
}

.member-product-points {
	display: flex;
	align-items: baseline;
	flex-shrink: 0;
	margin-left: 8px;
}

.member-product-points-num {
	font-size: 15px;
	font-weight: 600;
	color: #c9a227;
	line-height: 1;
}

.member-product-points-unit {
	margin-left: 3px;
	font-size: 11px;
	color: #c9a227;
	line-height: 1;
}

.theme-dark .member-product-points-num,
.theme-dark .member-product-points-unit {
	color: #e0b84a;
}

.member-join-btn {
	margin-top: 8px;
	height: 44px;
	border-radius: 22px;
	display: flex;
	align-items: center;
	justify-content: center;
}

.theme-light .member-join-btn {
	background-color: #007aff;
}

.theme-dark .member-join-btn {
	background-color: #0a84ff;
}

.member-join-btn--disabled {
	opacity: 0.45;
}

.member-join-btn-text {
	font-size: 15px;
	color: #ffffff;
	font-weight: 500;
}

.shop-panel {
	width: 100%;
	max-width: 480px;
	align-self: stretch;
	flex: 1;
	min-height: 0;
	display: flex;
	flex-direction: column;
}

.shop-toolbar {
	width: 100%;
	padding-bottom: 10px;
	flex-shrink: 0;
	display: flex;
	align-items: center;
	gap: 10px;
}

.shop-toolbar-end {
	flex: 1;
	display: flex;
	justify-content: flex-end;
	min-width: 0;
}

.shop-search-trigger {
	width: 40px;
	height: 40px;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 20px;
}

.theme-light .shop-search-trigger {
	background-color: #f0f2f5;
}

.theme-dark .shop-search-trigger {
	background-color: #2a2a2a;
}

.shop-search-bar {
	flex: 1;
	display: flex;
	align-items: center;
	height: 40px;
	padding: 0 6px 0 12px;
	border-radius: 20px;
	gap: 6px;
	box-sizing: border-box;
}

.theme-light .shop-search-bar {
	background-color: #f0f2f5;
}

.theme-dark .shop-search-bar {
	background-color: #2a2a2a;
}

.shop-search-bar-icon {
	flex-shrink: 0;
}

.shop-search-input {
	flex: 1;
	min-width: 0;
	height: 40px;
	font-size: 14px;
	line-height: 40px;
	background: transparent;
}

.theme-light .shop-search-input {
	color: #333333;
}

.theme-dark .shop-search-input {
	color: #e8e8e8;
}

.shop-search-placeholder {
	font-size: 14px;
	color: #767676;
}

.shop-search-clear {
	width: 28px;
	height: 28px;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	border-radius: 14px;
}

.theme-light .shop-search-clear {
	background-color: rgba(0, 0, 0, 0.06);
}

.theme-dark .shop-search-clear {
	background-color: rgba(255, 255, 255, 0.1);
}

.shop-search-clear-text {
	font-size: 18px;
	line-height: 1;
}

.theme-light .shop-search-clear-text {
	color: #666666;
}

.theme-dark .shop-search-clear-text {
	color: #b0b0b0;
}

.shop-add-btn {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	height: 32px;
	padding: 0 10px;
	border-radius: 16px;
	box-sizing: border-box;
	border-width: 1px;
	border-style: solid;
}

.theme-light .shop-add-btn {
	background-color: rgba(0, 122, 255, 0.08);
	border-color: rgba(0, 122, 255, 0.18);
}

.theme-dark .shop-add-btn {
	background-color: rgba(90, 200, 250, 0.12);
	border-color: rgba(90, 200, 250, 0.22);
}

.shop-add-btn-text {
	font-size: 12px;
	font-weight: 500;
	line-height: 1;
}

.theme-light .shop-add-btn-text {
	color: #007aff;
}

.theme-dark .shop-add-btn-text {
	color: #5ac8fa;
}

.shop-scroll {
	flex: 1;
	height: calc(100vh - 140px);
	width: 100%;
}

.shop-scroll--with-toolbar {
	height: calc(100vh - 184px);
}

.shop-section + .shop-section {
	margin-top: 8px;
}

.shop-section-title {
	display: block;
	padding: 4px 4px 10px;
	font-size: 14px;
	font-weight: 600;
	opacity: 0.72;
}

.shop-state {
	padding: 48px 16px;
	text-align: center;
}

.shop-state-text {
	font-size: 14px;
}

.theme-light .shop-state-text {
	color: #666666;
}

.theme-dark .shop-state-text {
	color: #a0a0a0;
}

.tab-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	display: flex;
	flex-direction: row;
	border-top-width: 1px;
	border-top-style: solid;
	z-index: 10;
}

.theme-light .tab-bar {
	background-color: #ffffff;
	border-top-color: #e5e5e5;
}

.theme-dark .tab-bar {
	background-color: #1e1e1e;
	border-top-color: #2a2a2a;
}

.tab-item {
	flex: 1;
	padding: 10px 0 12px;
	display: flex;
	align-items: center;
	justify-content: center;
}

.tab-label {
	font-size: 14px;
	opacity: 0.55;
}

.tab-item.active .tab-label {
	opacity: 1;
	font-weight: 600;
}

.theme-light .tab-item.active .tab-label {
	color: #007aff;
}

.theme-dark .tab-item.active .tab-label {
	color: #5ac8fa;
}
</style>
