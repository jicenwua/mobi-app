/**
 * 生成幂等请求 ID（UUID v4），用于防重复提交。
 */
export function createRequestId() {
	const template = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'
	return template.replace(/[xy]/g, (char) => {
		const rand = (Math.random() * 16) | 0
		const value = char === 'x' ? rand : (rand & 0x3) | 0x8
		return value.toString(16)
	})
}
