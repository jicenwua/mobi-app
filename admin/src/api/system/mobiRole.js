import request from '@/utils/request'

export function listMobiRole(params) {
  return request({
    url: '/system/mobi-role/list',
    method: 'get',
    params
  })
}

export function getMobiRole(id) {
  return request({
    url: `/system/mobi-role/${id}`,
    method: 'get'
  })
}

export function addMobiRole(data) {
  return request({
    url: '/system/mobi-role',
    method: 'post',
    data
  })
}

export function updateMobiRole(data) {
  return request({
    url: '/system/mobi-role',
    method: 'put',
    data
  })
}

export function delMobiRole(ids) {
  return request({
    url: `/system/mobi-role/${ids}`,
    method: 'delete'
  })
}

export function reloadMobiRoleCache() {
  return request({
    url: '/system/mobi-role/reload-cache',
    method: 'post'
  })
}
