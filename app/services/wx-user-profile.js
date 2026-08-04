/**
 * 微信头像昵称（登录资料）
 * getUserProfile 自 2022.10 起不再弹窗，成功回调固定返回「微信用户」+ 灰色默认头像；
 * 登录须使用「头像昵称填写能力」：button open-type="chooseAvatar" + input type="nickname"。
 */

/** 接口收回后 getUserProfile 返回的占位昵称 */
export const WX_PLACEHOLDER_NICKNAME = '微信用户'

/** 是否为 getUserProfile 收回后的占位资料（不可用于登录） */
export function isWxPlaceholderProfile({ nickName, nickname } = {}) {
	const name = (nickName || nickname || '').trim()
	return !name || name === WX_PLACEHOLDER_NICKNAME
}

/** 将微信头像 URL 下载为本地临时路径，供 multipart 上传 */
export function downloadAvatarTempPath(avatarUrl) {
	const url = (avatarUrl || '').trim()
	if (!url) {
		return Promise.reject(new Error('未获取到头像'))
	}
	return new Promise((resolve, reject) => {
		uni.downloadFile({
			url,
			timeout: 30000,
			success: (res) => {
				if (res.statusCode === 200 && res.tempFilePath) {
					resolve(res.tempFilePath)
					return
				}
				reject(new Error('头像下载失败'))
			},
			fail: (err) => reject(new Error(err.errMsg || '头像下载失败'))
		})
	})
}
