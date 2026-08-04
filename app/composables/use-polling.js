import { onHide, onShow, onUnload } from '@dcloudio/uni-app'

/**
 * 页面级定时轮询：onHide 暂停、onShow 恢复、onUnload 清理。
 * @param {() => boolean} shouldPoll 是否应继续轮询
 * @param {() => void | Promise<void>} tick 单次轮询逻辑
 * @param {number} intervalMs 间隔毫秒
 */
export function usePolling(shouldPoll, tick, intervalMs) {
	let timer = null

	function stop() {
		if (timer) {
			clearInterval(timer)
			timer = null
		}
	}

	function start() {
		stop()
		if (!shouldPoll()) return
		timer = setInterval(() => {
			if (!shouldPoll()) {
				stop()
				return
			}
			void tick()
		}, intervalMs)
	}

	onShow(() => start())
	onHide(() => stop())
	onUnload(() => stop())

	return { start, stop }
}
