import {
	GATEWAY_BASE_URL,
	GW_SERVICE_PREFIX,
	GW_FORBIDDEN_ADMIN_PATH_PREFIX,
	REQUEST_CRYPTO_ENABLED,
	GATEWAY_CRYPTO_MODE
} from '@/config/env.js'
import {
	getRequestCryptoEnabled,
	buildEncryptedRequestBody,
	buildEncryptedGetHeaders,
	buildAesModeSecureHeaders,
	maybeDecryptResponse,
	HDR
} from '@/utils/crypto-gateway.js'
import { getToken, applyResponseSession } from '@/api/modules/auth-token.js'
import { unwrapResponseBody, isUnauthorizedResponse } from '@/utils/api-response.js'
import {
	handleSessionUnauthorized,
	redirectToLoginIfNeeded,
	syncPermissionsAfterTokenRefresh
} from '@/services/auth-relogin.js'
import { runWithDedupe } from '@/utils/request-dedupe.js'

/** 去掉末尾斜杠 */
function normalizeBase(base) {
	return String(base || '').replace(/\/+$/, '')
}

/** 路径段以单斜杠开头、无末尾斜杠 */
function normalizeSegment(seg) {
	if (!seg) return ''
	const s = seg.startsWith('/') ? seg : `/${seg}`
	return s.replace(/\/+$/, '')
}

function normalizePath(path) {
	if (!path) return '/'
	return path.startsWith('/') ? path : `/${path}`
}

/** 小程序禁止访问管理端网关前缀 */
function isForbiddenAdminGatewayPath(pathname) {
	const block = normalizePath(GW_FORBIDDEN_ADMIN_PATH_PREFIX)
	const p = pathname.replace(/\/+$/, '') || '/'
	return p === block || p.startsWith(`${block}/`)
}

/**
 * 拼出经网关转发的完整 URL
 * @param {'customer'} service 业务服务 key
 * @param {string} path 下游路径，如 /app/login
 */
export function buildGatewayUrl(service, path) {
	const base = normalizeBase(GATEWAY_BASE_URL)
	const prefix = normalizeSegment(GW_SERVICE_PREFIX[service])
	const p = normalizePath(path)
	if (prefix === '') {
		throw new Error(
			`[buildGatewayUrl] 未知 service: ${String(service)}，小程序仅允许 customer（/mobi）`
		)
	}
	return `${base}${prefix}${p}`
}

/** 校验 URL 未指向管理端路径 */
function assertNotAdminGatewayUrl(urlStr) {
	let pathname = ''
	if (/^https?:\/\//i.test(urlStr)) {
		try {
			pathname = new URL(urlStr).pathname
		} catch {
			return
		}
	} else {
		pathname = normalizePath(urlStr)
	}
	if (isForbiddenAdminGatewayPath(pathname)) {
		throw new Error('[request] 禁止请求管理端路径 /mobi/dashboard')
	}
}

/** 按需协商会话密钥：GET 附加头；POST/PUT 加密 body */
function applyRequestCrypto(merged) {
	const cryptoOn = getRequestCryptoEnabled(REQUEST_CRYPTO_ENABLED)
	if (!cryptoOn) return

	const method = (merged.method || 'GET').toUpperCase()
	merged.header = merged.header || {}

	if (method === 'GET') {
		const getCrypto = buildEncryptedGetHeaders(merged.url, true)
		if (!getCrypto) return
		Object.assign(merged.header, getCrypto.headers)
		merged.__cryptoSessionKey = getCrypto.sessionKey
		return
	}

	if (method !== 'POST' && method !== 'PUT') return
	if (merged.data === undefined || merged.data === null) return

	const enc = buildEncryptedRequestBody(method, merged.data, merged.url, true)
	if (!enc) return

	merged.data = enc.body
	merged.__cryptoSessionKey = enc.sessionKey
	merged.header['content-type'] = 'application/json'
	merged.header[HDR.ENCRYPTED_BODY] = 'true'
	Object.assign(merged.header, enc.headers)
	if (String(GATEWAY_CRYPTO_MODE || 'rsa').toLowerCase() === 'aes') {
		Object.assign(merged.header, buildAesModeSecureHeaders(merged.url))
	}
}

/** 登录接口不参与 401 自动重登 */
function isLoginRequestUrl(url) {
	return String(url || '').includes('/app/login')
}

/**
 * 经网关的统一请求封装
 * @param {object} options uni.request 参数，另支持 service+path 或 url
 */
export function request(options) {
	return executeRequest(options, false)
}

function executeRequest(options, retried401) {
	const { service, path, url: rawUrl, success, fail, complete, ...rest } = options
	let url
	if (rawUrl != null && rawUrl !== '') {
		assertNotAdminGatewayUrl(rawUrl)
		if (/^https?:\/\//i.test(rawUrl)) {
			url = rawUrl
		} else {
			url = `${normalizeBase(GATEWAY_BASE_URL)}${normalizePath(rawUrl)}`
		}
	} else if (service && path) {
		url = buildGatewayUrl(service, path)
	} else {
		throw new Error('[request] 请传入 service+path，或传入相对网关的 url')
	}

	const merged = { ...rest, url }
	const token = getToken()
	if (token) {
		merged.header = { ...(merged.header || {}) }
		merged.header.Authorization = `Bearer ${token}`
	}

	try {
		applyRequestCrypto(merged)
	} catch (e) {
		const errMsg = e?.message || String(e)
		fail?.({ errMsg })
		complete?.()
		return { errMsg, abort() {} }
	}

	const dedupeConfig = {
		method: merged.method || 'GET',
		url,
		data: merged.data,
		params: merged.params
	}

	return runWithDedupe(
		() =>
			new Promise((resolve, reject) => {
				uni.request({
					...merged,
					success(res) {
						maybeDecryptResponse(res, merged.__cryptoSessionKey)
						resolve(res)
					},
					fail: reject
				})
			}),
		dedupeConfig,
		{ silent: true }
	)
		.then(async (res) => {
			const { tokenChanged, permChanged } = applyResponseSession(res)
			if (tokenChanged && !permChanged) {
				await syncPermissionsAfterTokenRefresh()
			}
			const body = unwrapResponseBody(res.data)
			if (
				!retried401 &&
				!isLoginRequestUrl(url) &&
				isUnauthorizedResponse(res, body)
			) {
				try {
					const loginResult = await handleSessionUnauthorized()
					if (loginResult.ok) {
						return executeRequest(options, true)
					}
					redirectToLoginIfNeeded(loginResult)
				} catch {
					redirectToLoginIfNeeded({ ok: false })
				}
			}
			success?.(res)
			return res
		})
		.catch((err) => {
			if (err?.duplicateRequest) {
				uni.showToast({ title: err.message || '请勿重复提交', icon: 'none' })
			}
			fail?.(err)
			throw err
		})
		.finally(() => {
			complete?.()
		})
}
