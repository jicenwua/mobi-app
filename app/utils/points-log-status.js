/** 积分流水状态（与后端 mobi_points_log.status 一致） */
export const POINTS_LOG_STATUS = {
	VERIFIED: 1,
	CANCELLED: 2,
	EXPIRED: 3,
	RECHARGE: 4,
	PENDING_USE: 5,
	ADMIN_ADD: 6,
	ADMIN_DEDUCT: 7
}

export const POINTS_LOG_STATUS_LABELS = {
	[POINTS_LOG_STATUS.VERIFIED]: '已核销',
	[POINTS_LOG_STATUS.CANCELLED]: '已退款',
	[POINTS_LOG_STATUS.EXPIRED]: '订单过期',
	[POINTS_LOG_STATUS.RECHARGE]: '积分充值',
	[POINTS_LOG_STATUS.PENDING_USE]: '待使用',
	[POINTS_LOG_STATUS.ADMIN_ADD]: '后台添加',
	[POINTS_LOG_STATUS.ADMIN_DEDUCT]: '后台扣除'
}

export function pointsLogStatusLabel(status) {
	return POINTS_LOG_STATUS_LABELS[status] || '—'
}

/** 状态标签样式后缀（与 POINTS_LOG_STATUS 对应） */
export const POINTS_LOG_STATUS_TAG = {
	[POINTS_LOG_STATUS.VERIFIED]: 'verified',
	[POINTS_LOG_STATUS.CANCELLED]: 'cancelled',
	[POINTS_LOG_STATUS.EXPIRED]: 'expired',
	[POINTS_LOG_STATUS.RECHARGE]: 'recharge',
	[POINTS_LOG_STATUS.PENDING_USE]: 'pending',
	[POINTS_LOG_STATUS.ADMIN_ADD]: 'admin-add',
	[POINTS_LOG_STATUS.ADMIN_DEDUCT]: 'admin-deduct'
}

export function pointsLogStatusTagClass(status) {
	return POINTS_LOG_STATUS_TAG[status] || 'unknown'
}

/** 是否为积分增加类流水 */
export function isPointsLogGain(row) {
	if (!row) return false
	if (row.actionType === 1) return true
	const status = row.status
	return status === POINTS_LOG_STATUS.RECHARGE || status === POINTS_LOG_STATUS.ADMIN_ADD
}

/** 积分变动绝对值（列表接口可能不含 changeBase/changeBonus，回退 consumePoints） */
export function pointsLogChangeAmount(row) {
	if (!row) return 0
	const base = Math.abs(row.changeBase ?? 0)
	const bonus = Math.abs(row.changeBonus ?? 0)
	const fromChanges = base + bonus
	if (fromChanges > 0) return fromChanges
	return Math.abs(row.consumePoints ?? 0)
}
