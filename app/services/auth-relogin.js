import { clearAuthSession, getToken } from '@/api/modules/auth-token.js'
import { disconnectNotifySocket } from '@/services/notify-socket.js'
import { getPrivacySettingState } from '@/utils/wx-privacy.js'

const LOGIN_PAGE = '/pages/login/login'

let autoLoginPromise = null
let quickLoginHandler = null
let resetSessionHandler = null
let fetchUserInfoHandler = null
let permissionSyncPromise = null

/**
 * 在 main.js 注册，避免 auth-relogin ↔ client ↔ auth 循环依赖
 * （微信小程序不支持 dynamic import 命名导出）
 */
export function configureAuthRelogin({
	performQuickLogin,
	resetAppSession,
	fetchCurrentUserInfo
}) {
	quickLoginHandler = performQuickLogin
	resetSessionHandler = resetAppSession
	fetchUserInfoHandler = fetchCurrentUserInfo
}

/** 是否已同意微信隐私指引（非首次进入无需再勾选） */
export async function canAutoLogin() {
	const state = await getPrivacySettingState()
	return !state.needAuthorization
}

/**
 * 静默自动登录（需已同意隐私指引）
 * @returns {Promise<{ ok: boolean, needPrivacy?: boolean, msg?: string, token?: string }>}
 */
export async function performAutoLogin() {
	if (autoLoginPromise) {
		return autoLoginPromise
	}

	autoLoginPromise = (async () => {
		if (!quickLoginHandler) {
			return { ok: false, msg: '登录模块未就绪' }
		}

		const allowed = await canAutoLogin()
		if (!allowed) {
			return { ok: false, needPrivacy: true, msg: '请先同意隐私保护指引' }
		}

		const result = await quickLoginHandler()
		if (result.ok && resetSessionHandler) {
			resetSessionHandler()
		}
		return result
	})().finally(() => {
		autoLoginPromise = null
	})

	return autoLoginPromise
}

/** 401 等会话失效：清 token 后静默重登 */
export async function handleSessionUnauthorized() {
	clearAuthSession()
	disconnectNotifySocket()
	if (resetSessionHandler) {
		resetSessionHandler()
	}
	return performAutoLogin()
}

function isOnLoginPage() {
	try {
		const pages = getCurrentPages()
		const current = pages[pages.length - 1]
		const route = current?.route || ''
		return route.includes('pages/login/login')
	} catch {
		return false
	}
}

/** token 已刷新但响应头未带权限时，拉取 /app/info 同步 permission */
export async function syncPermissionsAfterTokenRefresh() {
	if (!fetchUserInfoHandler) return
	if (permissionSyncPromise) {
		return permissionSyncPromise
	}
	permissionSyncPromise = fetchUserInfoHandler().finally(() => {
		permissionSyncPromise = null
	})
	return permissionSyncPromise
}

/** 无法自动登录时跳转登录页（已在登录页则跳过） */
export function redirectToLoginIfNeeded(loginResult) {
	if (loginResult?.ok) return
	if (isOnLoginPage()) return
	uni.reLaunch({ url: LOGIN_PAGE })
}

/** 本地无 token 时尝试静默登录，失败则跳转登录页 */
export async function ensureAuthenticated() {
	if (getToken()) {
		return { ok: true }
	}
	const result = await performAutoLogin()
	if (!result.ok) {
		redirectToLoginIfNeeded(result)
	}
	return result
}
