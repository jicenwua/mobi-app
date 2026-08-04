export function hasCouponThreshold(threshold) {
	return threshold != null && Number(threshold) > 0
}

export function formatCouponDiscountValue(type, value) {
	if (value == null) return '—'
	if (type === 1) {
		const fold = Number(value) / 10
		return Number.isInteger(fold) ? `${fold} 折` : `${Math.round(fold * 10) / 10} 折`
	}
	return `${value} 积分`
}

export function formatCouponRule(item) {
	const valueText = formatCouponDiscountValue(item.type, item.discountValue)
	if (!hasCouponThreshold(item.thresholdAmount)) {
		return item.type === 1 ? `无门槛 ${valueText}` : `无门槛减 ${item.discountValue} 积分`
	}
	if (item.type === 1) {
		return `满 ${item.thresholdAmount} 积分享 ${valueText}`
	}
	return `满 ${item.thresholdAmount} 积分减 ${item.discountValue} 积分`
}

export function formatCouponIssued(item) {
	const issued = item.issuedQuantity || 0
	if (item.totalQuantity == null || item.totalQuantity === '') {
		return `已发 ${issued}/不限`
	}
	return `已发 ${issued}/${item.totalQuantity}`
}

export function formatCouponDistributionPeriod(startTime, endTime) {
	const start = startTime ? formatManageCouponDate(startTime) : '立即开始'
	const end = endTime ? formatManageCouponDate(endTime) : '永久'
	return `${start} ~ ${end}`
}

export function couponStatusLabel(item) {
	const status = item?.distributionStatus
	if (status === 0) return '禁用'
	if (status === 1) return '未开始'
	if (status === 2) return '进行中'
	if (status === 3) return '已结束'
	if (status === 4) return '手动停止'
	return '—'
}

export function couponStatusCode(item) {
	const status = item?.distributionStatus
	if (status === 0 || status === 1 || status === 2 || status === 3 || status === 4) return status
	return null
}

export function couponPhaseTagClass(code) {
	if (code === 1) return 'phase-tag--pending'
	if (code === 2) return 'phase-tag--active'
	if (code === 3) return 'phase-tag--ended'
	if (code === 4) return 'phase-tag--stopped'
	return 'phase-tag--unknown'
}

export function formatValidDays(item) {
	if (item.validDays == null || item.validDays === '') return '领取后永久'
	const days = Number(item.validDays)
	if (Number.isNaN(days) || days <= 0) return '领取后永久'
	return `领取后 ${days} 天`
}

export function formatManageCouponDate(iso) {
	if (!iso) return '—'
	return String(iso).replace('T', ' ').slice(0, 10)
}

export function canEditCoupon(item) {
	const status = item?.distributionStatus
	return status === 1 || status === 2 || status === 4
}

export function canStopCouponDistribution(item) {
	if (!item) return false
	const status = item?.distributionStatus
	return status === 1 || status === 2
}

export function canResumeCouponDistribution(item) {
	if (!item) return false
	return item?.distributionStatus === 4
}

export const COUPON_TYPE_PICKER_LABELS = ['折扣（打折）', '满减（固定减免）']

export function couponTypeLabel(type) {
	if (type === 1) return '折扣'
	if (type === 2) return '满减'
	return '—'
}
