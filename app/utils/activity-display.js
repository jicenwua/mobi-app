import { formatDateTime } from '@/utils/datetime-format.js'

export function formatActivityTimeRange(activity) {
	const start = formatDateTime(activity?.startTime, { maxLen: 16 })
	const end = formatDateTime(activity?.endTime, { maxLen: 16 })
	if (start === '—' && end === '—') return '—'
	return `${start} 至 ${end}`
}

export function formatActivityRule(rule, activityType) {
	const threshold = rule?.thresholdAmount ?? rule?.thresholdPoints
	const gift = rule?.giftPoints
	if (threshold == null || gift == null) return '规则配置不完整'
	const action = activityType === 2 ? '消费满' : '充值满'
	return `${action} ${threshold} 积分，赠送 ${gift} 积分`
}
