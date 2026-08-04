import { ref } from 'vue'
import { navigateBackDelayed } from '@/utils/navigation.js'

const LEGACY_PERMISSIONS_KEY = 'user_permissions'

/** 当前会话权限（响应式，不落盘） */
const sessionPermissions = ref([])

/** 权限变更计数（兼容已有 computed 显式依赖） */
const permRevision = ref(0)

/** 供页面 computed 订阅权限刷新 */
export function getPermRevision() {
	return permRevision
}

/** 清除历史版本写入的权限缓存 */
function purgeLegacyPermissionsStorage() {
	try {
		uni.removeStorageSync(LEGACY_PERMISSIONS_KEY)
	} catch {
		/* 忽略 */
	}
}

purgeLegacyPermissionsStorage()

/** 规范化为字符串数组（支持登录返回的 permission.wxUser 对象） */
export function normalizePermissions(raw) {
	if (!raw) return []
	if (Array.isArray(raw)) return raw.map((p) => String(p)).filter(Boolean)
	if (typeof raw === 'object') {
		const flat = []
		for (const val of Object.values(raw)) {
			if (Array.isArray(val)) {
				flat.push(...val.map((p) => String(p)))
			} else if (typeof val === 'string' && val) {
				flat.push(val)
			}
		}
		return flat.filter(Boolean)
	}
	return []
}

/** 设置当前会话权限（仅内存，不落盘） */
export function setSessionPermissions(raw) {
	sessionPermissions.value = normalizePermissions(raw)
	permRevision.value += 1
	return sessionPermissions.value
}

/** 清空当前会话权限 */
export function clearSessionPermissions() {
	sessionPermissions.value = []
	permRevision.value += 1
}

/** 读取当前会话权限 */
export function getPermissions() {
	return sessionPermissions.value
}

/** 是否拥有指定权限标识 */
export function hasPermission(perm) {
	if (!perm) return false
	const list = sessionPermissions.value
	return list.includes(perm) || list.includes('*:*:*')
}

/** 是否拥有任一权限（用于 Tab、组合能力判断） */
export function hasAnyPermission(...perms) {
	return perms.some((p) => hasPermission(p))
}

/**
 * 无权限时提示并返回上一页
 * @returns {boolean} 是否通过校验
 */
export function assertPermission(perm, message = '无操作权限') {
	if (hasPermission(perm)) return true
	uni.showToast({ title: message, icon: 'none' })
	navigateBackDelayed(800, 1)
	return false
}
