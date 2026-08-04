import request from '@/utils/request'

// 查询菜单详细
export function getMenu(menuId) {
  const id = Number(menuId)
  if (!Number.isFinite(id)) {
    return Promise.reject(new Error('无效菜单 ID'))
  }
  return request({
    url: `/system/menu/${id}`,
    method: 'get'
  })
}

// 查询菜单下拉树结构
export function treeselect() {
  return request({
    url: '/system/menu/tree',
    method: 'get'
  })
}

// 管理端菜单扁平列表（含隐藏/停用）
export function listAdminMenus() {
  return request({
    url: '/system/menu/admin/list',
    method: 'get'
  })
}

// 根据角色ID查询菜单下拉树结构（后端暂无此接口，可使用 list 接口替代）
// export function roleMenuTreeselect(roleId) {
//   return request({
//     url: `/system/menu/roleMenuTreeselect/${roleId}`,
//     method: 'get'
//   })
// }

// 新增菜单
export function addMenu(data) {
  return request({
    url: '/system/menu',
    method: 'post',
    data: data
  })
}

// 修改菜单
export function updateMenu(data) {
  return request({
    url: '/system/menu',
    method: 'put',
    data: data
  })
}

// 删除菜单（单个 id 或 id 数组，后端路径支持 1,2,3）
export function delMenu(menuIds) {
  const idStr = Array.isArray(menuIds) ? menuIds.join(',') : String(menuIds)
  return request({
    url: `/system/menu/${idStr}`,
    method: 'delete'
  })
}
