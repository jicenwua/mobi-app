import request from '@/utils/request'

export function listShopCoupon(params) {
  return request({
    url: '/shop/coupon/list',
    method: 'get',
    params
  })
}

export function getShopCoupon(templateId) {
  return request({
    url: `/shop/coupon/${templateId}`,
    method: 'get'
  })
}

export function addShopCoupon(data) {
  return request({
    url: '/shop/coupon',
    method: 'post',
    data
  })
}

export function updateShopCoupon(data) {
  return request({
    url: '/shop/coupon',
    method: 'put',
    data
  })
}

export function delShopCoupon(templateId) {
  return request({
    url: `/shop/coupon/${templateId}`,
    method: 'delete'
  })
}

export function stopShopCoupon(templateId) {
  return request({
    url: `/shop/coupon/${templateId}/stop`,
    method: 'put'
  })
}

export function resumeShopCoupon(templateId) {
  return request({
    url: `/shop/coupon/${templateId}/resume`,
    method: 'put'
  })
}
