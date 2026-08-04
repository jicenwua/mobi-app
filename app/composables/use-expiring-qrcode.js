import { ref, onUnmounted } from 'vue'

/**
 * 二维码/邀请码过期倒计时，到期后触发 onExpire
 * @param {() => void | Promise<void>} onExpire
 */
export function useExpiringQrcode(onExpire) {
	const countdownLeft = ref(0)
	const expireAt = ref(0)
	let tickTimer = null

	function stopCountdown() {
		if (tickTimer) {
			clearInterval(tickTimer)
			tickTimer = null
		}
	}

	function startCountdown(totalSeconds) {
		stopCountdown()
		const seconds = Math.max(1, Math.floor(totalSeconds))
		countdownLeft.value = seconds
		tickTimer = setInterval(() => {
			if (countdownLeft.value <= 1) {
				countdownLeft.value = 0
				stopCountdown()
				void onExpire?.()
				return
			}
			countdownLeft.value -= 1
		}, 1000)
	}

	function syncFromExpireAt(at) {
		if (!at) return
		expireAt.value = at
		const left = Math.ceil((at - Date.now()) / 1000)
		if (left > 0) {
			startCountdown(left)
		} else {
			countdownLeft.value = 0
		}
	}

	onUnmounted(stopCountdown)

	return {
		countdownLeft,
		expireAt,
		stopCountdown,
		startCountdown,
		syncFromExpireAt
	}
}
