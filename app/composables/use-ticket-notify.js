import { onUnload } from '@dcloudio/uni-app'
import { onNotifyMessage } from '@/services/notify-socket.js'

/**
 * 工单 WebSocket 推送订阅，页面卸载时自动取消
 * @param {(payload: object) => void} handler
 */
export function useTicketNotify(handler) {
	let offNotify = null

	function subscribe() {
		offNotify?.()
		offNotify = onNotifyMessage(handler)
	}

	function unsubscribe() {
		offNotify?.()
		offNotify = null
	}

	onUnload(unsubscribe)

	return { subscribe, unsubscribe }
}
