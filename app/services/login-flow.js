import { wxLogin, fetchCurrentUserInfo } from '@/api/modules/auth.js'
import {
	applyLoginProfile,
	markProfileSetupDone
} from '@/services/user-profile.js'

/**
 * 一键登录：仅提交微信 code，由后端返回 token 与是否首次注册
 */
export async function performQuickLogin() {
	const loginResult = await wxLogin()
	if (!loginResult.ok) {
		return loginResult
	}

	if (loginResult.register) {
		return loginResult
	}

	markProfileSetupDone()
	const info = await fetchCurrentUserInfo()
	if (info.ok) {
		applyLoginProfile({
			nickname: info.nickname,
			avatarUrl: info.avatarUrl,
			usesDefaultAvatar: info.usesDefaultAvatar
		})
	}

	return loginResult
}

/** 根据登录结果决定跳转页 */
export function resolvePostLoginPath(register) {
	return register ? '/pages/login/profile-setup' : '/pages/main/main'
}
