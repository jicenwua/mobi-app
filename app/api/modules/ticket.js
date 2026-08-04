import { request } from '@/api/http/client.js'
import { CUSTOMER_API } from '@/api/constants/customer.js'
import { isApiSuccess, unwrapResponseBody, pickApiList } from '@/utils/api-response.js'

function parsePage(res) {
	const body = unwrapResponseBody(res.data)
	const ok = res.statusCode === 200 && isApiSuccess(body)
	return {
		ok,
		rows: pickApiList(body),
		total: body.total ?? 0,
		msg: body.msg || (ok ? '查询成功' : '加载失败')
	}
}

function parseData(res) {
	const body = unwrapResponseBody(res.data)
	const ok = res.statusCode === 200 && isApiSuccess(body)
	return {
		ok,
		data: body.data,
		msg: body.msg || (ok ? '操作成功' : '操作失败')
	}
}

/** 未读客服消息总数 */
export function fetchTicketUnreadCount() {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.TICKET_UNREAD_COUNT,
			method: 'GET',
			success: (res) => {
				const parsed = parseData(res)
				resolve({
					ok: parsed.ok,
					count: Number(parsed.data) || 0,
					msg: parsed.msg
				})
			},
			fail: (err) => resolve({ ok: false, count: 0, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 我的工单列表 */
export function fetchMyTickets(params = {}) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.TICKET_MY,
			method: 'GET',
			data: params,
			success: (res) => resolve(parsePage(res)),
			fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 创建工单 */
export function createTicket(data) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.TICKET,
			method: 'POST',
			data,
			success: (res) => resolve(parseData(res)),
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 工单详情 */
export function fetchTicketDetail(ticketId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.TICKET}/${ticketId}`,
			method: 'GET',
			success: (res) => resolve(parseData(res)),
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 用户回复工单 */
export function replyTicket(ticketId, content) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.TICKET}/${ticketId}/message`,
			method: 'POST',
			data: { content },
			success: (res) => resolve(parseData(res)),
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}
