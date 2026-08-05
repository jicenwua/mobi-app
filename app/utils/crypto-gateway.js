/**
 * 网关请求/响应加解密，与 CryptoGlobalFilter 对齐
 */
import CryptoJS from 'crypto-js'
import JSEncrypt from 'jsencrypt'
import {
	GATEWAY_AES_SECRET_BASE64,
	GATEWAY_CRYPTO_MODE,
	GATEWAY_RSA_PUBLIC_KEY_BASE64
} from '@/config/env.js'

/** 微信小程序无 Node crypto 同步随机源，需补丁 CryptoJS.random */
function patchCryptoJsRandom() {
	const WordArray = CryptoJS.lib.WordArray
	if (WordArray.__mobiRandomPatched) return
	WordArray.random = function randomWordArray(nBytes) {
		const words = []
		const nWords = Math.ceil(nBytes / 4)
		for (let i = 0; i < nWords; i++) {
			words.push((Math.random() * 0x100000000) >>> 0)
		}
		return WordArray.create(words, nBytes)
	}
	WordArray.__mobiRandomPatched = true
}

patchCryptoJsRandom()

export const HDR = {
	ENCRYPTED_BODY: 'X-Encrypted-Body',
	ENCRYPTED: 'X-Encrypted',
	ENCRYPTED_KEY: 'X-Encrypted-Key',
	IV: 'X-IV',
	TIMESTAMP: 'X-Timestamp',
	NONCE: 'X-Nonce',
	SIGNATURE: 'X-Signature'
}

let cryptoEnabledOverride

/** 运行时覆盖是否启用加解密 */
export function setRequestCryptoEnabled(enabled) {
	cryptoEnabledOverride = enabled
}

/** 读取是否启用加解密，未覆盖时用配置默认值 */
export function getRequestCryptoEnabled(defaultValue) {
	if (typeof cryptoEnabledOverride === 'boolean') return cryptoEnabledOverride
	return Boolean(defaultValue)
}

function toPemPublicKey(b64) {
	const s = String(b64 || '').trim()
	if (!s) return ''
	if (s.includes('BEGIN PUBLIC KEY')) return s
	const body = s.replace(/\s/g, '')
	const lines = body.match(/.{1,64}/g) || []
	return `-----BEGIN PUBLIC KEY-----\n${lines.join('\n')}\n-----END PUBLIC KEY-----`
}

function aesEncryptBase64(plainUtf8, aesKeyBase64, ivBase64) {
	const key = CryptoJS.enc.Base64.parse(aesKeyBase64)
	const iv = CryptoJS.enc.Base64.parse(ivBase64)
	const enc = CryptoJS.AES.encrypt(plainUtf8, key, {
		iv,
		mode: CryptoJS.mode.CBC,
		padding: CryptoJS.pad.Pkcs7
	})
	return enc.ciphertext.toString(CryptoJS.enc.Base64)
}

function aesDecryptBase64(payloadBase64, aesKeyBase64, ivBase64) {
	const key = CryptoJS.enc.Base64.parse(aesKeyBase64)
	const iv = CryptoJS.enc.Base64.parse(ivBase64)
	const decrypted = CryptoJS.AES.decrypt(
		{ ciphertext: CryptoJS.enc.Base64.parse(payloadBase64) },
		key,
		{ iv, mode: CryptoJS.mode.CBC, padding: CryptoJS.pad.Pkcs7 }
	)
	return CryptoJS.enc.Utf8.stringify(decrypted)
}

function rsaEncryptUtf8(plainText, publicKeyPem) {
	const jse = new JSEncrypt()
	jse.setPublicKey(publicKeyPem)
	const out = jse.encrypt(plainText)
	if (!out) throw new Error('RSA 加密失败，请检查 GATEWAY_RSA_PUBLIC_KEY_BASE64')
	return out
}

function buildSignature(queryParams, timestamp, nonce, secretStr) {
	const sortedKeys = Object.keys(queryParams || {}).sort()
	let paramString = ''
	for (const k of sortedKeys) {
		paramString += `${k}=${queryParams[k]}&`
	}
	const signSource = `${paramString}timestamp=${timestamp}&nonce=${nonce}&secret=${secretStr}`
	return CryptoJS.SHA256(signSource).toString()
}

function createRsaSessionKey() {
	const pem = toPemPublicKey(GATEWAY_RSA_PUBLIC_KEY_BASE64)
	if (!pem) {
		throw new Error('[crypto] 请在 config/env.js 配置 GATEWAY_RSA_PUBLIC_KEY_BASE64')
	}
	const sessionKey = CryptoJS.lib.WordArray.random(32).toString(CryptoJS.enc.Base64)
	const encryptedKey = rsaEncryptUtf8(sessionKey, pem)
	return { sessionKey, encryptedKey }
}

/** 从完整 URL 解析 query 参数对象 */
export function parseUrlQueryParams(fullUrl) {
	try {
		const u = fullUrl.split('?')
		if (u.length < 2) return {}
		const sp = new URLSearchParams(u[1])
		const out = {}
		sp.forEach((value, key) => {
			out[key] = value
		})
		return out
	} catch {
		return {}
	}
}

/**
 * GET 请求协商会话密钥并附加加密相关请求头
 * @returns {{ sessionKey: string, headers: Record<string, string> } | null}
 */
