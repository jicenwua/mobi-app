<template>
	<pay-pin-pad
		v-if="showPad"
		ref="pinRef"
		:title="stepTitle"
		:error-hint="pinError"
		:reset-key="pinResetKey"
		:busy="submitting"
		@complete="onPinComplete"
		@cancel="onCancel"
	/>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import PayPinPad from '@/components/pay/pay-pin-pad.vue'
import { setPayPassword } from '@/api/modules/user.js'
import { hasPayPasswordSet, setUserSecurity } from '@/services/user-security.js'
import { navigateBackDelayed } from '@/utils/navigation.js'

const showPad = ref(false)
const submitting = ref(false)
const pinRef = ref(null)
const pinError = ref('')
const pinResetKey = ref(0)
const isModify = ref(false)
const stepIndex = ref(0)
const oldPassword = ref('')
const newPassword = ref('')
let fromPay = false

const MAX_PIN_ERRORS = 3
const errorCount = ref(0)

const steps = computed(() =>
	isModify.value ? ['old', 'new', 'confirm'] : ['new', 'confirm']
)

const stepTitle = computed(() => {
	const step = steps.value[stepIndex.value]
	if (step === 'old') return '请输入原支付密码'
	if (step === 'new') return isModify.value ? '请输入新支付密码' : '请设置支付密码'
	return '请再次输入确认'
})

onLoad((query) => {
	fromPay = query?.from === 'pay'
	isModify.value = hasPayPasswordSet()
	if (fromPay && !isModify.value) {
		stepIndex.value = 0
		errorCount.value = 0
		pinError.value = ''
		showPad.value = true
		return
	}
	promptStart()
})

function promptStart() {
	const title = isModify.value ? '修改支付密码' : '设置支付密码'
	const content = isModify.value
		? '是否修改支付密码？修改后请使用新密码支付。'
		: '是否设置 6 位数字支付密码？设置后可进行支付。'
	uni.showModal({
		title,
		content,
		confirmText: '确定',
		cancelText: '取消',
		success: (res) => {
			if (res.confirm) {
				stepIndex.value = 0
				errorCount.value = 0
				pinError.value = ''
				showPad.value = true
			} else {
				uni.navigateBack()
			}
		},
		fail: () => uni.navigateBack()
	})
}

/** 清空六位输入格并可选展示错误提示 */
function resetPinInput(errorMsg = '') {
	pinResetKey.value += 1
	pinRef.value?.reset()
	pinError.value = errorMsg
}

/** 错误次数过多，退出设置页 */
function exitAfterTooManyErrors() {
	showPad.value = false
	uni.showModal({
		title: '提示',
		content: '错误次数过多，请稍后再试',
		showCancel: false,
		confirmText: '知道了',
		success: () => uni.navigateBack(),
		fail: () => uni.navigateBack()
	})
}

/**
 * 记录一次输入/校验错误；满 3 次则退出
 * @returns {boolean} 是否可继续输入
 */
function recordPinError(msg) {
	errorCount.value += 1
	if (errorCount.value >= MAX_PIN_ERRORS) {
		exitAfterTooManyErrors()
		return false
	}
	const left = MAX_PIN_ERRORS - errorCount.value
	const hint = left > 0 ? `${msg}（还可尝试 ${left} 次）` : msg
	resetPinInput(hint)
	return true
}

function nextStep() {
	resetPinInput('')
	if (stepIndex.value < steps.value.length - 1) {
		stepIndex.value += 1
	} else {
		submitPassword()
	}
}

async function onPinComplete(digits) {
	if (!/^\d{6}$/.test(digits)) {
		recordPinError('请输入 6 位数字')
		return
	}
	const step = steps.value[stepIndex.value]
	if (step === 'old') {
		oldPassword.value = digits
		nextStep()
		return
	}
	if (step === 'new') {
		if (isModify.value && digits === oldPassword.value) {
			recordPinError('新密码不能与原密码相同')
			return
		}
		newPassword.value = digits
		nextStep()
		return
	}
	if (digits !== newPassword.value) {
		// 确认不一致：清空输入格，留在确认步骤重新输入
		await nextTick()
		recordPinError('两次密码不一致，请重新输入')
		return
	}
	submitPassword()
}

async function submitPassword() {
	if (submitting.value) return
	submitting.value = true
	try {
		const result = await setPayPassword({
			oldPassword: isModify.value ? oldPassword.value : undefined,
			newPassword: newPassword.value
		})
		if (!result.ok) {
			const msg = result.msg || '设置失败'
			if (msg.includes('原密码')) {
				stepIndex.value = 0
				oldPassword.value = ''
				newPassword.value = ''
			}
			recordPinError(msg)
			return
		}
		setUserSecurity({ hasSetPassword: true })
		uni.showToast({ title: isModify.value ? '修改成功' : '设置成功', icon: 'success' })
		navigateBackDelayed(350)
	} finally {
		submitting.value = false
	}
}

function onCancel() {
	uni.navigateBack()
}
</script>
