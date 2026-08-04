import { request, buildGatewayUrl } from '@/api/http/client.js'
import { CUSTOMER_API } from '@/api/constants/customer.js'
import { getToken } from '@/api/modules/auth.js'
import { isApiSuccess, unwrapResponseBody, pickApiData, pickApiList } from '@/utils/api-response.js'
import { uploadMultipartForm } from '@/utils/multipart-upload.js'
import { isLocalUploadPath } from '@/utils/file-temp.js'
import { maybeDecryptResponse } from '@/utils/crypto-gateway.js'
import { PRODUCT_STATUS } from '@/utils/product-status.js'

function parseData(res) {
	const body = unwrapResponseBody(res.data)
	const ok = res.statusCode === 200 && isApiSuccess(body)
	return { ok, data: body.data, msg: body.msg || (ok ? '成功' : '失败') }
}

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

function flattenCatalogProducts(categories) {
	const rows = []
	for (const category of categories || []) {
		for (const product of category?.products || []) {
			if (product) rows.push(product)
		}
	}
	return rows
}

export function fetchManageProducts(shopId, pageNum = 1, pageSize = 50) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.PRODUCT_CATALOG,
			method: 'GET',
			data: { shopId },
			success: (res) => {
				const parsed = parseData(res)
				if (!parsed.ok) {
					resolve({ ok: false, rows: [], categories: [], total: 0, msg: parsed.msg })
					return
				}
				const categories = Array.isArray(parsed.data) ? parsed.data : []
				const rows = flattenCatalogProducts(categories)
				resolve({ ok: true, rows, categories, total: rows.length, msg: parsed.msg })
			},
			fail: (err) => resolve({ ok: false, rows: [], categories: [], total: 0, msg: err.errMsg || '网络错误' })
		})
	})
}

function buildProductBody(payload) {
	const item = {
		shopId: payload.shopId,
		productName: payload.productName,
		price: payload.price,
		status: payload.status ?? 1
	}
	if (payload.productId != null) item.productId = payload.productId
	if (payload.categoryId != null) item.categoryId = payload.categoryId
	if (payload.description != null) item.description = payload.description
	if (payload.imageUrl != null) item.imageUrl = payload.imageUrl
	if (payload.stock != null) item.stock = payload.stock
	return item
}

export function fetchManageProductCategories(shopId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.PRODUCT_CATEGORY,
			method: 'GET',
			data: { shopId },
			success: (res) => {
				const parsed = parseData(res)
				const rows = Array.isArray(parsed.data) ? parsed.data : []
				resolve({ ok: parsed.ok, rows, msg: parsed.msg })
			},
			fail: (err) => resolve({ ok: false, rows: [], msg: err.errMsg || '网络错误' })
		})
	})
}

export function addManageProductCategory(payload) {
	return addManageProductCategories([
		{ shopId: payload.shopId, categoryName: payload.categoryName, sortOrder: payload.sortOrder }
	])
}

