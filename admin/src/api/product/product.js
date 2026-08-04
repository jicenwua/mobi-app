import request from '@/utils/request'

export function listProduct(params) {
  return request({
    url: '/product/list',
    method: 'get',
    params
  })
}

export function updateProductStatus(data) {
  return request({
    url: '/product/status',
    method: 'put',
    data
  })
}
