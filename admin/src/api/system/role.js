import request from '@/utils/request'

/**
 * 分页查询角色列表（与后端 GET /system/role/list 对齐：pageNum、pageSize、roleName、roleKey、status）
 */
export function listRole(query) {
  const params = { ...query }
  ;['roleName', 'roleKey', 'status'].forEach((key) => {
    if (params[key] === '' || params[key] == null) {
      delete params[key]
    }
  })
  return request({
    url: '/system/role/list',
    method: 'get',
    params
  })
}

// 查询角色详细
export function getRole(roleId) {
  return request({
    url: `/system/role/${roleId}`,
    method: 'get'
  })
}

// 新增角色
export function addRole(data) {
  return request({
    url: '/system/role',
    method: 'post',
    data: data
  })
}

// 修改角色
export function updateRole(data) {
  return request({
    url: '/system/role',
    method: 'put',
    data: data
  })
}

// 删除角色（支持单个和批量）
export function delRole(roleIds) {
  return request({
    url: `/system/role/${roleIds}`,
    method: 'delete'
  })
}

// 分配菜单权限
export function assignMenus(data) {
  return request({
    url: '/system/role/menus',
    method: 'put',
    params: { roleId: data.roleId },
    data: data.menuIds
  })
}
