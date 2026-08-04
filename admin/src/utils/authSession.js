import { ElMessageBox } from 'element-plus'
import router from '@/router'
import { TOKEN_STORAGE_KEY } from '@/utils/authToken'
import { clearRoleCache } from '@/utils/roleCache'
import { disconnectNotifySocket } from '@/utils/notifySocket'
import { usePermissionStore } from '@/store/modules/permission'
import { useUserStore } from '@/store/modules/user'

/** 无需携带 token 的接口路径片段 */
export const PUBLIC_API_PATHS = ['/system/auth/login', '/code']

let reloginPromptPromise = null

export function isPublicApiRequest(config) {
  if (config?.skipToken) {
    return true
  }
  const url = config?.url || ''
  return PUBLIC_API_PATHS.some((path) => url.includes(path))
}

export function isLoginRoute() {
  return router.currentRoute.value?.path === '/login'
}

/** 清除 localStorage / session 缓存（Pinia 由调用方或 resetPiniaAuth 处理） */
export function clearAuthStorage() {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
  localStorage.removeItem('userInfo')
  sessionStorage.removeItem('admin-tags-view')
  clearRoleCache()
}

/** 同步清空 Pinia 中的用户与路由状态 */
export function resetPiniaAuth() {
  clearAuthStorage()
  disconnectNotifySocket()
  try {
    const userStore = useUserStore()
    userStore.token = ''
    userStore.userInfo = {}
    usePermissionStore().reset()
  } catch {
    /* Pinia 未就绪时忽略 */
  }
}

/**
 * 统一处理 401：立即清 token；已在登录页则不再弹窗；全局只弹一次确认框
 */
export function handleSessionExpired(message = '登录已过期，请重新登录') {
  resetPiniaAuth()

  if (isLoginRoute()) {
    return Promise.resolve()
  }

  if (reloginPromptPromise) {
    return reloginPromptPromise
  }

  reloginPromptPromise = ElMessageBox.confirm(message, '提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning',
    closeOnClickModal: false
  })
    .then(() => {
      if (!isLoginRoute()) {
        return router.push(`/login?redirect=${encodeURIComponent(router.currentRoute.value.fullPath)}`)
      }
    })
    .catch(() => {})
    .finally(() => {
      reloginPromptPromise = null
    })

  return reloginPromptPromise
}

export function isUnauthorizedResponse(res, error) {
  if (error?.response?.status === 401) {
    return true
  }
  const code = error?.code ?? res?.code ?? error?.response?.data?.code
  return code === 401
}
