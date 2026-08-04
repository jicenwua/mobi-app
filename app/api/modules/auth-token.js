import {
	clearSessionPermissions,
	setSessionPermissions,
	normalizePermissions
} from '@/utils/permissions.js'
import {
	applyServerSetPasswordFlag,
	clearUserSecurity
} from '@/services/user-security.js'

const TOKEN_KEY = 'auth_token'

/** 规范化 token（去掉 Bearer 前缀与首尾空格） */
export function normalizeToken(raw) {
	if (raw == null || raw === '') return ''
	const value = String(raw).trim()
	if (/^bearer\s+/i.test(value)) {
		return value.replace(/^bearer\s+/i, '').trim()
	}
	return value
}

/** 读取本地登录令牌 */
export function getToken() {
	try {
		return normalizeToken(uni.getStorageSync(TOKEN_KEY))
	} catch {
		return ''
	}
}

/** 写入或清除登录令牌；若与旧值不同返回 true */
export function setToken(token) {
	const normalized = normalizeToken(token)
	if (normalized) {
		const previous = getToken()
		uni.setStorageSync(TOKEN_KEY, normalized)
		return previous !== normalized
	}
	uni.removeStorageSync(TOKEN_KEY)
	clearSessionPermissions()
	clearUserSecurity()
	return false
}

/** 清除登录态（token + 会话权限） */
export function clearAuthSession() {
	setToken('')
}

/**
 * 从 uni.request 响应头读取后端续签/权限刷新后的 token
 * （HeaderAuthenticationFilter 写入 authorization）
 */
export function extractTokenFromResponse(res) {
	const headers = res?.header || res?.headers || {}
	const raw = headers.authorization || headers.Authorization || ''
	return normalizeToken(raw)
}

/** 响应头含新 token 时同步到本地 */
export function applyResponseToken(res) {
	const newToken = extractTokenFromResponse(res)
	if (!newToken) return false
	return setToken(newToken)
}

/**
 * 从响应头读取权限变更（HeaderAuthenticationFilter 在权限刷新时写入 role_permission）
 * @returns {object | array | null}
 */
export function extractPermissionsFromResponse(res) {
	const headers = res?.header || res?.headers || {}
	const raw =
		headers.role_permission ||
		headers['role-permission'] ||
		headers.Role_Permission ||
		''
	if (!raw) return null
	if (typeof raw === 'object') return raw
	try {
		return JSON.parse(raw)
	} catch {
		return null
	}
}

/**
 * 同步响应头中的 token / 权限（权限版本刷新时与 token 一并下发）
 * @returns {{ tokenChanged: boolean, permChanged: boolean }}
 */
export function applyResponseSession(res) {
	const tokenChanged = applyResponseToken(res)
	const permissions = extractPermissionsFromResponse(res)
	let permChanged = false
	if (permissions != null) {
		setSessionPermissions(permissions)
		permChanged = true
	}
	return { tokenChanged, permChanged }
}

export { normalizePermissions, setSessionPermissions, applyServerSetPasswordFlag }
