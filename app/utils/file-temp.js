import { DEFAULT_AVATAR_URL } from '@/config/env.js'

/** 可写临时目录（微信小程序优先 USER_DATA_PATH） */
export function getTempBasePath() {
	// #ifdef MP-WEIXIN
	if (typeof wx !== 'undefined' && wx.env?.USER_DATA_PATH) {
		return wx.env.USER_DATA_PATH
	}
	// #endif
	return `${uni.env?.USER_DATA_PATH || ''}`
}

/** 将 JSON 写入临时文件，供 multipart 的 @RequestPart 使用 */
export function writeJsonTempFile(data, prefix = 'part') {
	return new Promise((resolve, reject) => {
		const base = getTempBasePath()
		if (!base) {
			reject(new Error('无法写入临时目录'))
			return
		}
		const filePath = `${base}/${prefix}_${Date.now()}.json`
		uni.getFileSystemManager().writeFile({
			filePath,
			data: JSON.stringify(data),
			encoding: 'utf8',
			success: () => resolve(filePath),
			fail: (e) => reject(new Error(e.errMsg || '写入临时文件失败'))
		})
	})
}

/** 解析可用于 uni.uploadFile 的本地默认头像路径 */
export function resolveDefaultAvatarUploadPath() {
	return new Promise((resolve, reject) => {
		const src = DEFAULT_AVATAR_URL.startsWith('/')
			? DEFAULT_AVATAR_URL
			: `/${DEFAULT_AVATAR_URL}`
		const base = getTempBasePath()
		if (!base) {
			reject(new Error('无法准备默认头像'))
			return
		}
		const dest = `${base}/default_avatar_${Date.now()}.svg`
		uni.getFileSystemManager().copyFile({
			srcPath: src,
			destPath: dest,
			success: () => resolve(dest),
			fail: (e) => reject(new Error(e.errMsg || '无法读取默认头像'))
		})
	})
}

/** 是否为本地临时路径（可 multipart 上传） */
export function isLocalUploadPath(path) {
	const p = String(path || '').trim()
	if (!p) return false
	return (
		p.startsWith('wxfile://') ||
		p.startsWith('file://') ||
		p.startsWith('http://tmp/') ||
		(!p.startsWith('http://') && !p.startsWith('https://') && !p.startsWith('//'))
	)
}
