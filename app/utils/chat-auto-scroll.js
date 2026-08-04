import { nextTick, ref } from 'vue'

const DEFAULT_THRESHOLD = 80

/**
 * 小程序 scroll-view 聊天自动滚底。
 * 默认跟随最新消息；用户手动上滑时不打断。
 */
export function useMpChatAutoScroll(options = {}) {
	const threshold = options.threshold ?? DEFAULT_THRESHOLD
	const scrollTop = ref(0)
	const stickToBottom = ref(true)

	let viewHeight = 0
	let programmaticScroll = false
	let scrollToken = 0

	function handleScroll(e) {
		if (programmaticScroll) {
			return
		}
		const { scrollTop: top = 0, scrollHeight = 0 } = e.detail || {}
		if (viewHeight <= 0) {
			stickToBottom.value = true
			return
		}
		stickToBottom.value = scrollHeight - top - viewHeight <= threshold
	}

	function measure(context, scrollSelector = '.message-scroll', listSelector = '.message-list') {
		return new Promise((resolve) => {
			if (!context) {
				resolve({ viewHeight: 0, listHeight: 0 })
				return
			}
			const query = uni.createSelectorQuery().in(context)
			query.select(scrollSelector).boundingClientRect()
			query.select(listSelector).boundingClientRect()
			query.exec((res) => {
				viewHeight = res?.[0]?.height || 0
				resolve({
					viewHeight,
					listHeight: res?.[1]?.height || 0
				})
			})
		})
	}

	function applyScrollTop(target) {
		const normalized = Math.max(0, Math.ceil(target))
		scrollTop.value = scrollTop.value === normalized ? normalized + 1 : normalized
	}

	async function scrollToBottom(force = false, context) {
		if (!force && !stickToBottom.value) {
			return
		}

		programmaticScroll = true
		stickToBottom.value = true
		const token = ++scrollToken

		const attempt = async (delay = 0) => {
			if (token !== scrollToken) {
				return
			}
			if (delay > 0) {
				await new Promise((resolve) => setTimeout(resolve, delay))
			}
			await nextTick()

			const { viewHeight: measuredViewHeight, listHeight } = await measure(context)
			if (measuredViewHeight > 0 && listHeight > 0) {
				applyScrollTop(listHeight - measuredViewHeight + 24)
				return
			}
			// 测量失败时用极大值兜底，scroll-view 会自动滚到最底
			applyScrollTop(999999)
		}

		await attempt(0)
		await attempt(100)
		await attempt(280)

		setTimeout(() => {
			if (token === scrollToken) {
				programmaticScroll = false
			}
		}, 500)
	}

	return {
		scrollTop,
		stickToBottom,
		handleScroll,
		measure,
		scrollToBottom
	}
}
