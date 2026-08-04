import request from '@/utils/request'

export function listPointsAccount(params) {
  return request({
    url: '/points/account/list',
    method: 'get',
    params
  })
}

export function listConsumeLog(params) {
  return request({
    url: '/points/log/list',
    method: 'get',
    params
  })
}

export function getConsumeLogDetail(logId, shopId) {
  return request({
    url: `/points/log/${logId}`,
    method: 'get',
    params: shopId != null && shopId !== '' ? { shopId } : undefined
  })
}

export function adjustPointsAccount(data) {
  return request({
    url: '/points/account/adjust',
    method: 'post',
    data
  })
}

