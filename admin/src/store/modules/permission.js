import {defineStore} from 'pinia'
import {ref} from 'vue'
import router from '@/router'
import {constantRoutes} from '@/router'
import { getUserInfo } from '@/api/system/user'
import { useUserStore } from '@/store/modules/user'
import Layout from '@/layout/index.vue'

// 使用 import.meta.glob 预加载所有 views 下的组件
const modules = import.meta.glob('@/views/**/*.vue')

/** 路由 name 必须全局唯一（库内存在多个同名「用户管理」菜单） */
function getRouteName(menu) {
    if (menu.menuId != null) {
        return `Menu${menu.menuId}`
    }
    const path = (menu.path || '').replace(/\//g, '_').replace(/^_/, '') || 'unknown'
    return `Route_${path}`
}

/** 表单/详情/审核等页不进入 keep-alive */
function shouldNoCachePage(component) {
    if (!component) return false
    const last = component.replace(/^\//, '').split('/').pop()
    return ['form', 'detail', 'exec', 'handle'].includes(last)
}

/** 子路由 path 转为相对父级，避免嵌套匹配异常 */
function normalizeChildPath(parentPath, childPath) {
    if (!childPath) return ''
    const parent = (parentPath || '').replace(/\/$/, '')
    const child = childPath.startsWith('/') ? childPath : `/${childPath}`
    if (parent && child.startsWith(`${parent}/`)) {
        return child.slice(parent.length + 1)
    }
    return childPath.replace(/^\//, '')
}

export const usePermissionStore = defineStore('permission', () => {
    // 状态
    const routes = ref([])
    const addRoutes = ref([])
    const permissions = ref([])
    /** 变更后递增，用于强制重渲染侧栏与页面（权限指令等） */
    const routeRenderKey = ref(0)

    // 获取用户信息和菜单权限
    async function generateRoutes() {
        const userStore = useUserStore()
        const userInfoRes = await getUserInfo()
        const full = userInfoRes?.data
        if (!full) {
            throw new Error('获取用户信息失败')
        }

        const menus = Array.isArray(full.menus) ? full.menus : []
        if (!menus.length) {
            throw new Error('用户菜单为空')
        }

        // 权限标识仅内存，不落 localStorage
        permissions.value = full.permissions || []

        // 仅持久化昵称、头像、账号、菜单等白名单字段
        userStore.persistProfileAndMenus(full)

        // 从菜单生成路由
        const accessedRoutes = generateMenuRoutes(menus)

        // 设置路由
        addRoutes.value = accessedRoutes
        routes.value = constantRoutes.concat(accessedRoutes)

        // 动态添加路由（登出时会 removeRoute，避免重复注册）
        accessedRoutes.forEach(route => {
            addRouteRecursive(route)
        })

        return accessedRoutes
    }

    // 根据菜单生成路由
    function generateMenuRoutes(menus, parentPath = '') {
        const routes = []

        menus.forEach(menu => {
            // 跳过按钮类型菜单（F），只处理目录（M）和菜单（C）
            if (menu.menuType === 'F') {
                return
            }

            const isChild = !!parentPath
            const fullPath = menu.path || ''
            const routePath = isChild ? normalizeChildPath(parentPath, fullPath) : fullPath

            const hidden = String(menu.visible) === '1'
            const route = {
                path: routePath,
                name: getRouteName(menu),
                component: null,
                hidden,
                meta: {
                    title: (menu.menuName || '').trim() || '未命名菜单',
                    icon: menu.icon || '',
                    permissions: menu.perms ? [menu.perms] : [],
                    hidden,
                    noCache: shouldNoCachePage(menu.component)
                }
            }

            // 页面菜单必须配置 component，否则不生成路由（避免侧栏空白菜单项）
            if (menu.menuType === 'C' && !menu.component?.trim()) {
                return
            }

            // 动态加载组件：仅当页面文件存在时才注册路由
            if (menu.component && menu.component !== '') {
                const componentPath = menu.component.replace(/^\//, '')
                const viewFile = `/src/views/${componentPath}.vue`

                if (!modules[viewFile]) {
                    return
                }
                route.component = modules[viewFile]
            } else if (menu.children && menu.children.length > 0) {
                route.component = Layout
            }

            if (menu.children && menu.children.length > 0) {
                route.children = generateMenuRoutes(menu.children, fullPath)
                if (!route.children.length && !menu.component?.trim()) {
                    return
                }
                const firstLeaf = findFirstLeafRoute(route.children)
                if (firstLeaf && route.component === Layout) {
                    const parentFull = fullPath.replace(/\/$/, '')
                    route.redirect = parentFull
                        ? `${parentFull}/${firstLeaf.path}`.replace(/\/+/g, '/')
                        : `/${firstLeaf.path}`.replace(/\/+/g, '/')
                }
            }

            if (!route.component) {
                return
            }

            routes.push(route)
        })

        return routes
    }

    function findFirstLeafRoute(children) {
        if (!children?.length) return null
        for (const child of children) {
            if (child.meta?.hidden) continue
            if (child.children?.length) {
                const nested = findFirstLeafRoute(child.children)
                if (nested) return nested
            }
            return child
        }
        return null
    }

    function addRouteRecursive(route) {
        if (route.name && router.hasRoute(route.name)) {
            router.removeRoute(route.name)
        }
        router.addRoute(route)
    }

    function removeRouteRecursive(routeList) {
        if (!routeList?.length) return
        routeList.forEach(route => {
            if (route.children?.length) {
                removeRouteRecursive(route.children)
            }
            if (route.name && router.hasRoute(route.name)) {
                router.removeRoute(route.name)
            }
        })
    }

    // 设置权限标识
    function setPermissions(perms) {
        permissions.value = perms
    }

    /**
     * 后端续签 token 或权限变更后：移除旧动态路由并重新拉取菜单/权限
     */
    async function refreshDynamicRoutes() {
        const previousPaths = collectRoutePaths(addRoutes.value)
        removeRouteRecursive(addRoutes.value)
        addRoutes.value = []
        routes.value = [...constantRoutes]
        const accessedRoutes = await generateRoutes()
        const nextPaths = collectRoutePaths(accessedRoutes)
        if (!pathsEqual(previousPaths, nextPaths)) {
            routeRenderKey.value += 1
        }
        return accessedRoutes
    }

    function collectRoutePaths(routeList, base = '') {
        const paths = []
        if (!routeList?.length) return paths
        routeList.forEach((route) => {
            const full = [base, route.path].filter(Boolean).join('/').replace(/\/+/g, '/')
            paths.push(full || '/')
            if (route.children?.length) {
                paths.push(...collectRoutePaths(route.children, full))
            }
        })
        return paths
    }

    function pathsEqual(left, right) {
        if (left.length !== right.length) return false
        const a = [...left].sort()
        const b = [...right].sort()
        return a.every((item, index) => item === b[index])
    }

    // 重置（登出时移除动态路由）
    function reset() {
        removeRouteRecursive(addRoutes.value)
        routes.value = []
        addRoutes.value = []
        permissions.value = []
        routeRenderKey.value = 0
    }

    return {
        routes,
        addRoutes,
        permissions,
        routeRenderKey,
        generateRoutes,
        refreshDynamicRoutes,
        setPermissions,
        reset
    }
})
