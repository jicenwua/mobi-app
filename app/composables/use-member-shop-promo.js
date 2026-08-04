import { ref, computed } from 'vue'
import { claimShopCoupon } from '@/api/modules/shop.js'
import { isShopManager } from '@/utils/shop-role.js'
import {
	hasShownActivityPopup,
	markActivityPopupShown
} from '@/utils/activity-popup-session.js'
import {
	hasDismissedCouponBanner,
	markCouponBannerDismissed
} from '@/utils/coupon-banner-session.js'

/**
 * 会员店铺详情 — 活动公告与优惠券弹层
 */
export function useMemberShopPromo(shopId, detail) {
	const activityModalVisible = ref(false)
	const activityIndex = ref(0)
	const couponModalVisible = ref(false)
	const couponClaiming = ref(false)
	const claimedCouponIds = ref([])

	const activeActivities = computed(() => {
		const list = detail.value?.activities
		return Array.isArray(list) ? list.filter((item) => item && item.kind !== 2) : []
	})

	const announcement = computed(() => {
		const ann = detail.value?.announcement
		return ann && typeof ann === 'object' ? ann : null
	})

	const promoItems = computed(() => {
		const items = activeActivities.value.map((data) => ({ type: 'activity', data }))
		if (announcement.value) {
			items.push({ type: 'announcement', data: announcement.value })
		}
		return items
	})

	const currentPromoItem = computed(() => promoItems.value[activityIndex.value] || null)

	const currentPromoTitle = computed(() => {
		const item = currentPromoItem.value
		if (!item) return ''
		if (item.type === 'announcement') {
			return item.data.activityName || item.data.description?.slice(0, 20) || '店铺公告'
		}
		return item.data.activityName || '未命名活动'
	})

	const currentPromoDescription = computed(() => {
		const desc = currentPromoItem.value?.data?.description
		return desc ? String(desc).trim() : ''
	})

	const currentPromoRules = computed(() => {
		const rules = currentPromoItem.value?.type === 'activity' ? currentPromoItem.value.data?.rules : []
		return Array.isArray(rules) ? rules : []
	})

	const availableCoupons = computed(() => {
		const list = detail.value?.coupons
		if (!Array.isArray(list)) return []
		const claimed = new Set(claimedCouponIds.value)
		return list.filter((item) => {
			if (!item?.templateId || claimed.has(item.templateId)) return false
			if (item.remainingQuantity != null && item.remainingQuantity !== '') {
				return Number(item.remainingQuantity) > 0
			}
			const total = item.totalQuantity
			if (total == null || total === '') return true
			const issued = item.issuedQuantity ?? 0
			return total > 0 && issued < total
		})
	})

	function resetPromo() {
		activityModalVisible.value = false
		activityIndex.value = 0
		couponModalVisible.value = false
		couponClaiming.value = false
		claimedCouponIds.value = []
	}

	function showActivityModalIfNeeded() {
		const list = promoItems.value
		if (!list.length) {
			activityModalVisible.value = false
			return
		}
		const shopKey = shopId.value
		if (hasShownActivityPopup(shopKey)) {
			activityModalVisible.value = false
			return
		}
		activityIndex.value = 0
		activityModalVisible.value = true
	}

	function showCouponModalIfNeeded() {
		if (activityModalVisible.value) return
		const list = availableCoupons.value
		if (!list.length) {
			couponModalVisible.value = false
			return
		}
		if (hasDismissedCouponBanner(shopId.value)) {
			couponModalVisible.value = false
			return
		}
		couponModalVisible.value = true
	}

	function closeActivityModal() {
		activityModalVisible.value = false
		markActivityPopupShown(shopId.value)
		showCouponModalIfNeeded()
	}

	function closeCouponModal() {
		couponModalVisible.value = false
		markCouponBannerDismissed(shopId.value)
	}

	function prevPromoItem() {
		const len = promoItems.value.length
		if (len <= 1) return
		activityIndex.value = (activityIndex.value - 1 + len) % len
	}

	function nextPromoItem() {
		const len = promoItems.value.length
		if (len <= 1) return
		activityIndex.value = (activityIndex.value + 1) % len
	}

	function isCouponAlreadyClaimedMsg(msg) {
		const text = String(msg || '')
		return text.includes('已领取') || text.includes('您已领取')
	}

	function handleCouponClaimFailure(coupon, msg) {
		if (coupon?.templateId && isCouponAlreadyClaimedMsg(msg)) {
			claimedCouponIds.value = claimedCouponIds.value.concat(coupon.templateId)
		}
		if (!availableCoupons.value.length) {
			markCouponBannerDismissed(shopId.value)
			couponModalVisible.value = false
		}
		uni.showToast({ title: msg || '领取失败', icon: 'none' })
	}

	async function claimCoupon(coupon) {
		if (!coupon?.templateId || couponClaiming.value) return
		couponClaiming.value = true
		const res = await claimShopCoupon(coupon.templateId, shopId.value)
		couponClaiming.value = false
		if (!res.ok) {
			handleCouponClaimFailure(coupon, res.msg)
			return
		}
		claimedCouponIds.value = claimedCouponIds.value.concat(coupon.templateId)
		uni.showToast({ title: res.msg || '领取成功', icon: 'success' })
		if (!availableCoupons.value.length) {
			closeCouponModal()
		}
	}

	async function claimAllCoupons() {
		const list = availableCoupons.value
		if (!list.length || couponClaiming.value) return
		couponClaiming.value = true
		let successCount = 0
		let lastMsg = ''
		for (const coupon of list) {
			const res = await claimShopCoupon(coupon.templateId, shopId.value)
			if (res.ok) {
				successCount += 1
				claimedCouponIds.value = claimedCouponIds.value.concat(coupon.templateId)
				lastMsg = res.msg || '领取成功'
			} else {
				lastMsg = res.msg || '领取失败'
				if (isCouponAlreadyClaimedMsg(lastMsg)) {
					claimedCouponIds.value = claimedCouponIds.value.concat(coupon.templateId)
				}
			}
		}
		couponClaiming.value = false
		if (successCount > 0) {
			uni.showToast({
				title: successCount === list.length ? lastMsg || '领取成功' : `已领取 ${successCount} 张`,
				icon: 'success'
			})
		} else {
			uni.showToast({ title: lastMsg || '领取失败', icon: 'none' })
		}
		if (!availableCoupons.value.length) {
			if (successCount === 0 && isCouponAlreadyClaimedMsg(lastMsg)) {
				markCouponBannerDismissed(shopId.value)
				couponModalVisible.value = false
			} else {
				closeCouponModal()
			}
		}
	}

	function shouldIncludeCouponsInExtras() {
		return !isShopManager(detail.value)
	}

	return {
		activityModalVisible,
		activityIndex,
		couponModalVisible,
		couponClaiming,
		promoItems,
		currentPromoItem,
		currentPromoTitle,
		currentPromoDescription,
		currentPromoRules,
		availableCoupons,
		resetPromo,
		showActivityModalIfNeeded,
		showCouponModalIfNeeded,
		closeActivityModal,
		closeCouponModal,
		prevPromoItem,
		nextPromoItem,
		claimCoupon,
		claimAllCoupons,
		shouldIncludeCouponsInExtras
	}
}
