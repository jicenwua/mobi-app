import { request } from '@/api/http/client.js'
import { CUSTOMER_API } from '@/api/constants/customer.js'
import { isApiSuccess, unwrapResponseBody, pickApiList } from '@/utils/api-response.js'

function parseListRows(body) {
	return pickApiList(body)
}

function parsePage(res) {
	const body = unwrapResponseBody(res.data)
	const ok = res.statusCode === 200 && isApiSuccess(body)
	return {
		ok,
		rows: parseListRows(body),
		total: body.total ?? 0,
		msg: body.msg || (ok ? '查询成功' : '加载失败')
	}
}

/** 店员：店铺会员积分列表 */
export function fetchShopPointsAccountList(params) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.POINTS_SHOP_ACCOUNT_LIST,
			method: 'GET',
			data: params,
			success: (res) => resolve(parsePage(res)),
			fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 当前会员在指定店铺的消费/积分流水 */
export function fetchMyConsumeLogList(params) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.POINTS_MY_LOG_LIST,
			method: 'GET',
			data: params,
			success: (res) => resolve(parsePage(res)),
			fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 店员：店铺消费记录（列表不含商品明细） */
export function fetchShopConsumeLogList(params) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.POINTS_SHOP_LOG_LIST,
			method: 'GET',
			data: params,
			success: (res) => resolve(parsePage(res)),
			fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 消费记录详情（含商品明细与优惠券抵扣） */
export function fetchConsumeLogDetail(logId, shopId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.POINTS_LOG_DETAIL}/${logId}`,
			method: 'GET',
			data: { shopId },
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					data: body.data ?? null,
					msg: body.msg || (ok ? '查询成功' : '加载失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 店员扫码扣减会员积分 */
export function consumePoints(payload) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.POINTS_CONSUME,
			method: 'POST',
			data: payload,
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					data: body.data,
					msg: body.msg || (ok ? '扣款成功' : '扣款失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 店员/店长线下充值会员积分（基础 + 满赠，单条流水） */
export function rechargeMemberPoints(payload) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.POINTS_RECHARGE,
			method: 'POST',
			data: payload,
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					data: body.data ?? null,
					msg: body.msg || (ok ? '充值成功' : '充值失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 会员自助购买商品 */
export function purchaseProducts(payload) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.POINTS_PURCHASE,
			method: 'POST',
			data: payload,
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					data: body.data ?? null,
					msg: body.msg || (ok ? '购买成功' : '购买失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 会员全部店铺订单列表 */
export function fetchMyOrderList(params) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.POINTS_MY_ORDER_LIST,
			method: 'GET',
			data: params,
			success: (res) => resolve(parsePage(res)),
			fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 店员扫码核销待使用订单 */
export function verifyOrderQrcode(payload) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.ORDER_VERIFY,
			method: 'POST',
			data: payload,
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					data: body.data ?? null,
					msg: body.msg || (ok ? '核销成功' : '核销失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 会员自助退款待使用订单 */
export function refundOrder(payload) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.ORDER_REFUND,
			method: 'POST',
			data: payload,
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					data: body.data ?? null,
					msg: body.msg || (ok ? '退款成功' : '退款失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

