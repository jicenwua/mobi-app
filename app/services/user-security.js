const SECURITY_KEY = 'user_security'

const defaultSecurity = () => ({
	hasSetPassword: false
})

/** 读取本地安全状态（是否已设置支付密码） */
export function getUserSecurity() {
	try {
		const stored = uni.getStorageSync(SECURITY_KEY)
		if (stored && typeof stored === 'object') {
			return { ...defaultSecurity(), ...stored, hasSetPassword: !!stored.hasSetPassword }
		}
	} catch {
		/* 忽略 */
	}
	return defaultSecurity()
}

/** 是否已设置支付密码 */
export function hasPayPasswordSet() {
	return getUserSecurity().hasSetPassword
}

/** 写入安全状态（与 /app/info、登录响应同步） */
export function setUserSecurity(partial) {
	const next = {
		...getUserSecurity(),
		...partial,
		hasSetPassword: !!partial?.hasSetPassword
	}
	uni.setStorageSync(SECURITY_KEY, next)
	return next
}

/** 根据服务端 setPassword / isSetPassword 字段更新缓存 */
export function applyServerSetPasswordFlag(raw) {
	if (raw === undefined || raw === null) return getUserSecurity()
	const hasSetPassword = !!raw
	return setUserSecurity({ hasSetPassword })
}

/** 登出或 token 失效时清除 */
export function clearUserSecurity() {
	try {
		uni.removeStorageSync(SECURITY_KEY)
	} catch {
		/* 忽略 */
	}
}