export function buildEncryptedGetHeaders(fullUrl, cryptoEnabled) {
	if (!cryptoEnabled) return null

	const mode = (GATEWAY_CRYPTO_MODE || 'rsa').toLowerCase()
	const headers = { [HDR.ENCRYPTED_BODY]: 'true' }

	if (mode === 'rsa') {
		const { sessionKey, encryptedKey } = createRsaSessionKey()
		headers[HDR.ENCRYPTED_KEY] = encryptedKey
		return { sessionKey, headers }
	}

	if (mode === 'aes') {
		const secret = String(GATEWAY_AES_SECRET_BASE64 || '').trim()
		if (!secret) {
			throw new Error('[crypto] AES 模式请配置 GATEWAY_AES_SECRET_BASE64')
		}
		Object.assign(headers, buildAesModeSecureHeaders(fullUrl))
		return { sessionKey: secret, headers }
	}

	throw new Error(`[crypto] 不支持的 GATEWAY_CRYPTO_MODE: ${GATEWAY_CRYPTO_MODE}`)
}

/**
 * 将 POST/PUT JSON 加密：密钥与 IV 走请求头，body 仅传密文。
 * @returns {{ body: string, sessionKey: string, headers: Record<string, string> } | null}
 */
export function buildEncryptedRequestBody(method, data, fullUrl, cryptoEnabled) {
	const m = (method || 'GET').toUpperCase()
	if (!cryptoEnabled || (m !== 'POST' && m !== 'PUT')) return null
	if (data === undefined || data === null) return null

	const plain =
		typeof data === 'string' ? data : JSON.stringify(data === '' ? {} : data)

	const mode = (GATEWAY_CRYPTO_MODE || 'rsa').toLowerCase()
	const ivB64 = CryptoJS.lib.WordArray.random(16).toString(CryptoJS.enc.Base64)

	if (mode === 'rsa') {
		const { sessionKey, encryptedKey } = createRsaSessionKey()
		const payload = aesEncryptBase64(plain, sessionKey, ivB64)
		return {
			sessionKey,
			body: payload,
			headers: {
				[HDR.ENCRYPTED_KEY]: encryptedKey,
				[HDR.IV]: ivB64
			}
		}
	}

	if (mode === 'aes') {
		const secret = String(GATEWAY_AES_SECRET_BASE64 || '').trim()
		if (!secret) {
			throw new Error('[crypto] AES 模式请配置 GATEWAY_AES_SECRET_BASE64')
		}
		const payload = aesEncryptBase64(plain, secret, ivB64)
		return {
			sessionKey: secret,
			body: payload,
			headers: {
				[HDR.IV]: ivB64
			}
		}
	}

	throw new Error(`[crypto] 不支持的 GATEWAY_CRYPTO_MODE: ${GATEWAY_CRYPTO_MODE}`)
}

/** AES 模式请求头：时间戳、随机串、签名 */
export function buildAesModeSecureHeaders(fullUrl) {
	const secret = String(GATEWAY_AES_SECRET_BASE64 || '').trim()
	const queryParams = parseUrlQueryParams(fullUrl)
	const timestamp = String(Date.now())
	const nonce = String(Date.now() % 1000000)
	const signature = buildSignature(queryParams, timestamp, nonce, secret)
	return {
		[HDR.TIMESTAMP]: timestamp,
		[HDR.NONCE]: nonce,
		[HDR.SIGNATURE]: signature
	}
}

function responseBodyLooksEncrypted(data, headersLower) {
	const ivInHeader = headersLower[HDR.IV.toLowerCase()]
	if (ivInHeader) return true
	return data && typeof data === 'object' && data.payload != null && data.iv != null
}

/** 从响应 body 提取 AES 密文（兼容纯字符串与旧版 { payload, iv }） */
function extractResponsePayload(data) {
	if (data == null) return null
	if (typeof data === 'string') {
		const trimmed = data.trim()
		if (!trimmed) return null
		try {
			const parsed = JSON.parse(trimmed)
			if (typeof parsed === 'string') return parsed
			if (parsed && parsed.payload != null) return String(parsed.payload)
		} catch {
			return trimmed
		}
	}
	if (typeof data === 'object' && data.payload != null) {
		return String(data.payload)
	}
	return null
}

/** 响应为加密包时解密为 JSON 对象（IV 优先从响应头 X-IV 读取） */
export function maybeDecryptResponse(res, sessionKey) {
	const headers = res.header || res.headers || {}
	const lower = {}
	for (const k of Object.keys(headers)) {
		lower[String(k).toLowerCase()] = headers[k]
	}

	const encHeader = lower[HDR.ENCRYPTED.toLowerCase()]
	const isEncryptedHeader =
		encHeader != null && String(encHeader).toLowerCase() === 'true'
	if (!isEncryptedHeader && !responseBodyLooksEncrypted(res.data, lower)) {
		return res
	}

	const key = String(sessionKey || '').trim()
	if (!key) return res

	const iv =
		lower[HDR.IV.toLowerCase()] ||
		(typeof res.data === 'object' && res.data?.iv != null ? String(res.data.iv) : null)
	const payload = extractResponsePayload(res.data)
	if (!iv || !payload) return res

	try {
		const jsonText = aesDecryptBase64(payload, key, String(iv))
		res.data = JSON.parse(jsonText)
	} catch {
		/* 解密失败时保留原始 data */
	}
	return res
}
