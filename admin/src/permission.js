import router from './router'
import { usePermissionStore } from '@/store/modules/permission'
import { useUserStore } from '@/store/modules/user'
import { resetPiniaAuth } from '@/utils/authSession'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/404']

/** 仅登录失效才清会话：HTTP 401 或业务 code 401（网关/网络错误不清 token） */
function isLoginExpiredError(error) {
  if (!error) return false
  if (error.response?.status === 401) return true
  const code = error.code ?? error.response?.data?.code
  return code === 401
}

router.beforeEach(async (to) => {
  NProgress.start()

  const userStore = useUserStore()
  const permissionStore = usePermissionStore()

  document.title = to.meta?.title ? `${to.meta.title} - 后台管理系统` : '后台管理系统'

  if (userStore.token) {
    if (to.path === '/login') {
      NProgress.done()
      return '/'
    }

    try {
      if (permissionStore.routes.length === 0) {
        await permissionStore.generateRoutes()
        return { ...to, replace: true }
      }
      return true
    } catch (error) {
      NProgress.done()
      if (isLoginExpiredError(error)) {
        resetPiniaAuth()
        return `/login?redirect=${encodeURIComponent(to.fullPath)}`
      }
      return false
    }
  }

  if (whiteList.includes(to.path)) {
    return true
  }
  NProgress.done()
  return `/login?redirect=${encodeURIComponent(to.fullPath)}`
})

router.afterEach(() => {
  NProgress.done()
})
