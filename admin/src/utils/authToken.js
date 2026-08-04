/** localStorage 中登录 token 的键名 */
export const TOKEN_STORAGE_KEY = 'token'

/**
 * 规范化 token（去掉 Bearer 前缀与首尾空格）
 */
export function normalizeToken(raw) {
  if (raw == null || raw === '') {
    return ''
  }
  const value = String(raw).trim()
  if (/^bearer\s+/i.test(value)) {
    return value.replace(/^bearer\s+/i, '').trim()
  }
  return value
}

export function getStoredToken() {
  return normalizeToken(localStorage.getItem(TOKEN_STORAGE_KEY))
}

/**
 * 写入 token；若与旧值不同返回 true
 */
export function setStoredToken(token) {
  const normalized = normalizeToken(token)
  if (!normalized) {
    return false
  }
  const previous = getStoredToken()
  localStorage.setItem(TOKEN_STORAGE_KEY, normalized)
  return previous !== normalized
}

/**
 * 从 axios 响应头读取后端续签/权限刷新后的 token（HeaderAuthenticationFilter 写入 authorization）
 */
export function extractTokenFromResponse(response) {
  if (!response?.headers) {
    return ''
  }
  const headers = response.headers
  const raw =
    headers.authorization ??
    headers.Authorization ??
    (typeof headers.get === 'function' ? headers.get('authorization') || headers.get('Authorization') : '')
  return normalizeToken(raw)
}
