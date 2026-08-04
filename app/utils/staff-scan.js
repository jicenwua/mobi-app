/**
 * 店员扫码工具：付款码用于收银扣款，订单码用于核销待使用订单
 */
import { verifyPayQrcode } from '@/api/modules/qrcode.js'
import { verifyOrderQrcode } from '@/api/modules/points.js'
import { addShopStaffByToken } from '@/api/modules/shop.js'
import { invalidateMemberOrdersCache } from '@/utils/member-orders-cache.js'
import { parsePayTokenFromScan, parseOrderTokenFromScan, parseStaffInviteTokenFromScan } from '@/utils/qrcode-scan.js'

function openManageCheckout(shopId) {
	uni.navigateTo({
		url: `/pages/shop/manage?id=${shopId}`,
		animationType: 'slide-in-right',
		animationDuration: 200
	})
}

export function scanStaffPayCode({ shopId, onPayVerified }) {
	if (!shopId) {
		uni.showToast({ title: '店铺信息无效', icon: 'none' })
		return
	}
	uni.scanCode({
		onlyFromCamera: false,
		success: async (res) => {
			const payToken = parsePayTokenFromScan(res?.result || '')
			if (!payToken) {
				uni.showToast({ title: '请扫描会员付款码', icon: 'none' })
				return
			}
			const verify = await verifyPayQrcode({ shopId, token: payToken })
			if (!verify.ok || !verify.data) {
				uni.showToast({ title: verify.msg || '付款码无效', icon: 'none' })
				return
			}
			if (typeof onPayVerified === 'function') {
				onPayVerified(payToken, verify.data)
				return
			}
			openManageCheckout(shopId)
		},
		fail: () => {
			uni.showToast({ title: '扫码取消', icon: 'none' })
		}
	})
}

export function scanStaffOrderCode({ shopId, onVerified }) {
	if (!shopId) {
		uni.showToast({ title: '店铺信息无效', icon: 'none' })
		return
	}
	uni.scanCode({
		onlyFromCamera: false,
		success: async (res) => {
			const orderToken = parseOrderTokenFromScan(res?.result || '')
			if (!orderToken) {
				uni.showToast({ title: '请扫描订单核销码', icon: 'none' })
				return
			}
			const verify = await verifyOrderQrcode({
				shopId: Number(shopId),
				token: orderToken
			})
			if (!verify.ok) {
				uni.showToast({ title: verify.msg || '核销失败', icon: 'none' })
				return
			}
			uni.showToast({ title: '订单核销成功', icon: 'success' })
			invalidateMemberOrdersCache()
			if (typeof onVerified === 'function') {
				onVerified()
			}
		},
		fail: () => {
			uni.showToast({ title: '扫码取消', icon: 'none' })
		}
	})
}

/** 自动识别付款码或订单码 */
export function scanStaffVerifyCode({ shopId, onPayVerified }) {
	if (!shopId) {
		uni.showToast({ title: '店铺信息无效', icon: 'none' })
		return
	}
	uni.scanCode({
		onlyFromCamera: false,
		success: async (res) => {
			const raw = res?.result || ''
			const orderToken = parseOrderTokenFromScan(raw)
			if (orderToken) {
				const verify = await verifyOrderQrcode({
					shopId: Number(shopId),
					token: orderToken
				})
				if (!verify.ok) {
					uni.showToast({ title: verify.msg || '核销失败', icon: 'none' })
					return
				}
				uni.showToast({ title: '订单核销成功', icon: 'success' })
				invalidateMemberOrdersCache()
				return
			}
			const payToken = parsePayTokenFromScan(raw)
			if (!payToken) {
				uni.showToast({ title: '无效的二维码', icon: 'none' })
				return
			}
			const verify = await verifyPayQrcode({ shopId, token: payToken })
			if (!verify.ok || !verify.data) {
				uni.showToast({ title: verify.msg || '付款码无效', icon: 'none' })
				return
			}
			if (typeof onPayVerified === 'function') {
				onPayVerified(payToken, verify.data)
				return
			}
			openManageCheckout(shopId)
		},
		fail: () => {
			uni.showToast({ title: '扫码取消', icon: 'none' })
		}
	})
}

export function scanStaffInviteCode({ shopId, onAdded }) {
	if (!shopId) {
		uni.showToast({ title: '店铺信息无效', icon: 'none' })
		return
	}
	uni.scanCode({
		onlyFromCamera: false,
		success: async (res) => {
			const token = parseStaffInviteTokenFromScan(res?.result || '')
			if (!token) {
				uni.showToast({ title: '请扫描店员邀请码', icon: 'none' })
				return
			}
			const result = await addShopStaffByToken({ shopId: Number(shopId), token })
			if (!result.ok) {
				uni.showToast({ title: result.msg || '添加失败', icon: 'none' })
				return
			}
			const name = result.data?.nickname || '该用户'
			uni.showToast({ title: `已添加 ${name} 为店员`, icon: 'success' })
			if (typeof onAdded === 'function') {
				onAdded(result.data)
			}
		},
		fail: () => {
			uni.showToast({ title: '扫码取消', icon: 'none' })
		}
	})
}
