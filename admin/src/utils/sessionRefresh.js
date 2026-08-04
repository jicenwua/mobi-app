import router from '@/router'
import { usePermissionStore } from '@/store/modules/permission'
import { clearRoleCache } from '@/utils/roleCache'
import { reconnectNotifySocket } from '@/utils/notifySocket'
import { ElMessage } from 'element-plus'

let refreshPromise = null

/**
 * token 变更后：重新拉取用户信息、重建动态路由与权限，并刷新当前视图
 */
export function refreshSessionAfterTokenChange() {
  if (refreshPromise) {
    return refreshPromise
  }

  refreshPromise = (async () => {
    const permissionStore = usePermissionStore()
    await permissionStore.refreshDynamicRoutes()
    clearRoleCache()
    reconnectNotifySocket()

    const current = router.currentRoute.value
    if (current.name && !router.hasRoute(current.name)) {
      await router.replace('/dashboard')
      ElMessage.warning('当前页面权限已变更，已返回首页')
    }
  })()
    .finally(() => {
      refreshPromise = null
    })

  return refreshPromise
}
