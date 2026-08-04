import { ref } from 'vue'
import {
	fetchManageCoupons,
	addManageCoupon,
	addManageCouponStock,
	adjustManageCouponDistribution,
	stopManageCouponDistribution,
	resumeManageCouponDistribution,
	deleteManageCoupon,
	dateToDateTime,
	dateToDateTimeEnd,
	parsePositiveAmount,
	parsePositiveInt,
	parseOptionalAmount,
	parseOptionalPositiveInt
} from '@/api/modules/shop-manage.js'
import {
	formatCouponRule,
	formatValidDays,
	formatManageCouponDate,
	canEditCoupon,
	canStopCouponDistribution,
	canResumeCouponDistribution,
	COUPON_TYPE_PICKER_LABELS
} from '@/utils/coupon-manage-display.js'

function emptyCouponForm() {
	return {
		couponName: '',
		type: 1,
		thresholdAmount: '',
		discountValue: '',
		validDays: '',
		neverExpire: false,
		totalQuantity: '',
		distributionStartDate: '',
		distributionEndDate: ''
	}
}

/**
 * 店铺管理页 — 折扣券 CRUD 与表单
 */
export function useShopManageCoupons(shopId, { isManager, listLoading }) {
	const coupons = ref([])
	const couponStockVisible = ref(false)
	const couponStockTarget = ref(null)
	const couponStockForm = ref({ quantity: '' })
	const couponStockSubmitting = ref(false)
	const couponFormVisible = ref(false)
	const editingCouponId = ref(null)
	const editingCouponItem = ref(null)
	const couponTypeIndex = ref(0)
	const couponForm = ref(emptyCouponForm())
	const couponSubmitting = ref(false)

	const couponTypeLabels = COUPON_TYPE_PICKER_LABELS

	async function loadCoupons() {
		if (listLoading.value) return
		listLoading.value = true
		const res = await fetchManageCoupons(Number(shopId.value), 1, 50)
		listLoading.value = false
		if (!res.ok) {
			uni.showToast({ title: res.msg || '加载失败', icon: 'none' })
			return
		}
		coupons.value = res.rows
	}

	function onCouponTypeChange(e) {
		couponTypeIndex.value = Number(e.detail.value)
		couponForm.value.type = couponTypeIndex.value === 0 ? 1 : 2
	}

	function promptAddCoupon() {
		editingCouponId.value = null
		editingCouponItem.value = null
		couponTypeIndex.value = 0
		couponForm.value = emptyCouponForm()
		couponFormVisible.value = true
	}

	function editCoupon(item) {
		if (!canEditCoupon(item)) return
		editingCouponId.value = item.templateId
		editingCouponItem.value = item
		couponTypeIndex.value = item.type === 2 ? 1 : 0
		couponForm.value = {
			couponName: item.couponName || '',
			type: item.type === 2 ? 2 : 1,
			thresholdAmount: item.thresholdAmount != null ? String(item.thresholdAmount) : '',
			discountValue: item.discountValue != null ? String(item.discountValue) : '',
			validDays: item.validDays != null ? String(item.validDays) : '',
			neverExpire: item.validDays == null || item.validDays === '',
			totalQuantity: item.totalQuantity != null ? String(item.totalQuantity) : '',
			distributionStartDate: item.distributionStartTime ? formatManageCouponDate(item.distributionStartTime) : '',
			distributionEndDate: item.distributionEndTime ? formatManageCouponDate(item.distributionEndTime) : ''
		}
		couponFormVisible.value = true
	}

	function formatCouponRuleFromForm() {
		return formatCouponRule({
			type: couponForm.value.type,
			thresholdAmount: couponForm.value.thresholdAmount,
			discountValue: couponForm.value.discountValue
		})
	}

	function formatValidDaysFromForm() {
		if (couponForm.value.neverExpire) return '领取后永久'
		const days = Number(couponForm.value.validDays)
		if (!days || Number.isNaN(days)) return '领取后永久'
		return `领取后 ${days} 天`
	}

	function onNeverExpireChange(e) {
		couponForm.value.neverExpire = !!e.detail.value
		if (couponForm.value.neverExpire) {
			couponForm.value.validDays = ''
		}
	}

	function closeCouponForm() {
		if (couponSubmitting.value) return
		couponFormVisible.value = false
		editingCouponId.value = null
		editingCouponItem.value = null
	}

	async function doResumeOrStopCoupon(templateId, action, fromForm = false) {
		couponSubmitting.value = true
		const sid = Number(shopId.value)
		const res =
			action === 'resume'
				? await resumeManageCouponDistribution(templateId, sid)
				: await stopManageCouponDistribution(templateId, sid)
		couponSubmitting.value = false
		if (!res.ok) {
			uni.showToast({ title: res.msg || '操作失败', icon: 'none' })
			return
		}
		uni.showToast({ title: res.msg || (action === 'resume' ? '已恢复发放' : '已停止发放'), icon: 'success' })
		if (fromForm) {
			couponFormVisible.value = false
			editingCouponId.value = null
			editingCouponItem.value = null
		}
		loadCoupons()
	}

	function stopCouponDistribution() {
		if (!editingCouponId.value || couponSubmitting.value) return
		uni.showModal({
			title: '停止发放',
			content: '停止后用户将无法继续领取该优惠券，确定停止吗？',
			success: async (r) => {
				if (!r.confirm) return
				await doResumeOrStopCoupon(editingCouponId.value, 'stop', true)
			}
		})
	}

	function resumeCouponDistributionFromForm() {
		if (!editingCouponId.value || couponSubmitting.value) return
		uni.showModal({
			title: '恢复发放',
			content: '恢复后将取消手动停止，按当前发放时间与库存状态继续，确定恢复吗？',
			success: async (r) => {
				if (!r.confirm) return
				await doResumeOrStopCoupon(editingCouponId.value, 'resume', true)
			}
		})
	}

	function resumeCouponDistribution(item) {
		if (!item?.templateId || couponSubmitting.value) return
		uni.showModal({
			title: '恢复发放',
			content: `确定恢复「${item.couponName}」的发放吗？`,
			success: async (r) => {
				if (!r.confirm) return
				await doResumeOrStopCoupon(item.templateId, 'resume')
			}
		})
	}

	async function submitCouponForm() {
		if (editingCouponId.value) {
			await submitCouponDistributionEdit()
			return
		}
		const name = couponForm.value.couponName?.trim()
		if (!name) {
			uni.showToast({ title: '请输入折扣券名称', icon: 'none' })
			return
		}
		const thresholdAmount = parseOptionalAmount(couponForm.value.thresholdAmount)
		if (thresholdAmount == null) {
			uni.showToast({ title: '使用门槛须为非负数', icon: 'none' })
			return
		}
		const discountValue = parsePositiveAmount(couponForm.value.discountValue)
		if (!discountValue) {
			uni.showToast({ title: couponForm.value.type === 1 ? '请输入有效折扣力度' : '请输入有效减免积分', icon: 'none' })
			return
		}
		if (couponForm.value.type === 1 && discountValue > 100) {
			uni.showToast({ title: '折扣力度不能超过 100', icon: 'none' })
			return
		}
		const totalQuantityRaw = couponForm.value.totalQuantity?.trim()
		const totalQuantity = parseOptionalPositiveInt(couponForm.value.totalQuantity)
		if (totalQuantityRaw && totalQuantity == null) {
			uni.showToast({ title: '发放数量须为正整数，或留空表示无限', icon: 'none' })
			return
		}
		if (!couponForm.value.neverExpire) {
			const validDays = parsePositiveInt(couponForm.value.validDays)
			if (!validDays) {
				uni.showToast({ title: '请输入领取后有效天数', icon: 'none' })
				return
			}
			if (validDays > 365) {
				uni.showToast({ title: '有效天数不能超过 365 天', icon: 'none' })
				return
			}
		}
		if (
			couponForm.value.distributionStartDate &&
			couponForm.value.distributionEndDate &&
			couponForm.value.distributionStartDate > couponForm.value.distributionEndDate
		) {
			uni.showToast({ title: '结束发放时间须晚于开始时间', icon: 'none' })
			return
		}
		if (couponSubmitting.value) return
		couponSubmitting.value = true
		const validDays = couponForm.value.neverExpire ? null : parsePositiveInt(couponForm.value.validDays)
		const res = await addManageCoupon({
			shopId: Number(shopId.value),
			couponName: name,
			type: couponForm.value.type,
			thresholdAmount,
			discountValue,
			validDays,
			effectiveTime: null,
			expireTime: null,
			totalQuantity,
			distributionStartTime: dateToDateTime(couponForm.value.distributionStartDate),
			distributionEndTime: dateToDateTimeEnd(couponForm.value.distributionEndDate)
		})
		couponSubmitting.value = false
		if (!res.ok) {
			uni.showToast({ title: res.msg || '添加失败', icon: 'none' })
			return
		}
		uni.showToast({ title: '添加成功', icon: 'success' })
		couponFormVisible.value = false
		editingCouponId.value = null
		editingCouponItem.value = null
		loadCoupons()
	}

	async function submitCouponDistributionEdit() {
		if (
			couponForm.value.distributionStartDate &&
			couponForm.value.distributionEndDate &&
			couponForm.value.distributionStartDate > couponForm.value.distributionEndDate
		) {
			uni.showToast({ title: '结束发放时间须晚于开始时间', icon: 'none' })
			return
		}
		if (couponSubmitting.value) return
		couponSubmitting.value = true
		const res = await adjustManageCouponDistribution(editingCouponId.value, {
			shopId: Number(shopId.value),
			distributionStartTime: dateToDateTime(couponForm.value.distributionStartDate),
			distributionEndTime: dateToDateTimeEnd(couponForm.value.distributionEndDate)
		})
		couponSubmitting.value = false
		if (!res.ok) {
			uni.showToast({ title: res.msg || '保存失败', icon: 'none' })
			return
		}
		uni.showToast({ title: '保存成功', icon: 'success' })
		couponFormVisible.value = false
		editingCouponId.value = null
		editingCouponItem.value = null
		loadCoupons()
	}

	function deleteCoupon(item) {
		if (!isManager.value) return
		uni.showModal({
			title: '删除折扣券',
			content: `确定删除「${item.couponName}」吗？`,
			success: async (r) => {
				if (!r.confirm) return
				const res = await deleteManageCoupon(item.templateId)
				if (!res.ok) {
					uni.showToast({ title: res.msg || '删除失败', icon: 'none' })
					return
				}
				uni.showToast({ title: '已删除', icon: 'success' })
				loadCoupons()
			}
		})
	}

	function promptAddCouponStock(item) {
		couponStockTarget.value = item
		couponStockForm.value = { quantity: '' }
		couponStockVisible.value = true
	}

	function closeCouponStockForm() {
		if (couponStockSubmitting.value) return
		couponStockVisible.value = false
		couponStockTarget.value = null
	}

	async function submitCouponStockForm() {
		const addQuantity = parsePositiveInt(couponStockForm.value.quantity)
		if (!addQuantity) {
			uni.showToast({ title: '请输入有效数量', icon: 'none' })
			return
		}
		if (couponStockSubmitting.value || !couponStockTarget.value) return
		couponStockSubmitting.value = true
		const res = await addManageCouponStock(couponStockTarget.value.templateId, {
			shopId: Number(shopId.value),
			addQuantity
		})
		couponStockSubmitting.value = false
		if (!res.ok) {
			uni.showToast({ title: res.msg || '操作失败', icon: 'none' })
			return
		}
		uni.showToast({ title: '库存已追加', icon: 'success' })
		couponStockVisible.value = false
		couponStockTarget.value = null
		loadCoupons()
	}

	return {
		coupons,
		couponStockVisible,
		couponStockTarget,
		couponStockForm,
		couponStockSubmitting,
		couponFormVisible,
		editingCouponId,
		editingCouponItem,
		couponTypeIndex,
		couponTypeLabels,
		couponForm,
		couponSubmitting,
		loadCoupons,
		onCouponTypeChange,
		promptAddCoupon,
		editCoupon,
		formatCouponRuleFromForm,
		formatValidDaysFromForm,
		onNeverExpireChange,
		closeCouponForm,
		stopCouponDistribution,
		resumeCouponDistributionFromForm,
		resumeCouponDistribution,
		submitCouponForm,
		deleteCoupon,
		promptAddCouponStock,
		closeCouponStockForm,
		submitCouponStockForm,
		canEditCoupon,
		canStopCouponDistribution,
		canResumeCouponDistribution,
		formatValidDays
	}
}
