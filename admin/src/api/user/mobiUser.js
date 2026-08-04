import request from '@/utils/request'

export function listMobiUser(params) {
  return request({
    url: '/user/mobi-user/list',
    method: 'get',
    params
  })
}

export function getMobiUser(userId) {
  return request({
    url: `/user/mobi-user/${userId}`,
    method: 'get'
  })
}

export function updateMobiUser(data) {
  return request({
    url: '/user/mobi-user',
    method: 'put',
    data
  })
}
