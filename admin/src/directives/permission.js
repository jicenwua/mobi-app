import { usePermissionStore } from '@/store/modules/permission'

function applyPermission(el, binding) {
  const { value } = binding
  const permissionStore = usePermissionStore()
  const permissions = permissionStore.permissions

  if (!value || !Array.isArray(value) || value.length === 0) {
    throw new Error('请设置权限标签值')
  }

  const hasPermission = permissions.some((permission) => value.includes(permission))
  el.style.display = hasPermission ? '' : 'none'
}

/**
 * 权限判断指令
 * 使用方式: v-hasPermi="['system:user:add']"
 */
export default {
  mounted(el, binding) {
    applyPermission(el, binding)
  },
  updated(el, binding) {
    applyPermission(el, binding)
  }
}
