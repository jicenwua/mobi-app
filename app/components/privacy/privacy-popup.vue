<template>
	<view
		v-if="privacyPopupVisible"
		class="privacy-mask"
		@touchmove.stop.prevent
	>
		<view class="privacy-panel" @click.stop>
			<text class="privacy-title">用户隐私保护提示</text>
			<view class="privacy-body">
				<text class="privacy-text">
					感谢使用尚品发艺会员。在使用本小程序服务前，请您仔细阅读并充分理解
				</text>
				<text class="privacy-link" @click="openPrivacyContract">{{ contractLabel }}</text>
				<text class="privacy-text">
					。点击「同意并继续」即表示您已阅读并同意该指引；我们将按照指引收集、使用和保护您的个人信息（如微信头像、昵称等）。
				</text>
			</view>
			<view class="privacy-actions">
				<button class="privacy-btn privacy-btn--ghost" @click="handleDisagree">
					拒绝
				</button>
				<button
					id="privacy-agree-btn"
					class="privacy-btn privacy-btn--primary"
					open-type="agreePrivacyAuthorization"
					@agreeprivacyauthorization="handleAgree"
				>
					同意并继续
				</button>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import {
	privacyPopupVisible,
	openPrivacyContract,
	onPrivacyAgree,
	onPrivacyDisagree,
	getPrivacySettingState,
	formatPrivacyContractLabel
} from '@/utils/wx-privacy.js'

const contractName = ref('用户隐私保护指引')
const contractLabel = computed(() => formatPrivacyContractLabel(contractName.value))

// 暴露方法给父组件调用
const showPrivacyDialog = () => {
	privacyPopupVisible.value = true
}

defineExpose({
	showPrivacyDialog
})

watch(
	privacyPopupVisible,
	(visible) => {
		if (!visible) return
		getPrivacySettingState().then((state) => {
			if (state.privacyContractName) {
				contractName.value = state.privacyContractName
			}
		})
	},
	{ immediate: true }
)

function handleAgree() {
	onPrivacyAgree()
}

function handleDisagree() {
	onPrivacyDisagree()
	uni.showModal({
		title: '提示',
		content: '您需要同意隐私政策后才能使用本小程序',
		showCancel: false,
		confirmText: '退出',
		success: () => {
			// 退出小程序
			// #ifdef MP-WEIXIN
			uni.exitMiniProgram()
			// #endif
			// #ifndef MP-WEIXIN
			uni.showToast({
				title: '需同意隐私政策后才能使用',
				icon: 'none',
				duration: 2500
			})
			// #endif
		}
	})
}
</script>

<style scoped>
.privacy-mask {
	position: fixed;
	left: 0;
	top: 0;
	right: 0;
	bottom: 0;
	z-index: 99999;
	background-color: rgba(0, 0, 0, 0.55);
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 32px 24px;
	box-sizing: border-box;
}

.privacy-panel {
	width: 100%;
	max-width: 320px;
	background-color: #ffffff;
	border-radius: 16px;
	padding: 24px 20px 20px;
	box-sizing: border-box;
}

.privacy-title {
	display: block;
	font-size: 17px;
	font-weight: 600;
	color: #1a1a1a;
	text-align: center;
	margin-bottom: 16px;
}

.privacy-body {
	margin-bottom: 24px;
	line-height: 1.65;
}

.privacy-text {
	font-size: 14px;
	color: #555555;
}

.privacy-link {
	font-size: 14px;
	color: #007aff;
}

.privacy-actions {
	display: flex;
	gap: 12px;
}

.privacy-btn {
	flex: 1;
	height: 44px;
	line-height: 44px;
	font-size: 15px;
	border-radius: 22px;
	margin: 0;
	padding: 0;
	border: none;
}

.privacy-btn::after {
	border: none;
}

.privacy-btn--ghost {
	background-color: #f5f5f5;
	color: #666666;
}

.privacy-btn--primary {
	background-color: #07c160;
	color: #ffffff;
	font-weight: 500;
}
</style>
