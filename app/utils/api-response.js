/**
 * 网关统一响应解析（与后端 ResponseEntityUtils 一致）
 */

function safeJsonParse(str) {
	try {
		return JSON.parse(str)
	} catch {
		return {}
	}
}

/** 将 uni.request / uploadFile 的 data 转为对象 */
export function unwrapResponseBody(data) {
	if (data == null || data === '') return {}
	if (typeof data === 'string') return safeJsonParse(data)
	if (typeof data === 'object') return data
	return {}
}

/** 业务是否成功：HTTP 200 且 code === 200 */
export function isApiSuccess(body) {
	return body?.code === 200
}

/** HTTP 401 或业务 code 401 */
export function isUnauthorizedResponse(res, body) {
	return res?.statusCode === 401 || body?.code === 401
}

/** HTTP 5xx 服务端异常（如网关解密失败） */
export function isServerErrorResponse(res) {
	const code = Number(res?.statusCode)
	return code >= 500 && code < 600
}

/** 取业务 data 载荷 */
export function pickApiData(body) {
	return body?.data
}

/** 分页列表：从 ResponseEntity 解析 data 数组 */
export function pickApiList(body) {
	if (Array.isArray(body?.data)) return body.data
	if (Array.isArray(body?.data?.records)) return body.data.records
	if (Array.isArray(body?.data?.list)) return body.data.list
	return []
}

/** 解析是否已设置密码（后端字段 setPassword / isSetPassword） */
export function parseSetPasswordFlag(data) {
	if (data == null || typeof data !== 'object') return false
	if (data.setPassword !== undefined) return !!data.setPassword
	if (data.isSetPassword !== undefined) return !!data.isSetPassword
	return false
}

/** 解析登录 data：token / register / nickName / avatar / permission / setPassword */
export function parseLoginData(data) {
	if (data == null) {
		return {
			token: '',
			register: false,
			nickName: '',
			avatar: '',
			permission: [],
			setPassword: false
		}
	}
	if (typeof data === 'string') {
		return {
			token: data,
			register: false,
			nickName: '',
			avatar: '',
			permission: [],
			setPassword: false
		}
	}
	return {
		token: data.token || data.accessToken || '',
		register: !!data.register,
		nickName: data.nickName || data.nickname || '',
		avatar: data.avatar || data.avatarUrl || '',
		permission: data.permission ?? data.permissions ?? [],
		setPassword: parseSetPasswordFlag(data)
	}
}
