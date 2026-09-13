/**
 * 微信小程序一次 uploadFile 只能传一个文件；
 * 多文件 multipart 需手动拼 body，用 uni.request 提交。
 */
import { runWithDedupe } from '@/utils/request-dedupe.js'

function randomBoundary() {
	return `----UniForm${Date.now().toString(16)}${Math.random().toString(16).slice(2)}`
}

function utf8Bytes(str) {
	if (typeof TextEncoder !== 'undefined') {
		return new TextEncoder().encode(str)
	}
	const utf8 = unescape(encodeURIComponent(str))
	const arr = new Uint8Array(utf8.length)
	for (let i = 0; i < utf8.length; i++) arr[i] = utf8.charCodeAt(i)
	return arr
}

function concatBuffers(chunks) {
	let total = 0
	for (const c of chunks) total += c.byteLength
	const out = new Uint8Array(total)
	let offset = 0
	for (const c of chunks) {
		out.set(new Uint8Array(c), offset)
		offset += c.byteLength
	}
	return out.buffer
}

function buildPartHeader(boundary, name, filename, contentType) {
	let header = `--${boundary}\r\nContent-Disposition: form-data; name="${name}"`
	if (filename) header += `; filename="${filename}"`
	header += '\r\n'
	if (contentType) header += `Content-Type: ${contentType}\r\n`
	header += '\r\n'
	return utf8Bytes(header)
}

function guessFilename(filePath, fallback = 'file.jpg') {
	const seg = String(filePath || '')
		.split(/[/\\]/)
		.filter(Boolean)
		.pop()
	return seg || fallback
}

function base64ToArrayBuffer(base64) {
	const raw = String(base64 || '').replace(/^data:\w+\/[\w+.-]+;base64,/, '')
	// #ifdef MP-WEIXIN
	if (typeof wx !== 'undefined' && typeof wx.base64ToArrayBuffer === 'function') {
		return wx.base64ToArrayBuffer(raw)
	}
	// #endif
	if (typeof uni !== 'undefined' && typeof uni.base64ToArrayBuffer === 'function') {
		return uni.base64ToArrayBuffer(raw)
	}
	const binary = atob(raw)
	const bytes = new Uint8Array(binary.length)
	for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
	return bytes.buffer
}

/** 将 readFile 的 data 统一为 ArrayBuffer（兼容各端返回类型） */
function toArrayBuffer(data) {
	if (data == null) return null
	if (data instanceof ArrayBuffer) return data
	if (ArrayBuffer.isView(data)) {
		return data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength)
	}
	if (typeof data === 'string') {
		try {
			return base64ToArrayBuffer(data)
		} catch {
			return null
		}
	}
	return null
}

function readFileArrayBuffer(filePath) {
	const fs = uni.getFileSystemManager()
	return new Promise((resolve, reject) => {
		const readAsBase64 = () => {
			fs.readFile({
				filePath,
				encoding: 'base64',
				success: (res) => {
					try {
						resolve(base64ToArrayBuffer(res.data))
					} catch {
						reject(new Error('读取文件格式不支持'))
					}
				},
				fail: (e) => reject(new Error(e.errMsg || '读取文件失败'))
			})
		}

		fs.readFile({
			filePath,
			success: (res) => {
				const buf = toArrayBuffer(res.data)
				if (buf) {
					resolve(buf)
					return
				}
				readAsBase64()
			},
			fail: () => readAsBase64()
		})
	})
}

/**
 * @param {{ url: string, parts: Array<{ name: string, filePath?: string, data?: string|ArrayBuffer, filename?: string, contentType?: string }>, header?: object, timeout?: number }} opts
 */
export function uploadMultipartForm(opts) {
	const dedupeConfig = {
		method: 'POST',
		url: opts.url,
		data: '[multipart]'
	}

	return runWithDedupe(
		() =>
			new Promise(async (resolve, reject) => {
				try {
					const boundary = randomBoundary()
					const chunks = []

					for (const part of opts.parts || []) {
						const { name, filePath, data, filename, contentType, text } = part
						if (filePath) {
							const fn = filename || guessFilename(filePath)
							const ct = contentType || 'image/jpeg'
							chunks.push(buildPartHeader(boundary, name, fn, ct))
							chunks.push(await readFileArrayBuffer(filePath))
						} else if (data != null) {
							if (text) {
								chunks.push(buildPartHeader(boundary, name, null, null))
							} else {
								const fn = filename || 'part.dat'
								const ct = contentType || 'application/octet-stream'
								chunks.push(buildPartHeader(boundary, name, fn, ct))
							}
							chunks.push(typeof data === 'string' ? utf8Bytes(data) : data)
						} else {
							continue
						}
						chunks.push(utf8Bytes('\r\n'))
					}
					chunks.push(utf8Bytes(`--${boundary}--\r\n`))

					const body = concatBuffers(chunks)
					uni.request({
						url: opts.url,
						method: opts.method || 'POST',
						header: {
							...(opts.header || {}),
							'Content-Type': `multipart/form-data; boundary=${boundary}`
						},
						data: body,
						timeout: opts.timeout || 120000,
						success: (res) => resolve(res),
						fail: (err) => reject(new Error(err.errMsg || '上传失败'))
					})
				} catch (e) {
					reject(e)
				}
			}),
		dedupeConfig
	)
}
