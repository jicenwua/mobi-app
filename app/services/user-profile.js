import { isDefaultAvatarUrl, resolveAvatarUrl } from '@/utils/avatar.js'

const PROFILE_KEY = 'user_profile'
const COUPON_COUNT_KEY = 'user_coupon_count'
const PROFILE_SETUP_DONE_KEY = 'profile_setup_done'

const defaultProfile = () => ({
	avatar: '',
	avatarUrl: '',
	nickname: '',
	isDefault: false
})

/** 生成默认昵称，如「会员384291」 */
export function generateDefaultNickname() {
	const suffix = String(Math.floor(100000 + Math.random() * 900000))
	return `会员${suffix}`
}

/** 读取本地用户资料（空头像补默认图） */
export function getUserProfile() {
	try {
		const stored = uni.getStorageSync(PROFILE_KEY)
		if (stored && typeof stored === 'object') {
			const merged = { ...defaultProfile(), ...stored }
			const avatar = resolveAvatarUrl(merged.avatar || merged.avatarUrl)
			return { ...merged, avatar, avatarUrl: avatar }
		}
	} catch {
		/* 存储不可用时返回空资料 */
	}
	return defaultProfile()
}

/** 写入用户资料 */
export function setUserProfile(profile) {
	const avatar = resolveAvatarUrl(profile?.avatar || profile?.avatarUrl || '')
	const next = { ...defaultProfile(), ...profile, avatar, avatarUrl: avatar }
	uni.setStorageSync(PROFILE_KEY, next)
	return next
}

/** 是否已完成首次资料流程 */
export function hasCompletedProfileSetup() {
	try {
		return !!uni.getStorageSync(PROFILE_SETUP_DONE_KEY)
	} catch {
		return false
	}
}

/** 标记已完成登录/资料授权流程 */
export function markProfileSetupDone() {
	uni.setStorageSync(PROFILE_SETUP_DONE_KEY, true)
}

/** 生成首次登录用的默认资料（不写本地） */
export function buildDefaultLoginProfile() {
	return {
		avatar: '',
		avatarUrl: '',
		nickname: generateDefaultNickname(),
		isDefault: true
	}
}

/**
 * 静默登录成功后同步服务端昵称/头像
 * @param {{ nickname?: string, avatarUrl?: string, usesDefaultAvatar?: boolean }} params
 */
export function applyLoginProfile({
	nickname = '',
	avatarUrl = '',
	usesDefaultAvatar = false
}) {
	const current = getUserProfile()
	const name = (nickname || '').trim() || current.nickname
	const url = resolveAvatarUrl(avatarUrl || current.avatarUrl || current.avatar)
	return setUserProfile({
		...current,
		nickname: name,
		avatar: url,
		avatarUrl: url,
		isDefault: usesDefaultAvatar
	})
}

/**
 * 首次默认昵称登录成功后落库
 * @param {ReturnType<typeof buildDefaultLoginProfile>} profile
 */
export function applyDefaultProfile(profile) {
	const base = profile || buildDefaultLoginProfile()
	const avatar = resolveAvatarUrl(base.avatar || base.avatarUrl)
	const next = setUserProfile({
		avatar,
		avatarUrl: avatar,
		nickname: base.nickname || generateDefaultNickname(),
		isDefault: true
	})
	markProfileSetupDone()
	return next
}

/**
 * 个人资料页手动修改后落库
 * @param {{ avatar?: string, nickname?: string, isDefault?: boolean }} params
 */
export function applyManualProfile({ avatar = '', nickname = '', isDefault } = {}) {
	const name = (nickname || '').trim()
	if (!name) return null
	const url = resolveAvatarUrl(avatar)
	const profile = setUserProfile({
		avatar: url,
		avatarUrl: url,
		nickname: name,
		isDefault:
			isDefault !== undefined ? !!isDefault : isDefaultAvatarUrl(url)
	})
	markProfileSetupDone()
	return profile
}

/** 读取折扣券数量（本地占位，待对接接口） */
export function getCouponCount() {
	try {
		const n = uni.getStorageSync(COUPON_COUNT_KEY)
		if (typeof n === 'number' && n >= 0) return n
		if (typeof n === 'string' && n !== '') {
			const parsed = parseInt(n, 10)
			if (!Number.isNaN(parsed) && parsed >= 0) return parsed
		}
	} catch {
		/* 忽略 */
	}
	return 0
}