/** @param {Array<{ shopId: number, categoryName: string, sortOrder?: number }>} items */
export function addManageProductCategories(items) {
	return new Promise((resolve) => {
		if (!items?.length) {
			resolve({ ok: true, msg: '分类新增成功' })
			return
		}
		request({
			service: 'customer',
			path: CUSTOMER_API.PRODUCT_CATEGORY,
			method: 'POST',
			data: items.map((item) =>
				Object.fromEntries(
					Object.entries({
						shopId: item.shopId,
						categoryName: item.categoryName,
						sortOrder: item.sortOrder
					}).filter(([, v]) => v != null)
				)
			),
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, msg: body.msg || (ok ? '分类新增成功' : '添加失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

/** @param {Array<{ categoryId: number, categoryName?: string, sortOrder?: number }>} items */
export function updateManageProductCategories(items) {
	return new Promise((resolve) => {
		if (!items?.length) {
			resolve({ ok: true, msg: '分类更新成功' })
			return
		}
		request({
			service: 'customer',
			path: CUSTOMER_API.PRODUCT_CATEGORY,
			method: 'PUT',
			data: items.map((item) =>
				Object.fromEntries(
					Object.entries({
						categoryId: item.categoryId,
						categoryName: item.categoryName,
						sortOrder: item.sortOrder
					}).filter(([, v]) => v != null)
				)
			),
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, msg: body.msg || (ok ? '分类更新成功' : '更新失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

export function deleteManageProductCategory(categoryId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.PRODUCT_CATEGORY}/${categoryId}`,
			method: 'DELETE',
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, msg: body.msg || (ok ? '分类删除成功' : '删除失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

function parseMutationResponse(res) {
	maybeDecryptResponse(res)
	const body = unwrapResponseBody(res.data)
	const ok = res.statusCode === 200 && isApiSuccess(body)
	return { ok, msg: body.msg || (ok ? '保存成功' : '保存失败') }
}

function buildProductMultipartParts(metaList, imagePaths = []) {
	const parts = [
		{
			name: 'meta',
			data: JSON.stringify(metaList),
			filename: 'meta.json',
			contentType: 'application/json'
		}
	]
	for (const filePath of imagePaths) {
		if (filePath && isLocalUploadPath(filePath)) {
			parts.push({ name: 'images', filePath })
		}
	}
	return parts
}

function buildProductUpdateMultipartParts(meta, imagePath) {
	const parts = [
		{
			name: 'meta',
			data: JSON.stringify(meta),
			filename: 'meta.json',
			contentType: 'application/json'
		}
	]
	if (imagePath && isLocalUploadPath(imagePath)) {
		parts.push({ name: 'image', filePath: imagePath })
	}
	return parts
}

function submitProductMultipart(url, metaList, imagePaths = []) {
	const token = getToken()
	const header = {}
	if (token) header.Authorization = `Bearer ${token}`
	return uploadMultipartForm({
		url,
		parts: buildProductMultipartParts(metaList, imagePaths),
		header,
		timeout: 120000
	}).then(parseMutationResponse)
}

/**
 * 批量新增商品；统一走 multipart（meta 必填，images 可选）。
 */
export function addManageProducts(items) {
	const list = Array.isArray(items) ? items : [items]
	const metaList = list.map(({ imagePath, ...rest }) => buildProductBody(rest))
	const imagePaths = list.map((item) => (isLocalUploadPath(item.imagePath) ? item.imagePath : null))
	const paths = imagePaths.filter(Boolean)
	const metaWithIndex = metaList.map((meta, i) => {
		if (!imagePaths[i]) return meta
		const imageIndex = paths.indexOf(imagePaths[i])
		return imageIndex >= 0 ? { ...meta, imageIndex } : meta
	})
	return submitProductMultipart(buildGatewayUrl('customer', CUSTOMER_API.PRODUCT), metaWithIndex, paths)
}

export function addManageProduct(payload) {
	return addManageProducts([payload])
}

/** 统一走 multipart：meta 须含 productId，image 可选；响应 data 为更新后的图片地址（未换图时为 null） */
export function updateManageProduct(productId, payload) {
	const { imagePath, ...rest } = payload || {}
	const body = buildProductBody({ ...rest, productId: Number(productId), status: undefined })
	const token = getToken()
	const header = {}
	if (token) header.Authorization = `Bearer ${token}`
	return uploadMultipartForm({
		url: buildGatewayUrl('customer', CUSTOMER_API.PRODUCT),
		method: 'PUT',
		parts: buildProductUpdateMultipartParts(body, imagePath),
		header,
		timeout: 120000
	})
		.then((res) => {
			const parsed = parseMutationResponse(res)
			return {
				ok: parsed.ok,
				msg: parsed.msg,
				imageUrl: parsed.ok ? pickApiData(unwrapResponseBody(res.data)) : undefined
			}
		})
		.catch((err) => ({ ok: false, msg: err.message || '上传失败' }))
}

/** 追加库存；stock 为正数追加、-1 设为无限 */
export function updateManageProductStock(productId, stock) {
	if (productId == null || stock == null || stock === '') {
		return Promise.resolve({ ok: false, msg: '商品ID与库存数量不能为空' })
	}
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.PRODUCT}/stock`,
			method: 'PUT',
			data: {
				productId: Number(productId),
				stock: Number(stock)
			},
			success: (res) => resolve(parseMutationResponse(res)),
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

export function updateManageProductStatus(productId, status) {
	if (status !== PRODUCT_STATUS.ON_SALE && status !== PRODUCT_STATUS.OFF_SHELF) {
		return Promise.resolve({ ok: false, msg: '仅支持设置为出售中或下架' })
	}
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.PRODUCT}/${productId}/status?status=${status}`,
			method: 'PUT',
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, msg: body.msg || (ok ? '状态更新成功' : '更新失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

export function deleteManageProduct(productId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.PRODUCT}/${productId}`,
			method: 'DELETE',
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, msg: body.msg || (ok ? '删除成功' : '删除失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

export function fetchManageActivities(shopId, pageNum = 1, pageSize = 50) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.ACTIVITY_LIST,
			method: 'GET',
			data: { shopId, pageNum, pageSize },
			success: (res) => resolve(parsePage(res)),
			fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || '网络错误' })
		})
	})
}

export function fetchManageActivityDetail(activityId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.ACTIVITY}/${activityId}`,
			method: 'GET',
			success: (res) => resolve(parseData(res)),
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

function buildActivityPayload(payload) {
	const kind = payload.kind ?? 1
	const item = {
		shopId: payload.shopId,
		activityName: payload.activityName,
		kind,
		description: payload.description?.trim() || null,
		startTime: payload.startTime ?? null,
		endTime: payload.endTime ?? null
	}
	if (kind === 1) {
		item.activityType = payload.activityType
		item.rules = payload.rules
	}
	return item
}

export function saveManageActivity(payload) {
	const activityId = payload?.activityId
	const isUpdate = activityId != null && activityId !== ''
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: isUpdate ? `${CUSTOMER_API.ACTIVITY}/${activityId}` : CUSTOMER_API.ACTIVITY,
			method: isUpdate ? 'PUT' : 'POST',
			data: buildActivityPayload(payload),
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, data: body.data, msg: body.msg || (ok ? '保存成功' : '保存失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

export function fetchManageCoupons(shopId, pageNum = 1, pageSize = 50) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.COUPON_TEMPLATE_LIST,
			method: 'GET',
			data: { shopId, pageNum, pageSize },
			success: (res) => resolve(parsePage(res)),
			fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || '网络错误' })
		})
	})
}

export function addManageCoupon(payload) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.COUPON_TEMPLATE,
			method: 'POST',
			data: payload,
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, msg: body.msg || (ok ? '添加成功' : '添加失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

export function addManageCouponStock(templateId, payload) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.COUPON_TEMPLATE_STOCK}/${templateId}/stock`,
			method: 'PUT',
			data: {
				shopId: payload.shopId,
				addQuantity: payload.addQuantity
			},
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, msg: body.msg || (ok ? '库存追加成功' : '操作失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

export function adjustManageCouponDistribution(templateId, payload) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.COUPON_TEMPLATE}/${templateId}/distribution`,
			method: 'PUT',
			data: {
				shopId: payload.shopId,
				distributionStartTime: payload.distributionStartTime,
				distributionEndTime: payload.distributionEndTime
			},
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, msg: body.msg || (ok ? '更新成功' : '操作失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

export function stopManageCouponDistribution(templateId, shopId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.COUPON_TEMPLATE}/${templateId}/stop?shopId=${shopId}`,
			method: 'PUT',
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, msg: body.msg || (ok ? '已停止发放' : '操作失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

export function resumeManageCouponDistribution(templateId, shopId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.COUPON_TEMPLATE}/${templateId}/resume?shopId=${shopId}`,
			method: 'PUT',
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, msg: body.msg || (ok ? '已恢复发放' : '操作失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

export function deleteManageCoupon(templateId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.COUPON_TEMPLATE}/${templateId}`,
			method: 'DELETE',
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, msg: body.msg || (ok ? '删除成功' : '删除失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 日期字符串转 ISO 日期时间（当天 00:00:00） */
export function dateToDateTime(dateStr) {
	if (!dateStr) return null
	const d = String(dateStr).trim()
	if (!d) return null
	return d.length > 10 ? d.replace(' ', 'T') : `${d}T00:00:00`
}

/** 日期字符串转 ISO 日期时间（当天 23:59:59，用于发放结束日） */
export function dateToDateTimeEnd(dateStr) {
	if (!dateStr) return null
	const d = String(dateStr).trim()
	if (!d) return null
	if (d.length > 10) return d.replace(' ', 'T')
	return `${d}T23:59:59`
}

/** 正数金额 */
export function parsePositiveAmount(value) {
	const n = parseFloat(String(value ?? '').trim())
	if (Number.isNaN(n) || n <= 0) return null
	return n
}

/** 正整数数量 */
export function parsePositiveInt(value) {
	const n = parseInt(String(value ?? '').trim(), 10)
	if (Number.isNaN(n) || n <= 0) return null
	return n
}

/** 可选正整数：留空返回 null（表示不限） */
export function parseOptionalPositiveInt(value) {
	const raw = String(value ?? '').trim()
	if (!raw) return null
	return parsePositiveInt(raw)
}

/** 可选金额：留空返回 0（表示无门槛） */
export function parseOptionalAmount(value) {
	const raw = String(value ?? '').trim()
	if (!raw) return 0
	const n = parseFloat(raw)
	if (Number.isNaN(n) || n < 0) return null
	return n
}

export function deleteManageActivity(activityId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.ACTIVITY}/${activityId}`,
			method: 'DELETE',
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, msg: body.msg || (ok ? '删除成功' : '删除失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

export function stopManageActivity(activityId, shopId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.ACTIVITY}/${activityId}/stop?shopId=${shopId}`,
			method: 'PUT',
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, msg: body.msg || (ok ? '活动已停止' : '操作失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

export function enableManageActivity(activityId, shopId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.ACTIVITY}/${activityId}/enable?shopId=${shopId}`,
			method: 'PUT',
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({ ok, msg: body.msg || (ok ? '活动已启用' : '操作失败') })
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 分转元展示 */
export function formatPriceYuan(fen) {
	if (fen == null || Number.isNaN(Number(fen))) return '0.00'
	return (Number(fen) / 100).toFixed(2)
}

/** 元转分提交 */
export function yuanToFen(yuan) {
	const n = parseFloat(String(yuan).trim())
	if (Number.isNaN(n) || n <= 0) return null
	return Math.round(n * 100)
}

/** 商品积分（正整数） */
export function parseProductPoints(value) {
	const n = parseInt(String(value ?? '').trim(), 10)
	if (Number.isNaN(n) || n <= 0) return null
	return n
}
