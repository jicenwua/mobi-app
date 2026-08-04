/** 进入个人资料页前的内存预取，减少跳转后读 storage */

let editCache = null

/**
 * 写入编辑页初始数据
 * @param {object|null} profile
 */
export function setProfileEditCache(profile) {
	if (!profile || typeof profile !== 'object') {
		editCache = null
		return
	}
	editCache = {
		nickname: profile.nickname || '',
		avatar: profile.avatar || '',
		avatarUrl: profile.avatarUrl || profile.avatar || '',
		isDefault: !!profile.isDefault
	}
}

/** 取出并清空编辑页缓存（单次消费） */
export function takeProfileEditCache() {
	const data = editCache
	editCache = null
	return data
}
