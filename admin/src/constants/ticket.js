/** 与后端 Constants.TICKET_STAFF_AVATAR_URL 一致 */
export const TICKET_STAFF_AVATAR_URL =
  'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

export const DEFAULT_USER_AVATAR_URL =
  'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534eecb7181.png'

export function resolveTicketAvatar(url, isStaff) {
  if (isStaff) return TICKET_STAFF_AVATAR_URL
  return url || DEFAULT_USER_AVATAR_URL
}

/** 工单状态文案 */
export function ticketStatusLabel(status) {
  if (status === 1) return '处理中'
  if (status === 2) return '已完成'
  return '待处理'
}

/** 工单状态 Tag 类型 */
export function ticketStatusTag(status) {
  if (status === 1) return 'warning'
  if (status === 2) return 'info'
  return 'danger'
}
