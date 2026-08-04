<template>
	<view class="page">
		<view class="brand">
			<view class="brand-logo-wrap">
				<image class="brand-logo" src="/static/尚品发艺.png" mode="aspectFill" />
			</view>
			<text class="brand-title">尚品发艺会员</text>
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
import { getToken } from '@/api/modules/auth.js'
import { performQuickLogin, resolvePostLoginPath } from '@/services/login-flow.js'
import { resetAppSession } from '@/services/app-session.js'
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

const privacyContractLabel = computed(() =>
	formatPrivacyContractLabel(privacyContractName.value)
)

onLoad((options) => {
	const launchInvite = parseLaunchShopInvite(options)
	if (launchInvite?.token) {
		savePendingShopInvite(launchInvite.token)
	} else if (launchInvite?.shopCode) {
		savePendingShopCode(launchInvite.shopCode)
	}
	if (!getToken()) return
	const target = hasCompletedProfileSetup()
		? '/pages/main/main'
		: '/pages/login/profile-setup'
	uni.reLaunch({ url: target })
})

/** 进入页面：同步微信隐私状态，首次进入需弹出隐私指引 */
onReady(() => {
	if (getToken()) return
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

.brand {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 22vh;
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
