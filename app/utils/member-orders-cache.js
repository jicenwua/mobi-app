const TAB_KEYS = ['unused', 'verified']

function emptySnapshot() {
	return { loaded: false, rows: [], finished: false, pageNum: 1 }
}

/** @type {Record<string, { loaded: boolean, rows: object[], finished: boolean, pageNum: number }>} */
const store = {
	unused: emptySnapshot(),
	verified: emptySnapshot()
}

/** 缓存版本号：invalidate 时递增，供列表组件判断是否需要重新拉取 */
let cacheEpoch = 0

export function getMemberOrdersCacheEpoch() {
	return cacheEpoch
}

export function isMemberOrdersCacheReady(tabKey) {
	return !!store[tabKey]?.loaded
}

export function getMemberOrdersCache(tabKey) {
	const cached = store[tabKey]
	if (!cached?.loaded) return null
	return {
		rows: [...cached.rows],
		finished: cached.finished,
		pageNum: cached.pageNum
	}
}

export function setMemberOrdersCache(tabKey, { rows, finished, pageNum }) {
	if (!store[tabKey]) return
	store[tabKey] = {
		loaded: true,
		rows: Array.isArray(rows) ? [...rows] : [],
		finished: !!finished,
		pageNum: Number(pageNum) > 0 ? Number(pageNum) : 1
	}
}

/**
 * 清除订单列表缓存（购买、核销、退款等场景调用）。
 * @param {string} [tabKey] 不传则清除全部 Tab
 */
export function invalidateMemberOrdersCache(tabKey) {
	cacheEpoch += 1
	if (tabKey) {
		store[tabKey] = emptySnapshot()
		return
	}
	for (const key of TAB_KEYS) {
		store[key] = emptySnapshot()
	}
}
