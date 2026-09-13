import { request, buildGatewayUrl } from '@/api/http/client.js'
import { CUSTOMER_API } from '@/api/constants/customer.js'
import { getToken } from '@/api/modules/auth.js'
import { isApiSuccess, unwrapResponseBody, isUnauthorizedResponse, pickApiList } from '@/utils/api-response.js'
import { maybeDecryptResponse } from '@/utils/crypto-gateway.js'
import { uploadMultipartForm } from '@/utils/multipart-upload.js'
import { getCachedShopDetail, setCachedShopDetail } from '@/utils/shop-detail-cache.js'

const PAGE_SIZE = 10

function cacheShopListRows(rows) {
	for (const row of rows || []) {
		const normalized = normalizeShopDetail(row)
		if (normalized) setCachedShopDetail(normalized.id, normalized)
	}
}

/** 将接口返回的轮播图字段统一为 URL 数组（兼容 picture / carouselImages） */
export function getShopCarouselImages(shop) {
	if (!shop) return []
	const raw = shop.carouselImages ?? shop.picture
	if (Array.isArray(raw)) return raw.map((s) => (s || '').trim()).filter(Boolean)
	if (typeof raw === 'string' && raw.trim()) {
		return raw
			.split(',')
			.map((s) => s.trim())
			.filter(Boolean)
	}
	const ext = (shop.shopExterior || '').trim()
	return ext ? [ext] : []
}

export function normalizeShopDetail(row) {
	if (!row || typeof row !== 'object') return null
	const carouselImages = getShopCarouselImages(row)
	const shopId = row.shopId ?? row.id
	if (shopId == null || shopId === '') return null
	return {
		...row,
		id: shopId,
		shopId,
		remainingPoints: row.remainingPoints ?? row.amount ?? row.basePoints,
		carouselImages
	}
}

function normalizeShopRow(row) {
	return normalizeShopDetail(row) ?? row
}

/** 卡片/列表传入的店铺数据是否足以渲染详情页主体 */
export function isUsableShopDetail(shop) {
	const normalized = normalizeShopDetail(shop)
	return !!(normalized?.id && (normalized.shopName || normalized.shopCode))
}

function filterShopsByName(rows, query) {
	const q = (query || '').trim().toLowerCase()
	if (!q) return rows
	return rows.filter((row) => (row.shopName || '').toLowerCase().includes(q))
}

function mergeShopRows(existing, incoming) {
	const map = new Map()
	for (const row of existing) {
		if (row?.id != null) map.set(row.id, row)
	}
	for (const row of incoming) {
		if (row?.id != null) map.set(row.id, row)
	}
	return [...map.values()]
}

function parseShopListPage(res, pageSize = PAGE_SIZE) {
	const body = unwrapResponseBody(res.data)
	if (isUnauthorizedResponse(res, body)) {
		return {
			ok: false,
			unauthorized: true,
			rows: [],
			total: 0,
			hasMore: false,
			msg: body.msg || '登录已失效'
		}
	}
	const ok = res.statusCode === 200 && isApiSuccess(body)
	const rows = pickApiList(body).map(normalizeShopRow)
	const total = Number(body.total) || 0
	const pageRows = rows.length
	return {
		ok,
		rows,
		total,
		hasMore: pageRows >= pageSize,
		msg: body.msg || (ok ? '查询成功' : '加载失败')
	}
}

/** 拼接完整地址 */
export function formatShopAddress(shop) {
	if (!shop) return ''
	const parts = [shop.province, shop.city, shop.district, shop.address]
		.map((s) => (s || '').trim())
		.filter(Boolean)
	return parts.join('')
}

/**
 * 分页查询用户关联店铺（GET /shop），按最后进入时间倒序。
 * @param {{ pageNum: number, query?: string }} params
 */
export function fetchUserShops({ pageNum, query }) {
	const data = {
		pageNum,
		pageSize: PAGE_SIZE
	}

	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.SHOP_LIST,
			method: 'GET',
			data,
			success: (res) => {
				const parsed = parseShopListPage(res)
				if (!parsed.ok) {
					resolve(parsed)
					return
				}
				const rows = filterShopsByName(parsed.rows, query)
				cacheShopListRows(rows)
				resolve({
					...parsed,
					rows
				})
			},
			fail: (err) =>
				resolve({ ok: false, rows: [], hasMore: false, msg: err.errMsg || '网络错误' })
		})
	})
}

