import request from '@/utils/request'

export function getTicketUnreadSummary() {
  return request({
    url: '/ticket/unread-summary',
    method: 'get'
  })
}

export function listTicket(params) {
  return request({
    url: '/ticket/list',
    method: 'get',
    params
  })
}

export function listMyTicket(params) {
  return request({
    url: '/ticket/mine',
    method: 'get',
    params
  })
}

export function getTicketDetail(ticketId) {
  return request({
    url: `/ticket/${ticketId}`,
    method: 'get'
  })
}

export function claimTicket(ticketId) {
  return request({
    url: `/ticket/${ticketId}/claim`,
    method: 'post'
  })
}

export function replyTicket(ticketId, data) {
  return request({
    url: `/ticket/${ticketId}/reply`,
    method: 'post',
    data
  })
}

export function completeTicket(ticketId) {
  return request({
    url: `/ticket/${ticketId}/complete`,
    method: 'post'
  })
}
