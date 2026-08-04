import { ref } from 'vue'

/**
 * 防抖执行（搜索输入等）
 * @param {(...args: unknown[]) => void} fn
 * @param {number} delayMs
 */
export function useDebounceFn(fn, delayMs = 300) {
	let timer = null

	function cancel() {
		if (timer) {
			clearTimeout(timer)
			timer = null
		}
	}

	function run(...args) {
		cancel()
		timer = setTimeout(() => {
			timer = null
			fn(...args)
		}, delayMs)
	}

	return { run, cancel }
}
