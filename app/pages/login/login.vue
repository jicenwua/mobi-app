<template>
	<view class="page">
		<view class="page-header" :style="headerStyle">
			<view
				class="back-btn"
				hover-class="tap-hover-opacity"
				:hover-stay-time="70"
				@click="goBack"
			>
				<text class="back-glyph">‹</text>
				<text class="back-text">返回</text>
			</view>
		</view>

		<view class="brand">
			<view class="brand-logo-wrap">
				<image class="brand-logo" src="/static/尚品发艺.png" mode="aspectFill" />
			</view>
			<text class="brand-title">尚品发艺会员助手</text>
			<text class="brand-desc">登录后可使用店铺与会员服务</text>
		</view>

		<view class="actions">
			<button
				class="wx-login-btn"
				:loading="loggingIn"
				:disabled="loggingIn || !agreedToPrivacy"
				@click="onWxLoginTap"
			>
				{{ loggingIn ? '登录中…' : '微信一键登录' }}
			</button>

			<view class="privacy-row">
				<view class="privacy-label" @click="onPrivacyToggle">
					<view
						class="privacy-check-box"
						:class="{ 'privacy-check-box--checked': agreedToPrivacy }"
					>
						<text v-if="agreedToPrivacy" class="privacy-check-mark">✓</text>
					</view>
					<text class="privacy-text">
						我已阅读并同意
						<text class="privacy-link" @click.stop="openPrivacyGuide">
							{{ privacyContractLabel }}
						</text>
					</text>
				</view>
			</view>

			<text v-if="errorMsg" class="error-text">{{ errorMsg }}</text>
		</view>

		<PrivacyPopup />
	</view>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { onLoad, onReady } from '@dcloudio/uni-app'
import { fetchCurrentUserInfo } from '@/api/modules/auth.js'
import { performQuickLogin, resolvePostLoginPath } from '@/services/login-flow.js'
import { resetAppSession } from '@/services/app-session.js'
import { setGuestMode } from '@/services/guest-mode.js'
import { hasCompletedProfileSetup } from '@/services/user-profile.js'
import PrivacyPopup from '@/components/privacy/privacy-popup.vue'
import {
	openPrivacyContract,
	getPrivacySettingState,
	showPrivacyPopupManually,
	wxPrivacyAuthorized,
	formatPrivacyContractLabel
} from '@/utils/wx-privacy.js'
import {
	parseLaunchShopInvite,
	savePendingShopCode,
	savePendingShopInvite
} from '@/utils/qrcode-scan.js'

const loggingIn = ref(false)
const errorMsg = ref('')
const agreedToPrivacy = ref(false)
const privacyContractName = ref('用户隐私保护指引')
const statusBarHeight = ref(0)

const headerStyle = computed(() => ({
	paddingTop: `${statusBarHeight.value || 0}px`
}))

const privacyContractLabel = computed(() =>
	formatPrivacyContractLabel(privacyContractName.value)
)

onLoad(async (options) => {
	const sys = uni.getSystemInfoSync()
	statusBarHeight.value = sys.statusBarHeight || 20

	const launchInvite = parseLaunchShopInvite(options)
	if (launchInvite?.token) {
		savePendingShopInvite(launchInvite.token)
	} else if (launchInvite?.shopCode) {
		savePendingShopCode(launchInvite.shopCode)
	}
	// 校验 token 是否仍有效，避免主/登录页因失效 token 互相跳转
	const info = await fetchCurrentUserInfo()
	if (!info.ok) return
	const target = hasCompletedProfileSetup()
		? '/pages/main/main'
		: '/pages/login/profile-setup'
	uni.reLaunch({ url: target })
})

/** 进入页面：同步微信隐私状态，首次进入需弹出隐私指引 */
onReady(() => {
	getPrivacySettingState().then((state) => {
		privacyContractName.value = state.privacyContractName
		if (!state.needAuthorization) {
			wxPrivacyAuthorized.value = true
		}
		if (wxPrivacyAuthorized.value) {
			agreedToPrivacy.value = true
			return
		}
		if (state.needAuthorization) {
			showPrivacyPopupManually()
		}
	})
})

watch(wxPrivacyAuthorized, (newVal) => {
	if (newVal) {
		agreedToPrivacy.value = true
	}
})

