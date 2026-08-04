/**
 * 微信官方「隐私信息授权」API 封装
 * - wx.getPrivacySetting
 * - wx.requirePrivacyAuthorize
 * - wx.openPrivacyContract
 * - wx.onNeedPrivacyAuthorization
 * @see https://developers.weixin.qq.com/miniprogram/dev/framework/user-privacy/
 */
import { ref } from 'vue'

/** 自定义隐私弹窗（配合 onNeedPrivacyAuthorization） */
export const privacyPopupVisible = ref(false)
/** 微信侧是否已完成隐私指引授权 */
export const wxPrivacyAuthorized = ref(false)

/** 隐私指引名称展示为《xxx》，避免与接口返回值重复加书名号 */
export function formatPrivacyContractLabel(name) {
	const inner = String(name || '')
		.trim()
		.replace(/^《+/, '')
		.replace(/》+$/, '')
	return inner ? `《${inner}》` : '《用户隐私保护指引》'
}

/** 检查是否为首次访问 */
export function checkFirstVisit() {
	return new Promise((resolve) => {
		// #ifdef MP-WEIXIN
		const visited = uni.getStorageSync('has_visited_before')
		if (!visited) {
			// 标记为已访问
			uni.setStorageSync('has_visited_before', true)
			resolve(true)
		} else {
			resolve(false)
		}
		// #endif
		// #ifndef MP-WEIXIN
		resolve(false)
		// #endif
	})
}

/** 主动显示隐私弹窗（用于首次进入或登录页引导） */
export function showPrivacyPopupManually() {
	privacyPopupVisible.value = true
}

/** 根据 wx.getPrivacySetting 同步微信侧授权状态 */
export async function syncPrivacyAuthFromSetting() {
	const state = await getPrivacySettingState()
	if (!state.needAuthorization) {
		wxPrivacyAuthorized.value = true
	}
	return state
}

/** wx.onNeedPrivacyAuthorization 传入的 resolve */
let pendingPrivacyResolve = null
/** requirePrivacyAuthorize / 业务等待回调 */
let privacyWaitResolve = null

function isWxMp() {
	// #ifdef MP-WEIXIN
	return true
	// #endif
	// #ifndef MP-WEIXIN
	return false
	// #endif
}

function settleWait(ok) {
	if (privacyWaitResolve) {
		const fn = privacyWaitResolve
		privacyWaitResolve = null
		fn(ok)
	}
}

/** wx.openPrivacyContract：打开后台配置的隐私指引 */
export function openPrivacyContract() {
	if (!isWxMp() || typeof uni.openPrivacyContract !== 'function') {
		uni.showToast({ title: '请在微信小程序中查看', icon: 'none' })
		return
	}
	uni.openPrivacyContract({
		fail: () => uni.showToast({ title: '暂无法打开隐私指引', icon: 'none' })
	})
}

function showPrivacyPopup(resolve) {
	if (resolve) {
		pendingPrivacyResolve = resolve
	}
	privacyPopupVisible.value = true
}

function showManualPrivacyPopup(done) {
	privacyWaitResolve = done
	showPrivacyPopup(null)
}

function finishPrivacyResolve(payload) {
	if (pendingPrivacyResolve) {
		pendingPrivacyResolve(payload)
		pendingPrivacyResolve = null
	}
	if (!privacyWaitResolve) {
		privacyPopupVisible.value = false
	}
}

/** 弹窗内点击同意（button open-type="agreePrivacyAuthorization"） */
export function onPrivacyAgree() {
	finishPrivacyResolve({ event: 'agree', buttonId: 'privacy-agree-btn' })
	settleWait(true)
	wxPrivacyAuthorized.value = true
	privacyPopupVisible.value = false
}

/** 弹窗内点击拒绝 */
export function onPrivacyDisagree() {
	finishPrivacyResolve({ event: 'disagree', buttonId: 'privacy-disagree-btn' })
	settleWait(false)
	privacyPopupVisible.value = false
}

/** wx.onNeedPrivacyAuthorization：须在 App.onLaunch 注册 */
export function setupPrivacyAuthorization() {
	if (!isWxMp() || typeof uni.onNeedPrivacyAuthorization !== 'function') {
		return
	}
	uni.onNeedPrivacyAuthorization((resolve) => {
		showPrivacyPopup(resolve)
	})
}

/** wx.getPrivacySetting：查询是否需用户同意隐私指引 */
export function getPrivacySettingState() {
	return new Promise((resolve) => {
		if (!isWxMp() || typeof uni.getPrivacySetting !== 'function') {
			resolve({
				needAuthorization: false,
				privacyContractName: '用户隐私保护指引'
			})
			return
		}
		uni.getPrivacySetting({
			success: (res) => {
				resolve({
					needAuthorization: !!res.needAuthorization,
					privacyContractName: res.privacyContractName || '用户隐私保护指引'
				})
			},
			fail: () =>
				resolve({
					needAuthorization: false,
					privacyContractName: '用户隐私保护指引'
				})
		})
	})
}

/**
 * wx.requirePrivacyAuthorize：主动拉起隐私授权（会触发 onNeedPrivacyAuthorization 弹窗）
 * @returns {Promise<boolean>}
 */
export function requestPrivacyAuthorize() {
	return new Promise((resolve) => {
		if (!isWxMp()) {
			resolve(true)
			return
		}
		getPrivacySettingState().then(({ needAuthorization }) => {
			if (!needAuthorization) {
				resolve(true)
				return
			}
			if (typeof uni.requirePrivacyAuthorize !== 'function') {
				showManualPrivacyPopup(resolve)
				return
			}

			let settled = false
			const done = (ok) => {
				if (settled) return
				settled = true
				clearTimeout(timer)
				if (!ok && !pendingPrivacyResolve) {
					privacyPopupVisible.value = false
				}
				resolve(!!ok)
			}

			// 使用自定义弹窗，避免 requirePrivacyAuthorize 无回调导致框架层 timeout
			showManualPrivacyPopup(done)
		})
	})
}

/**
 * 进入登录页时：查询隐私状态并尝试拉起授权弹窗
 * @returns {Promise<{ needAuthorization: boolean, privacyContractName: string, authorized: boolean }>}
 */
export async function initPrivacyOnPageEnter() {
	const state = await getPrivacySettingState()
	if (!state.needAuthorization) {
		wxPrivacyAuthorized.value = true
		return { ...state, authorized: true }
	}
	const authorized = await requestPrivacyAuthorize()
	wxPrivacyAuthorized.value = authorized
	return { ...state, authorized }
}

/**
 * 登录前再次确认隐私已同意（getPrivacySetting + 必要时 requirePrivacyAuthorize）
 */
export async function ensurePrivacyBeforeUserProfile() {
	const state = await getPrivacySettingState()
	if (!state.needAuthorization) {
		return true
	}
	return requestPrivacyAuthorize()
}
