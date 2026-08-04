import { request } from '@/api/http/client.js'
import { CUSTOMER_API } from '@/api/constants/customer.js'
import { isApiSuccess, unwrapResponseBody } from '@/utils/api-response.js'

export function generatePayQrcode() {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.PAY_QRCODE_GENERATE,
			method: 'GET',
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					data: body.data || null,
					msg: body.msg || (ok ? '生成成功' : '生成失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

export function verifyPayQrcode({ shopId, token }) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.PAY_QRCODE_VERIFY,
			method: 'GET',
			data: { shopId, token },
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					data: body.data || null,
					msg: body.msg || (ok ? '校验成功' : '校验失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

export function resolveShopInvite(token) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.SHOP_INVITE_RESOLVE,
			method: 'GET',
			data: { token },
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				const data = body.data || null
				if (data && Array.isArray(data.picture)) {
					data.carouselImages = data.picture
				}
				resolve({
					ok,
					data,
					msg: body.msg || (ok ? '查询成功' : '查询失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

export function fetchShopQrcode(shopId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.SHOP_QRCODE}/${shopId}`,
			method: 'GET',
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					data: body.data || null,
					msg: body.msg || (ok ? '生成成功' : '生成失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

export function generateOrderQrcode({ logId, shopId }) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.ORDER_QRCODE_GENERATE,
			method: 'GET',
			data: { logId, shopId },
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					data: body.data || null,
					msg: body.msg || (ok ? '生成成功' : '生成失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

export async function generateStaffInviteQrcode() {
	try {
		const res = await request({
			service: 'customer',
			path: CUSTOMER_API.STAFF_INVITE_QRCODE_GENERATE,
			method: 'GET'
		})
		const body = unwrapResponseBody(res.data)
		const ok = res.statusCode === 200 && isApiSuccess(body)
		return {
			ok,
			data: body.data || null,
			msg: body.msg || (ok ? '生成成功' : '生成失败')
		}
	} catch (err) {
		return {
			ok: false,
			data: null,
			msg: err?.errMsg || err?.message || '网络错误'
		}
	}
}