function onPrivacyToggle() {
	if (agreedToPrivacy.value) {
		agreedToPrivacy.value = false
		return
	}
	if (!wxPrivacyAuthorized.value) {
		showPrivacyPopupManually()
		return
	}
	agreedToPrivacy.value = true
}

function openPrivacyGuide() {
	openPrivacyContract()
}

function goBack() {
	if (loggingIn.value) return
	setGuestMode(true)
	const pages = getCurrentPages()
	if (pages.length > 1) {
		uni.navigateBack()
		return
	}
	uni.reLaunch({ url: '/pages/main/main' })
}

function onWxLoginTap() {
	if (loggingIn.value) return
	errorMsg.value = ''

	if (!agreedToPrivacy.value) {
		uni.showToast({ title: '请先勾选同意隐私政策', icon: 'none' })
		return
	}

	// #ifndef MP-WEIXIN
	errorMsg.value = '请在微信小程序中使用'
	uni.showToast({ title: errorMsg.value, icon: 'none' })
	return
	// #endif

	if (!wxPrivacyAuthorized.value) {
		showPrivacyPopupManually()
		uni.showToast({ title: '请先同意隐私保护指引', icon: 'none' })
		return
	}

	runLogin()
}

async function runLogin() {
	if (loggingIn.value) return
	loggingIn.value = true
	try {
		const result = await performQuickLogin()
		if (!result.ok) {
			errorMsg.value = result.msg || '登录失败'
			uni.showToast({ title: errorMsg.value, icon: 'none' })
			return
		}
		resetAppSession()
		uni.reLaunch({ url: resolvePostLoginPath(result.register) })
	} catch (e) {
		errorMsg.value = e.message || '登录失败'
		uni.showToast({ title: errorMsg.value, icon: 'none' })
	} finally {
		loggingIn.value = false
	}
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 0 32px 48px;
	box-sizing: border-box;
	background: linear-gradient(180deg, #f0f6ff 0%, #f5f5f5 45%);
}

.page-header {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	z-index: 10;
	box-sizing: border-box;
}

.back-btn {
	display: inline-flex;
	align-items: center;
	height: 44px;
	padding: 0 12px;
}

.back-glyph {
	font-size: 28px;
	font-weight: 300;
	line-height: 1;
	margin-top: -2px;
	color: #007aff;
}

.back-text {
	margin-left: 2px;
	font-size: 16px;
	color: #007aff;
}

.brand {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: calc(22vh + 44px);
}

.brand-logo-wrap {
	width: 96px;
	height: 96px;
	border-radius: 22px;
	overflow: hidden;
	background-color: #ffffff;
	box-shadow: 0 8px 24px rgba(0, 122, 255, 0.12);
}

.brand-logo {
	width: 100%;
	height: 100%;
	display: block;
}

.brand-title {
	margin-top: 20px;
	font-size: 22px;
	font-weight: 600;
	color: #1a1a1a;
}

.brand-desc {
	margin-top: 10px;
	font-size: 14px;
	color: #888888;
}

.actions {
	width: 100%;
	max-width: 320px;
	margin-top: 48px;
}

.wx-login-btn {
	width: 100%;
	height: 48px;
	line-height: 48px;
	font-size: 16px;
	font-weight: 500;
	color: #ffffff;
	background-color: #07c160;
	border-radius: 24px;
	border: none;
}

.wx-login-btn::after {
	border: none;
}

.wx-login-btn[disabled] {
	opacity: 0.55;
	color: #ffffff;
	background-color: #9ad9b9;
}

.privacy-row {
	margin-top: 16px;
	width: 100%;
}

.privacy-label {
	display: flex;
	flex-direction: row;
	align-items: flex-start;
	gap: 8px;
}

.privacy-check-box {
	width: 16px;
	height: 16px;
	margin-top: 2px;
	flex-shrink: 0;
	border: 1px solid #cccccc;
	border-radius: 3px;
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: center;
	background-color: #ffffff;
}

.privacy-check-box--checked {
	background-color: #07c160;
	border-color: #07c160;
}

.privacy-check-mark {
	font-size: 11px;
	line-height: 1;
	color: #ffffff;
	font-weight: 600;
}

.privacy-text {
	flex: 1;
	font-size: 13px;
	color: #888888;
	line-height: 20px;
}

.privacy-link {
	color: #007aff;
	font-size: 13px;
}

.error-text {
	display: block;
	margin-top: 12px;
	font-size: 13px;
	color: #e64340;
	text-align: center;
	line-height: 1.5;
}
</style>