function getTempBasePath() {
	// #ifdef MP-WEIXIN
	if (typeof wx !== 'undefined' && wx.env?.USER_DATA_PATH) {
		return wx.env.USER_DATA_PATH
	}
	// #endif
	return `${uni.env?.USER_DATA_PATH || ''}`
}

function writeMetaTempFile(meta) {
	return new Promise((resolve, reject) => {
		const base = getTempBasePath()
		if (!base) {
			reject(new Error('无法写入临时目录'))
			return
		}
		const filePath = `${base}/shop_meta_${Date.now()}.json`
		uni.getFileSystemManager().writeFile({
			filePath,
			data: JSON.stringify(meta),
			encoding: 'utf8',
			success: () => resolve(filePath),
			fail: (e) => reject(new Error(e.errMsg || '写入表单失败'))
		})
	})
}

function buildShopUploadParts(meta, files) {
	const parts = [
		{
			name: 'meta',
			data: JSON.stringify(meta),
			filename: 'meta.json',
			contentType: 'application/json'
		},
		{ name: 'idCardFront', filePath: files.idCardFront },
		{ name: 'idCardBack', filePath: files.idCardBack },
		{ name: 'businessLicensePic', filePath: files.businessLicensePic },
		{ name: 'shopExterior', filePath: files.shopExterior },
		{ name: 'shopInterior', filePath: files.shopInterior }
	]
	for (const filePath of files.carouselImages || []) {
		if (filePath) parts.push({ name: 'carouselImages', filePath })
	}
	return parts.filter((p) => p.filePath || p.data != null)
}

function parseShopAddFullResponse(res) {
	maybeDecryptResponse(res)
	const body = unwrapResponseBody(res.data)
	const ok = res.statusCode === 200 && isApiSuccess(body)
	return {
		ok,
		id: body.data,
		msg: body.msg || (ok ? '提交成功，请等待审核' : '提交失败')
	}
}

/**
 * 提交新增店铺申请（multipart，与后端 /shop/add-full 对齐）
 */
export function submitShopAddFull(meta, files) {
	return new Promise(async (resolve) => {
		try {
			const url = buildGatewayUrl('customer', CUSTOMER_API.SHOP_ADD_FULL)
			const token = getToken()
			const header = {}
			if (token) header.Authorization = `Bearer ${token}`

			// #ifdef MP-WEIXIN
			const res = await uploadMultipartForm({
				url,
				parts: buildShopUploadParts(meta, files),
				header,
				timeout: 120000
			})
			resolve(parseShopAddFullResponse(res))
			return
			// #endif

			const metaPath = await writeMetaTempFile(meta)
			const uploadFiles = [
				{ name: 'meta', uri: metaPath },
				{ name: 'idCardFront', uri: files.idCardFront },
				{ name: 'idCardBack', uri: files.idCardBack },
				{ name: 'businessLicensePic', uri: files.businessLicensePic },
				{ name: 'shopExterior', uri: files.shopExterior },
				{ name: 'shopInterior', uri: files.shopInterior }
			]
			for (const uri of files.carouselImages || []) {
				if (uri) uploadFiles.push({ name: 'carouselImages', uri })
			}

			uni.uploadFile({
				url,
				files: uploadFiles,
				header,
				timeout: 120000,
				success: (res) => resolve(parseShopAddFullResponse(res)),
				fail: (err) => resolve({ ok: false, msg: err.errMsg || '上传失败' })
			})
		} catch (e) {
			resolve({ ok: false, msg: e.message || '提交失败' })
		}
	})
}

/**
 * 分页查询用户关联店铺（含剩余积分，按最后进入时间倒序）
 */
export function fetchMemberShops({ pageNum }) {
	return fetchUserShops({ pageNum })
}

/**
 * 按店铺代码查询店铺详情（加入前预览）GET /shop/enter/{shopCode}
 */
