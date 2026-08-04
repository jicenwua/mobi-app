import { DEFAULT_AVATAR_URL, SERVER_DEFAULT_AVATAR_URL } from '@/config/env.js'

/** 头像是否视为未设置 */
export function isEmptyAvatar(url) {
	return !url || !String(url).trim()
}

/** 展示用头像：服务端为空时使用本地默认图 */
export function resolveAvatarUrl(url) {
	return isEmptyAvatar(url) ? DEFAULT_AVATAR_URL : String(url).trim()
}

/** 是否为默认占位图（不应提交给服务端） */
export function isDefaultAvatarUrl(url) {
	const u = String(url || '').trim()
	if (isEmptyAvatar(u)) return true
	if (u === DEFAULT_AVATAR_URL || u.includes('default-avatar')) return true
	if (u === SERVER_DEFAULT_AVATAR_URL) return true
	try {
		const path = u.startsWith('http') ? new URL(u).pathname : u
		const serverPath = new URL(SERVER_DEFAULT_AVATAR_URL).pathname
		if (path === serverPath) return true
	} catch {
		/* 非 URL 时仅字面量匹配 */
	}
	return false
}

/** 提交给后端的 avatar：默认图传空串 */
export function toServerAvatar(url) {
	return isDefaultAvatarUrl(url) ? '' : String(url).trim()
}

/** 可提交后端的自定义头像 URL，默认图返回 undefined */
export function pickServerAvatar(url) {
	const v = toServerAvatar(url)
	return v || undefined
}
