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

/**
 * 我的优惠券分页列表 GET /coupon/my
 * @param {{ status?: number, shopId?: number, pageNum?: number, pageSize?: number }} params
 */
export function fetchMyCoupons(params = {}) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.COUPON_MY,
			method: 'GET',
			data: params,
			success: (res) => resolve(parsePage(res)),
			fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 待使用优惠券数量 */
export async function fetchUnusedCouponCount() {
	const res = await fetchMyCoupons({ status: 0, pageNum: 1, pageSize: 1 })
	if (!res.ok) return { ok: false, count: 0, msg: res.msg }
	return { ok: true, count: Number(res.total) || 0, msg: res.msg }
}