export function fetchEnterShopByCode(shopCode) {
	const code = (shopCode || '').trim()
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: `${CUSTOMER_API.SHOP_ENTER_INFO}/${encodeURIComponent(code)}`,
			method: 'GET',
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

/**
 * 按店铺 ID 查询店铺详情（未登录浏览）GET /shop/guest?shopId=
 */
export function fetchShopPreviewById(shopId) {
	const id = shopId != null && shopId !== '' ? String(shopId) : ''
	return new Promise((resolve) => {
		if (!id) {
			resolve({ ok: false, data: null, msg: '店铺信息无效' })
			return
		}
		request({
			service: 'customer',
			path: CUSTOMER_API.SHOP_GUEST_PREVIEW,
			method: 'GET',
			data: { shopId: id },
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				const data = ok ? normalizeShopDetail(body.data) : null
				if (data) setCachedShopDetail(data.id, data)
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

/**
 * 从用户店铺列表中查找指定店铺（无卡片透传时的兜底）。
 * 列表接口 GET /shop 已返回 ShopDetailRes 读模型。
 */
export async function fetchShopFromUserList(shopId) {
	const targetId = shopId != null && shopId !== '' ? String(shopId) : ''
	if (!targetId) {
		return { ok: false, data: null, msg: '店铺信息无效' }
	}
	const cached = getCachedShopDetail(targetId)
	if (cached) {
		return { ok: true, data: cached, msg: '查询成功' }
	}
	let pageNum = 1
	while (pageNum <= 20) {
		const res = await fetchUserShops({ pageNum })
		if (!res.ok) {
			return { ok: false, data: null, msg: res.msg || '加载失败' }
		}
		const found = (res.rows || []).find((row) => String(row.id) === targetId)
		if (found) {
			const data = normalizeShopDetail(found)
			if (data) setCachedShopDetail(targetId, data)
			return { ok: true, data, msg: '查询成功' }
		}
		if (!res.hasMore) break
		pageNum += 1
	}
	return { ok: false, data: null, msg: '店铺不存在' }
}

function parseListBody(body) {
	return pickApiList(body)
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

function catalogRequest(shopId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.PRODUCT_CATALOG,
			method: 'GET',
			data: { shopId },
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				const categories = ok && Array.isArray(body.data) ? body.data : []
				resolve({
					ok,
					data: categories,
					rows: ok ? flattenCatalogProducts(categories) : [],
					msg: body.msg || (ok ? '查询成功' : '加载失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, rows: [], msg: err.errMsg || '网络错误' })
		})
	})
}

/** 会员详情：店铺商品目录 GET /product/catalog（含分类分组） */
export function fetchShopProductCatalog(shopId) {
	return catalogRequest(shopId)
}

/** 获取分类分组目录 */
export function fetchShopProductCatalogGrouped(shopId) {
	return catalogRequest(shopId).then((res) => ({
		ok: res.ok,
		categories: Array.isArray(res.data) ? res.data : [],
		msg: res.msg
	}))
}

/** 会员详情：进行中满赠活动 GET /activity/ongoing */
export function fetchShopOngoingActivities(shopId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.ACTIVITY_ONGOING,
			method: 'GET',
			data: { shopId },
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					rows: ok ? parseListBody(body) : [],
					msg: body.msg || (ok ? '查询成功' : '加载失败')
				})
			},
			fail: (err) => resolve({ ok: false, rows: [], msg: err.errMsg || '网络错误' })
		})
	})
}

/** 会员详情：最新公告 GET /activity/announcement/latest */
export function fetchShopLatestAnnouncement(shopId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.ACTIVITY_ANNOUNCEMENT_LATEST,
			method: 'GET',
			data: { shopId },
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					data: ok ? body.data || null : null,
					msg: body.msg || (ok ? '查询成功' : '加载失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 会员详情：可领取优惠券 GET /coupon/distributing */
export function fetchShopDistributingCoupons(shopId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.COUPON_DISTRIBUTING,
			method: 'GET',
			data: { shopId },
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					rows: ok ? parseListBody(body) : [],
					msg: body.msg || (ok ? '查询成功' : '加载失败')
				})
			},
			fail: (err) => resolve({ ok: false, rows: [], msg: err.errMsg || '网络错误' })
		})
	})
}

/**
 * 会员店铺详情：异步并行加载活动、公告、可领取优惠券。
 * @param {{ includeCoupons?: boolean }} [options] includeCoupons 为 false 时跳过优惠券请求（如店长角色）
 */
