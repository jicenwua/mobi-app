/**
 * 解析扫码结果中的店铺代码
 * @param {string} raw
 * @returns {string}
 */
export function parseShopCodeFromScan(raw) {
	const text = (raw || '').trim()
	if (!text) return ''
	const inviteMatch = text.match(/^MOBI:SHOP:INV:(.+)$/i)
	if (inviteMatch) return ''
	const mobiMatch = text.match(/^MOBI:SHOP:(.+)$/i)
	if (mobiMatch) return mobiMatch[1].trim()
	if (/^[A-Za-z0-9]{6,16}$/.test(text)) return text
	return text
}

/**
 * 解析扫码结果中的店铺邀请 token
 * @param {string} raw
 * @returns {string}
 */
export function parseShopInviteFromScan(raw) {
	const text = (raw || '').trim()
	if (!text) return ''
	const match = text.match(/^MOBI:SHOP:INV:(.+)$/i)
	return match ? match[1].trim() : ''
}

/**
 * 解析扫码结果中的付款码 token
 * @param {string} raw
 * @returns {string}
 */
export function parsePayTokenFromScan(raw) {
	const text = (raw || '').trim()
	const match = text.match(/^MOBI:PAY:(.+)$/i)
	return match ? match[1].trim() : ''
}

/**
 * 解析扫码结果中的订单核销码 token
 * @param {string} raw
 * @returns {string}
 */
export function parseOrderTokenFromScan(raw) {
	const text = (raw || '').trim()
	const match = text.match(/^MOBI:ORDER:(.+)$/i)
	return match ? match[1].trim() : ''
}

/**
 * 解析扫码结果中的店员邀请 token
 * @param {string} raw
 * @returns {string}
 */
export function parseStaffInviteTokenFromScan(raw) {
	const text = (raw || '').trim()
	const match = text.match(/^MOBI:STAFF:INV:(.+)$/i)
	return match ? match[1].trim() : ''
}

/**
 * 解析小程序启动 scene / query 中的店铺邀请或店铺代码
 * @param {Record<string, string>|undefined} options
 * @returns {{ token?: string, shopCode?: string }|null}
 */
export function parseLaunchShopInvite(options) {
	if (!options) return null
	if (options.inviteToken) {
		return { token: String(options.inviteToken).trim() }
	}
	if (options.shopCode) {
		return { shopCode: String(options.shopCode).trim() }
	}
	if (options.scene) {
		const scene = decodeURIComponent(String(options.scene))
		if (scene.startsWith('t') && scene.length > 1) {
			return { token: scene.slice(1).trim() }
		}
		if (scene.startsWith('s=')) {
			return { shopCode: scene.slice(2).trim() }
		}
		return { shopCode: scene.trim() }
	}
	return null
}

const PENDING_SHOP_CODE_KEY = 'pending_shop_code'
const PENDING_SHOP_INVITE_KEY = 'pending_shop_invite'

export function savePendingShopCode(code) {
	if (!code) return
	try {
		uni.setStorageSync(PENDING_SHOP_CODE_KEY, code)
	} catch {
		/* ignore */
	}
}

export function takePendingShopCode() {
	try {
		const code = uni.getStorageSync(PENDING_SHOP_CODE_KEY)
		if (code) uni.removeStorageSync(PENDING_SHOP_CODE_KEY)
		return code ? String(code).trim() : ''
	} catch {
		return ''
	}
}

export function savePendingShopInvite(token) {
	if (!token) return
	try {
		uni.setStorageSync(PENDING_SHOP_INVITE_KEY, token)
	} catch {
		/* ignore */
	}
}

export function takePendingShopInvite() {
	try {
		const token = uni.getStorageSync(PENDING_SHOP_INVITE_KEY)
		if (token) uni.removeStorageSync(PENDING_SHOP_INVITE_KEY)
		return token ? String(token).trim() : ''
	} catch {
		return ''
	}
}

export function buildShopQrContent(shopCode) {
	return `MOBI:SHOP:${shopCode}`
}

export function buildShopInviteQrContent(token) {
	return `MOBI:SHOP:INV:${token}`
}
