import { requestPrivacyAuthorize } from '@/utils/wx-privacy.js'

function pickImageFailMessage(err) {
	const errno = err?.errno
	const errMsg = String(err?.errMsg || '')
	if (errno === 112 || errMsg.includes('privacy agreement')) {
		return '请在微信公众平台隐私指引中声明「收集你选中的照片或视频信息」'
	}
	return '无法选择图片'
}

/**
 * 选择单张图片（微信小程序走 chooseMedia，其它平台走 chooseImage）
 * @returns {Promise<string|null>} 本地临时路径，取消或失败返回 null
 */
export async function pickImageFile(options = {}) {
	const {
		count = 1,
		sizeType = ['compressed'],
		sourceType = ['album', 'camera']
	} = options

	const authorized = await requestPrivacyAuthorize()
	if (!authorized) {
		uni.showToast({ title: '需先同意隐私保护指引', icon: 'none' })
		return null
	}

	return new Promise((resolve) => {
		const onFail = (err) => {
			console.error('pick image fail', err)
			uni.showToast({ title: pickImageFailMessage(err), icon: 'none', duration: 3000 })
			resolve(null)
		}

		// #ifdef MP-WEIXIN
		uni.chooseMedia({
			count,
			mediaType: ['image'],
			sizeType,
			sourceType,
			success: (res) => resolve(res.tempFiles?.[0]?.tempFilePath || null),
			fail: onFail
		})
		// #endif
		// #ifndef MP-WEIXIN
		uni.chooseImage({
			count,
			sizeType,
			sourceType,
			success: (res) => resolve(res.tempFilePaths?.[0] || null),
			fail: onFail
		})
		// #endif
	})
}
