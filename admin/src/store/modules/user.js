import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, logout } from '@/api/system/user'
import { loadPersistedUserInfo, savePersistedUserInfo, pickPersistedUserProfile } from '@/utils/userInfoPersist'
import { clearRoleCache } from '@/utils/roleCache'
import { disconnectNotifySocket } from '@/utils/notifySocket'
import { usePermissionStore } from '@/store/modules/permission'
import { TOKEN_STORAGE_KEY, normalizeToken, setStoredToken } from '@/utils/authToken'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(normalizeToken(localStorage.getItem(TOKEN_STORAGE_KEY)))
  /** 仅含可持久化字段（账号、昵称、头像、菜单树）；权限标识在 permission 仓库内存中 */
  const userInfo = ref(loadPersistedUserInfo())

  // 登录
  async function userLogin(loginForm) {
    const res = await login(loginForm)
    const tokenValue = normalizeToken(res.data)
    syncToken(tokenValue)
    return res
  }

  // 登出
  async function userLogout() {
    try {
      await logout()
    } catch {
      /* 登出接口失败仍清本地会话 */
    } finally {
      token.value = ''
      userInfo.value = {}
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      localStorage.removeItem('userInfo')
      clearRoleCache()
      disconnectNotifySocket()
      try {
        usePermissionStore().reset()
      } catch {
        /* Pinia 未初始化时忽略 */
      }
    }
  }

  /**
   * 根据接口返回的完整用户信息，更新内存与 localStorage（仅白名单字段）
   */
  function persistProfileAndMenus(fullApi) {
    const slice = pickPersistedUserProfile(fullApi)
    userInfo.value = slice
    savePersistedUserInfo(slice)
  }

  /** 兼容旧调用：入参可为接口 VO，只持久化白名单 */
  function setUserInfo(info) {
    persistProfileAndMenus(info)
  }

  /** 打开登录页：仅清本地会话，不弹窗、不调 logout 接口 */
  function prepareLoginPage() {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    localStorage.removeItem('userInfo')
    clearRoleCache()
    disconnectNotifySocket()
    token.value = ''
    userInfo.value = {}
    try {
      usePermissionStore().reset()
    } catch {
      /* 忽略 */
    }
  }

  /** 响应头续签 token 时与 localStorage 保持一致 */
  function syncToken(newToken) {
    const normalized = normalizeToken(newToken)
    if (!normalized) {
      return
    }
    token.value = normalized
    setStoredToken(normalized)
  }

  return {
    token,
    userInfo,
    userLogin,
    userLogout,
    setUserInfo,
    persistProfileAndMenus,
    prepareLoginPage,
    syncToken
  }
})
