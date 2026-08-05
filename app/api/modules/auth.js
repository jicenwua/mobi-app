import { request } from '@/api/http/client.js'
import { CUSTOMER_API } from '@/api/constants/customer.js'
import {
	isApiSuccess,
	parseLoginData,
	pickApiData,
	unwrapResponseBody,
	isUnauthorizedResponse
} from '@/utils/api-response.js'
import {
	isDefaultAvatarUrl,
	isEmptyAvatar,
	resolveAvatarUrl
} from '@/utils/avatar.js'
import {
	getToken,
	setToken,
	clearAuthSession,
	setSessionPermissions,
	applyServerSetPasswordFlag
} from '@/api/modules/auth-token.js'
import { disconnectNotifySocket } from '@/services/notify-socket.js'

export { getToken, setToken, clearAuthSession }

const WX_LOGIN_TIMEOUT_MS = 15000

/** 将登录/用户信息中的 token、权限、密码状态写入本地 */
function applyAuthFromLoginData(login) {
	if (login.token) {
		setToken(login.token)
	}
	setSessionPermissions(login.permission)
	applyServerSetPasswordFlag(login.setPassword)
}

function buildLoginResult(ok, login, body) {
	const avatarUrl = resolveAvatarUrl(login.avatar)
	return {
		ok,
		token: login.token || '',
		register: !!login.register,
		nickname: login.nickName,
		avatarUrl,
		setPassword: login.setPassword,
		usesDefaultAvatar: isEmptyAvatar(login.avatar) || isDefaultAvatarUrl(avatarUrl),
		msg: body?.msg || (ok ? '登录成功' : '登录失败')
	}
}

/** 调用 uni.login 获取微信 code */
function wxLoginCode() {
	return new Promise((resolve, reject) => {
		let settled = false
		const finish = (fn, arg) => {
			if (settled) return
			settled = true
			clearTimeout(timer)
			fn(arg)
		}
		const timer = setTimeout(() => {
			finish(reject, new Error('uni.login 超时，请检查 AppID 与网络'))
		}, WX_LOGIN_TIMEOUT_MS)
		const handlers = {
			success: (res) => {
				if (res.code) finish(resolve, res.code)
				else finish(reject, new Error('未获取到微信 code'))
			},
			fail: (err) => finish(reject, new Error(err.errMsg || 'uni.login 失败'))
		}
		// #ifdef MP-WEIXIN
		uni.login(handlers)
		// #endif
		// #ifndef MP-WEIXIN
		uni.login({ ...handlers, provider: 'weixin' })
		// #endif
	})
}

/**
 * 使用本地 token 拉取当前用户信息 GET /app/info（会刷新本地 token）
 */
export function fetchCurrentUserInfo() {
	return new Promise((resolve) => {
		if (!getToken()) {
			resolve({ ok: false, msg: '未登录' })
			return
		}
		request({
			service: 'customer',
			path: CUSTOMER_API.USER_INFO,
			method: 'GET',
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				if (isUnauthorizedResponse(res, body)) {
					clearAuthSession()
					disconnectNotifySocket()
					resolve({ ok: false, unauthorized: true, msg: body.msg || '登录已失效' })
					return
				}
				const ok = res.statusCode === 200 && isApiSuccess(body)
				const login = parseLoginData(pickApiData(body))
				const avatarUrl = resolveAvatarUrl(login.avatar)
				if (ok) {
					applyAuthFromLoginData(login)
				}
				resolve({
					ok,
					nickname: login.nickName,
					avatarUrl,
					setPassword: login.setPassword,
					usesDefaultAvatar:
						isEmptyAvatar(login.avatar) || isDefaultAvatarUrl(avatarUrl),
					msg: body.msg || (ok ? '获取成功' : '获取用户信息失败')
				})
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

/**
 * 微信登录 POST /app/login（JSON：code + 可选 phoneCode）
 * @param {{ phoneCode?: string }} params
 */
export function wxLogin({ phoneCode } = {}) {
	return new Promise(async (resolve) => {
		try {
			const code = await wxLoginCode()
			const data = { code }
			const phone = (phoneCode || '').trim()
			if (phone) data.phoneCode = phone

			request({
				service: 'customer',
				path: CUSTOMER_API.LOGIN,
				method: 'POST',
				data,
				timeout: 60000,
				success: (res) => {
					const body = unwrapResponseBody(res.data)
					const ok = res.statusCode === 200 && isApiSuccess(body)
					const login = parseLoginData(pickApiData(body))
					if (ok) {
						applyAuthFromLoginData(login)
					}
					resolve(buildLoginResult(ok, login, body))
				},
				fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
			})
		} catch (e) {
			resolve({ ok: false, msg: e.message || '登录失败' })
		}
	})
}
