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
 * 将 POST/PUT JSON 转为网关可解密字符串；不加密时返回 null
 */
export function buildEncryptedRequestBody(method, data, fullUrl, cryptoEnabled) {
	const m = (method || 'GET').toUpperCase()
	if (!cryptoEnabled || (m !== 'POST' && m !== 'PUT')) return null
	if (data === undefined || data === null) return null

	const plain =
		typeof data === 'string' ? data : JSON.stringify(data === '' ? {} : data)

	const mode = (GATEWAY_CRYPTO_MODE || 'rsa').toLowerCase()

	if (mode === 'rsa') {
		const pem = toPemPublicKey(GATEWAY_RSA_PUBLIC_KEY_BASE64)
		if (!pem) {
			throw new Error('[crypto] 请在 config/env.js 配置 GATEWAY_RSA_PUBLIC_KEY_BASE64')
		}
		const aesKeyB64 = CryptoJS.lib.WordArray.random(32).toString(CryptoJS.enc.Base64)
		const ivB64 = CryptoJS.lib.WordArray.random(16).toString(CryptoJS.enc.Base64)
		const payload = aesEncryptBase64(plain, aesKeyB64, ivB64)
		const encryptedKey = rsaEncryptUtf8(aesKeyB64, pem)
		return JSON.stringify({ encryptedKey, payload, iv: ivB64 })
	}

	if (mode === 'aes') {
		const secret = String(GATEWAY_AES_SECRET_BASE64 || '').trim()
		if (!secret) {
			throw new Error('[crypto] AES 模式请配置 GATEWAY_AES_SECRET_BASE64')
		}
		const ivB64 = CryptoJS.lib.WordArray.random(16).toString(CryptoJS.enc.Base64)
		const payload = aesEncryptBase64(plain, secret, ivB64)
		return JSON.stringify({ payload, iv: ivB64 })
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

/** 响应为加密包时解密为 JSON 对象 */
export function maybeDecryptResponse(res) {
	const headers = res.header || res.headers || {}
	const lower = {}
	for (const k of Object.keys(headers)) {
		lower[String(k).toLowerCase()] = headers[k]
	}
	const enc = lower[HDR.ENCRYPTED.toLowerCase()]
	if (!enc || String(enc).toLowerCase() !== 'true') {
		return res
	}

	let data = res.data
	if (typeof data === 'string') {
		try {
			data = JSON.parse(data)
		} catch {
			return res
		}
	}
	if (!data || typeof data !== 'object') return res

	try {
		if (data.encryptedKey != null && data.payload != null && data.iv != null) {
			const jsonText = aesDecryptBase64(data.payload, String(data.encryptedKey), String(data.iv))
			res.data = JSON.parse(jsonText)
			return res
		}
		if (data.payload != null && data.iv != null) {
			const secret = String(GATEWAY_AES_SECRET_BASE64 || '').trim()
			if (!secret) return res
			const jsonText = aesDecryptBase64(data.payload, secret, String(data.iv))
			res.data = JSON.parse(jsonText)
			return res
		}
	} catch {
		/* 解密失败时保留原始 data */
	}
	return res
}
