import request from '@/utils/request'

export function listUserCoupon(params) {
  return request({
    url: '/user/coupon/list',
    method: 'get',
    params
  })
}

export function grantUserCoupon(data) {
  return request({
    url: '/user/coupon/grant',
    method: 'post',
    data
  })
}

export function delUserCoupon(userCouponId) {
  return request({
    url: `/user/coupon/${userCouponId}`,
    method: 'delete'
  })
}
