import { listRole } from '@/api/system/role'

const ROLE_CACHE_KEY = 'role_options_cache'

/** 移除旧版写在 localStorage 的缓存，避免跨窗口复用 */
function clearLegacyLocalStorageCache() {
  try {
    localStorage.removeItem(ROLE_CACHE_KEY)
  } catch {
    /* ignore */
  }
}

/**
 * 获取角色列表缓存（当前浏览器标签页 sessionStorage，新窗口需重新请求）
 */
export function getRoleCache() {
  clearLegacyLocalStorageCache()
  try {
    const cacheStr = sessionStorage.getItem(ROLE_CACHE_KEY)
    if (!cacheStr) return null
    const cache = JSON.parse(cacheStr)
    return cache.data
  } catch {
    clearRoleCache()
    return null
  }
}

/**
 * 设置角色列表缓存（仅当前标签页有效）
 */
export function setRoleCache(data) {
  try {
    const cache = { data }
    sessionStorage.setItem(ROLE_CACHE_KEY, JSON.stringify(cache))
  } catch {
    /* sessionStorage 不可用时忽略 */
  }
}

/**
 * 清除角色列表缓存
 */
export function clearRoleCache() {
  try {
    sessionStorage.removeItem(ROLE_CACHE_KEY)
    clearLegacyLocalStorageCache()
  } catch {
    /* sessionStorage 不可用时忽略 */
  }
}

/**
 * 获取角色列表（带会话级缓存）
 * 优先从 sessionStorage 读取，否则请求接口
 */
export async function getRoleOptions() {
  const cachedData = getRoleCache()
  if (cachedData) {
    return cachedData
  }

  try {
    const res = await listRole({ pageNum: 1, pageSize: 9999 })

    let roleList = []
    if (Array.isArray(res.data)) {
      roleList = res.data
    } else if (res.data?.records && Array.isArray(res.data.records)) {
      roleList = res.data.records
    }

    const filteredList = roleList.map((role) => {
      const roleId = role.roleId ?? role.id
      return {
        id: roleId,
        roleId,
        roleName: role.roleName
      }
    })
    setRoleCache(filteredList)

    return filteredList
  } catch (error) {
    return []
  }
}
