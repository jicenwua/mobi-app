import { getToken } from '@/api/modules/auth-token.js'

/** 未登录浏览时默认展示的店铺 ID */
export const DEFAULT_GUEST_SHOP_ID = '1'

const LOGIN_PAGE = '/pages/login/login'

let guestModeActive = false

export function setGuestMode(active) {
	guestModeActive = !!active
}

export function isGuestMode() {
	return guestModeActive && !getToken()
}

export function isLoggedIn() {
	return !!getToken()
}

/**
 * 需要登录的操作入口：未登录则跳转登录页
 * @returns {boolean} 是否已登录
 */
export function requireLogin() {
	if (isLoggedIn()) return true
	uni.navigateTo({
		url: LOGIN_PAGE,
		animationType: 'slide-in-right',
		animationDuration: 200
	})
	return false
}
