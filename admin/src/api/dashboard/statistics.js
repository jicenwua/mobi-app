import request from '@/utils/request'

/** 首页 / 店铺统计数据 */
export function getShopStatistics(params) {
  return request({
    url: '/shop/statistics',
    method: 'get',
    params
  })
}
