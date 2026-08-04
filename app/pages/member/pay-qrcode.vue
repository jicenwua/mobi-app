<template>
	<view class="page" :class="isDark ? 'page--dark' : 'page--light'">
		<pay-pin-pad
			v-if="showPinPad"
			title="请输入支付密码"
			:error-hint="pinError"
			:reset-key="pinResetKey"
			:busy="pinVerifying"
			@complete="onPinComplete"
			@cancel="onPinCancel"
		/>
		<view v-else class="card">
			<text class="title">向店员出示付款码</text>
			<text class="subtitle">扫码后将从店员所在店铺积分余额扣款</text>
			<view v-if="loading" class="state-wrap">
				<text class="state-text">生成中…</text>
			</view>
			<view v-else-if="qrContent" class="qr-wrap">
				<QrcodeCanvas :text="qrContent" :size="220" canvas-id="pay-qrcode" />
				<text class="expire-text">{{ expireHint }}</text>
			</view>
			<view v-else class="state-wrap">
				<text class="state-text">{{ errorMsg || '无法生成付款码' }}</text>
			</view>
			<view class="refresh-btn" :class="{ disabled: loading }" @click="refresh">
				<text class="refresh-text">刷新付款码</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow, onHide, onUnload } from '@dcloudio/uni-app'
import { useExpiringQrcode } from '@/composables/use-expiring-qrcode.js'
import { generatePayQrcode } from '@/api/modules/qrcode.js'
import { verifyPayPassword } from '@/api/modules/user.js'
import { guardPayPassword } from '@/services/payment.js'
import { canShowMemberTab } from '@/utils/wx-perm.js'
import { hasPayPasswordSet } from '@/services/user-security.js'
import { navigateBackDelayed } from '@/utils/navigation.js'
import { useTheme } from '@/composables/use-theme.js'
import QrcodeCanvas from '@/components/common/qrcode-canvas.vue'
import PayPinPad from '@/components/pay/pay-pin-pad.vue'

const MAX_PIN_ERRORS = 3

const { isDark, loadTheme } = useTheme()
const qrContent = ref('')
const loading = ref(false)
const errorMsg = ref('')
const showPinPad = ref(false)
const pinError = ref('')
const pinResetKey = ref(0)
const pinVerifying = ref(false)
const passwordVerified = ref(false)
let passwordGateDone = false
let skipPinOnce = false
let pinErrorCount = 0

const {
	countdownLeft,
	expireAt,
	stopCountdown,
	syncFromExpireAt: syncCountdownFromExpireAt
} = useExpiringQrcode(() => {
	void refresh()
})

const expireHint = computed(() => {
	if (!qrContent.value || countdownLeft.value <= 0) return ''
	return `${countdownLeft.value} 秒后自动刷新`
})

onLoad(() => {
	if (!canShowMemberTab()) {
		uni.showToast({ title: '无付款码权限', icon: 'none' })
		navigateBackDelayed(800)
		return
	}
	loadTheme()
	startPayQrcode()
})

onShow(() => {
	if (!passwordGateDone) return
	// 刚从设密页返回：已设置密码，跳过再次输入，直接生成付款码
	if (skipPinOnce && hasPayPasswordSet()) {
		skipPinOnce = false
		passwordVerified.value = true
		showPinPad.value = false
		beginQrcodeRefresh()
		return
	}
	if (passwordVerified.value && qrContent.value) {
		syncCountdownFromExpireAt(expireAt.value)
	}
})

onHide(() => {
	stopCountdown()
})

onUnload(() => {
	stopCountdown()
})

function openPinPad() {
	pinError.value = ''
	pinErrorCount = 0
	showPinPad.value = true
}

async function startPayQrcode() {
	const guard = await guardPayPassword()
	passwordGateDone = true
	if (!guard.ok) {
		if (guard.navigatedToSet) {
			skipPinOnce = true
		} else {
			uni.navigateBack()
		}
		return
	}
	openPinPad()
}

async function onPinComplete(digits) {
	if (!/^\d{6}$/.test(digits)) {
		recordPinError('请输入 6 位数字')
		return
	}
	pinVerifying.value = true
	const res = await verifyPayPassword(digits)
	pinVerifying.value = false
	if (!res.ok) {
		if (res.type === 'forbidden') {
			showPinPad.value = false
			uni.showModal({
				title: '无法校验密码',
				content: res.msg || '当前账号无权限校验支付密码，请联系管理员',
				showCancel: false,
				success: () => uni.navigateBack(),
				fail: () => uni.navigateBack()
			})
			return
		}
		if (res.type === 'network') {
			showPinPad.value = false
			uni.showToast({ title: res.msg || '网络错误', icon: 'none' })
			return
		}
		recordPinError(res.msg || '支付密码错误')
		return
	}
	passwordVerified.value = true
	showPinPad.value = false
	await beginQrcodeRefresh()
}

function recordPinError(msg) {
	pinErrorCount += 1
	if (pinErrorCount >= MAX_PIN_ERRORS) {
		showPinPad.value = false
		uni.showModal({
			title: '提示',
			content: '错误次数过多，请稍后再试',
			showCancel: false,
			success: () => uni.navigateBack(),
			fail: () => uni.navigateBack()
		})
		return
	}
	const left = MAX_PIN_ERRORS - pinErrorCount
	pinResetKey.value += 1
	pinError.value = `${msg}（还可尝试 ${left} 次）`
}

function onPinCancel() {
	uni.navigateBack()
}

async function beginQrcodeRefresh() {
	stopCountdown()
	await refresh()
}

async function refresh() {
	if (loading.value || !passwordVerified.value) return
	loading.value = true
	errorMsg.value = ''
	const res = await generatePayQrcode()
	loading.value = false
	if (!res.ok || !res.data?.qrContent) {
		errorMsg.value = res.msg || '生成失败'
		qrContent.value = ''
		countdownLeft.value = 0
		stopCountdown()
		return
	}
	qrContent.value = res.data.qrContent
	expireAt.value = res.data.expireAt || Date.now() + 120000
	syncCountdownFromExpireAt(expireAt.value)
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	box-sizing: border-box;
}

.page--light {
	background: #f5f5f5;
	color: #333;
}

.page--dark {
	background: #121212;
	color: #e8e8e8;
}

.card {
	padding: 24px 16px;
	text-align: center;
}

.page--light .card {
	background: #f5f5f5;
}

.page--dark .card {
	background: #121212;
}

.title {
	display: block;
	font-size: 18px;
	font-weight: 600;
	margin-bottom: 6px;
}

.subtitle {
	display: block;
	font-size: 13px;
	opacity: 0.55;
	margin-bottom: 20px;
}

.qr-wrap {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 12px;
}

.expire-text {
	font-size: 12px;
	opacity: 0.5;
}

.state-wrap {
	padding: 40px 0;
}

.state-text {
	font-size: 14px;
	opacity: 0.5;
}

.refresh-btn {
	margin-top: 20px;
	padding: 10px 20px;
	border-radius: 8px;
	background: #007aff;
	display: inline-block;
}

.refresh-btn.disabled {
	opacity: 0.6;
	pointer-events: none;
}

.refresh-text {
	color: #fff;
	font-size: 14px;
}
</style>
