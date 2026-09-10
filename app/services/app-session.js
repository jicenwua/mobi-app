import { fetchCurrentUserInfo, getToken, clearAuthSession } from '@/api/modules/auth.js'
import { performAutoLogin } from '@/services/auth-relogin.js'
import { disconnectNotifySocket } from '@/services/notify-socket.js'
import { applyLoginProfile } from '@/services/user-profile.js'
import { applyServerSetPasswordFlag } from '@/services/user-security.js'

let bootstrapPromise = null

/** 重置启动会话（登录成功后由登录页调用） */
export function resetAppSession() {
	bootstrapPromise = null
}

/**
 * 应用启动会话：有有效 token 则拉取 /app/info；否则需跳转登录页
 * @returns {Promise<{ loginResult: object, needLogin: boolean }>}
 */
export function bootstrapAppSession() {
	if (!bootstrapPromise) {
		bootstrapPromise = runBootstrap().then((result) => {
			if (!result.loginResult?.ok) {
				bootstrapPromise = null
			}
			return result
		})
	}
	return bootstrapPromise
}

function syncProfileFromInfo(info) {
	if (!info.ok) return
	if (info.setPassword !== undefined && info.setPassword !== null) {
		applyServerSetPasswordFlag(info.setPassword)
	}
	if (info.nickname || info.avatarUrl) {
		applyLoginProfile({
			nickname: info.nickname,
			avatarUrl: info.avatarUrl,
			usesDefaultAvatar: info.usesDefaultAvatar
		})
	}
}

async function restoreSessionFromToken() {
	const info = await fetchCurrentUserInfo()
	if (info.ok) {
		syncProfileFromInfo(info)
		return {
			loginResult: { ok: true, token: getToken(), msg: '已登录' },
			needLogin: false
		}
	}
	// 本地 token 已失效或服务端异常：清除会话，避免主/登录页互相跳转
	clearAuthSession()
	disconnectNotifySocket()
	if (info.serverError) {
		return {
			loginResult: { ok: false, msg: info.msg || '服务暂不可用，请稍后重试' },
			needLogin: true,
			serverError: true
		}
	}
	if (info.unauthorized) {
		return null
	}
	return null
}

async function runBootstrap() {
	const token = getToken()
	if (token) {
		const restored = await restoreSessionFromToken()
		if (restored?.loginResult?.ok) {
			return restored
		}
		// 网关/服务端 5xx 时不静默重登，避免反复请求
		if (restored?.serverError) {
			return {
				loginResult: restored.loginResult,
				needLogin: true
			}
		}
	}

	const autoResult = await performAutoLogin()
	if (autoResult.ok) {
		const info = await fetchCurrentUserInfo()
		if (info.ok) {
			syncProfileFromInfo(info)
		}
		return {
			loginResult: { ok: true, token: getToken(), msg: '已登录' },
			needLogin: false
		}
	}

	return {
		loginResult: {
			ok: false,
			msg: autoResult.msg || '请先登录',
			needPrivacy: !!autoResult.needPrivacy
		},
		needLogin: true
	}
}