export async function fetchShopMemberExtras(shopId, { includeCoupons = true } = {}) {
	const id = shopId != null && shopId !== '' ? Number(shopId) : NaN
	if (Number.isNaN(id)) {
		return {
			ok: false,
			activities: [],
			announcement: null,
			coupons: [],
			msg: '店铺信息无效'
		}
	}
	const [actRes, annRes, couponRes] = await Promise.all([
		fetchShopOngoingActivities(id),
		fetchShopLatestAnnouncement(id),
		includeCoupons
			? fetchShopDistributingCoupons(id)
			: Promise.resolve({ ok: true, rows: [] })
	])
	const ok = actRes.ok || annRes.ok || couponRes.ok
	const msgs = [actRes, annRes, couponRes].filter((r) => !r.ok && r.msg).map((r) => r.msg)
	return {
		ok,
		activities: actRes.ok ? actRes.rows : [],
		announcement: annRes.ok ? annRes.data : null,
		coupons: couponRes.ok ? couponRes.rows : [],
		msg: ok ? '查询成功' : msgs[0] || '加载失败'
	}
}

/**
 * 会员领取店铺优惠券 POST /coupon/claim
 */
export function claimShopCoupon(templateId, shopId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.COUPON_CLAIM,
			method: 'POST',
			data: {
				templateId,
				shopId: shopId != null && shopId !== '' ? Number(shopId) : undefined
			},
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					data: body.data,
					msg: body.msg || (ok ? '领取成功' : '领取失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 同店铺上报进入时间的冷却间隔（毫秒） */
const ENTER_TIME_COOLDOWN_MS = 60_000
const enterTimeLastAt = new Map()

/**
 * 异步上报进入店铺时间（不阻塞页面、不弹 toast）。
 * 同店铺在冷却期内不重复请求，避免频繁进出详情页刷接口。
 */
export function reportShopEnterTimeAsync(shopId) {
	if (!getToken()) return
	const id = shopId != null && shopId !== '' ? String(shopId) : ''
	if (!id) return

	const now = Date.now()
	const last = enterTimeLastAt.get(id)
	if (last != null && now - last < ENTER_TIME_COOLDOWN_MS) return
	enterTimeLastAt.set(id, now)

	request({
		service: 'customer',
		path: CUSTOMER_API.SHOP_ENTER_TIME,
		method: 'GET',
		data: { shopId: Number(id) },
		success: () => {},
		fail: () => {
			enterTimeLastAt.delete(id)
		}
	})
}

/** 加入店铺 POST /shop/enter?shopId= */
export function joinShop(shopId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.SHOP_ENTER,
			method: 'POST',
			data: { shopId },
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					msg: body.msg || (ok ? '加入成功' : '加入失败')
				})
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 店长扫码添加店员 POST /shop/staff?shopId=&token= */
export function addShopStaffByToken({ shopId, token }) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.SHOP_STAFF_ADD,
			method: 'POST',
			data: { shopId, token },
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					data: body.data || null,
					msg: body.msg || (ok ? '添加成功' : '添加失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 店长查看店员列表 GET /shop/staff/list?shopId= */
export function fetchShopStaffList(shopId) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.SHOP_STAFF_LIST,
			method: 'GET',
			data: { shopId },
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					rows: ok ? parseListBody(body) : [],
					msg: body.msg || (ok ? '查询成功' : '加载失败')
				})
			},
			fail: (err) => resolve({ ok: false, rows: [], msg: err.errMsg || '网络错误' })
		})
	})
}

/** 店长移除店员 DELETE /shop/staff?shopId=&userId= */
export function removeShopStaff({ shopId, userId }) {
	return new Promise((resolve) => {
		request({
			service: 'customer',
			path: CUSTOMER_API.SHOP_STAFF_REMOVE,
			method: 'DELETE',
			data: { shopId, userId },
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					msg: body.msg || (ok ? '已移除店员' : '操作失败')
				})
			},
			fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误' })
		})
	})
}

/** 店铺统计数据 GET /shop/statistics */
export function fetchShopStatistics({ shopId, filterShopId, startDate, endDate }) {
	return new Promise((resolve) => {
		const data = { shopId }
		if (filterShopId != null && filterShopId !== '') {
			data.filterShopId = filterShopId
		}
		if (startDate) {
			data.startDate = startDate
		}
		if (endDate) {
			data.endDate = endDate
		}
		request({
			service: 'customer',
			path: CUSTOMER_API.SHOP_STATISTICS,
			method: 'GET',
			data,
			success: (res) => {
				const body = unwrapResponseBody(res.data)
				const ok = res.statusCode === 200 && isApiSuccess(body)
				resolve({
					ok,
					data: ok ? body.data || null : null,
					msg: body.msg || (ok ? '查询成功' : '加载失败')
				})
			},
			fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || '网络错误' })
		})
	})
}

export { PAGE_SIZE }
