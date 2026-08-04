const POINTS_DECIMALS = 2

/** 积分保留两位小数并向上取整（如 5.673 → 5.68） */
export function ceilPointsToDecimals(value, decimals = POINTS_DECIMALS) {
	const n = Number(value)
	if (Number.isNaN(n)) return 0
	const factor = 10 ** decimals
	return Math.ceil(n * factor) / factor
}

/**
 * 计算使用优惠券后的应付积分（与后端 CouponSettlementCalculator 一致）。
 * @param {number} originalPoints 商品原价积分合计
 * @param {number|null} couponType 1-折扣 2-满减
 * @param {number|null} discountValue 折扣力度或减免积分
 */
export function calculateCouponPayablePoints(originalPoints, couponType, discountValue) {
	const original = Number(originalPoints)
	if (!original || original <= 0) return 0
	if (!couponType || discountValue == null || Number(discountValue) <= 0) {
		return ceilPointsToDecimals(original)
	}
	if (couponType === 1) {
		return ceilPointsToDecimals((original * Number(discountValue)) / 100)
	}
	if (couponType === 2) {
		return ceilPointsToDecimals(Math.max(0, original - Number(discountValue)))
	}
	return ceilPointsToDecimals(original)
}

/** 展示积分金额：固定保留两位小数（如 100 → 100.00） */
export function formatPointsAmount(value) {
	const n = Number(value)
	if (Number.isNaN(n)) return '0.00'
	return n.toFixed(POINTS_DECIMALS)
}

/** 判断订单原价是否满足优惠券门槛（与后端 CouponSettlementCalculator 一致） */
export function meetsCouponThreshold(thresholdAmount, originalPoints) {
	const original = Number(originalPoints)
	if (!original || original <= 0) return false
	const threshold = Number(thresholdAmount)
	if (!threshold || threshold <= 0) return true
	return original >= threshold
}

/** 计算优惠券抵扣积分（原价 − 应付） */
export function calculateCouponDiscountPoints(originalPoints, payablePoints) {
	const original = Number(originalPoints)
	const payable = Number(payablePoints)
	if (Number.isNaN(original) || Number.isNaN(payable)) return 0
	const discount = original - payable
	return discount < 0 ? 0 : ceilPointsToDecimals(discount)
}

/**
 * 计算商品行积分（与后端 PurchaseSettlementService 一致：单价×数量，四舍五入为整数）。
 */
export function calculateLinePoints(price, count) {
	const p = Number(price)
	const c = Number(count)
	if (Number.isNaN(p) || Number.isNaN(c) || c <= 0) return 0
	return Math.round(p * c)
}

export function calculateUnitPoints(price) {
	const p = Number(price)
	if (Number.isNaN(p)) return 0
	return Math.round(p)
}
