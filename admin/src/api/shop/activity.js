import request from '@/utils/request'

export function listShopActivity(params) {
  return request({
    url: '/shop/activity/list',
    method: 'get',
    params
  })
}

export function getShopActivity(id) {
  return request({
    url: `/shop/activity/${id}`,
    method: 'get'
  })
}

export function saveShopActivity(data) {
  return request({
    url: '/shop/activity',
    method: 'post',
    data
  })
}

export function delShopActivity(id) {
  return request({
    url: `/shop/activity/${id}`,
    method: 'delete'
  })
}

export function stopShopActivity(id) {
  return request({
    url: `/shop/activity/${id}/stop`,
    method: 'put'
  })
}

export function enableShopActivity(id) {
  return request({
    url: `/shop/activity/${id}/enable`,
    method: 'put'
  })
}
