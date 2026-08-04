import { hasPayPasswordSet } from '@/services/user-security.js'

const SET_PASSWORD_URL = '/pages/mine/set-password'

/**
 * 支付前校验：未设置密码则弹窗引导，阻止支付
 * @returns {Promise<{ ok: boolean, navigatedToSet?: boolean }>}
 *   ok=true 可继续；ok=false 且 navigatedToSet=true 表示已跳转设密页；否则为用户取消
 */
export function guardPayPassword() {
	if (hasPayPasswordSet()) {
		return Promise.resolve({ ok: true })
	}
	return new Promise((resolve) => {
		uni.showModal({
			title: '请先设置支付密码',
			content: '为保障资金安全，支付前需设置 6 位数字支付密码',
			confirmText: '去设置',
			cancelText: '取消',
			success: (res) => {
				if (res.confirm) {
					// 等 modal 关闭后再跳转，避免 navigateTo:fail timeout
					setTimeout(() => {
						uni.navigateTo({
							url: `${SET_PASSWORD_URL}?from=pay`,
							animationType: 'slide-in-right',
							animationDuration: 200,
							fail: () => {
								uni.showToast({ title: '打开设密页失败，请从「我的」重试', icon: 'none' })
							}
						})
					}, 80)
					resolve({ ok: false, navigatedToSet: true })
				} else {
					resolve({ ok: false, navigatedToSet: false })
				}
			},
			fail: () => resolve({ ok: false, navigatedToSet: false })
		})
	})
}

/**
 * 包装支付流程：先校验密码，再执行 payAction
 * @param {() => Promise<{ ok: boolean, msg?: string }>} payAction
 */
export async function runPayment(payAction) {
	const guard = await guardPayPassword()
	if (!guard.ok) {
		return { ok: false, msg: '请先设置支付密码' }
	}
	return payAction()
}
