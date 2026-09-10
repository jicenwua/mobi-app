import { ref, computed, watch, toRef } from 'vue'
import {
	normalizeShopDetail,
	isUsableShopDetail,
	fetchShopFromUserList,
	fetchShopProductCatalog,
	fetchShopMemberExtras,
	formatShopAddress,
	getShopCarouselImages
} from '@/api/modules/shop.js'
import { formatPointsAmount } from '@/utils/points-format.js'
import { formatActivityTimeRange, formatActivityRule } from '@/utils/activity-display.js'
import { canShowMemberTab, canGeneratePayQrcode, canManageShop, canStaffVerifyAtShop } from '@/utils/wx-perm.js'
import { scanStaffVerifyCode } from '@/utils/staff-scan.js'
import { isShopClerkCapable } from '@/utils/shop-role.js'
import { useMemberShopCart } from '@/composables/use-member-shop-cart.js'
import { useMemberShopPromo } from '@/composables/use-member-shop-promo.js'
import { rememberShopDetail, navigateWithShop, peekShopDetail } from '@/utils/shop-page-context.js'

/**
 * 会员店铺详情页 — 数据加载与各子模块编排
 */
export function useMemberShopDetail(props) {
	const shopId = toRef(props, 'shopId')
	const detail = ref(null)
	const loading = ref(true)
	const errorMsg = ref('')

	const products = ref([])
	const productCategories = ref([])
	const productsLoaded = ref(false)
	const productsLoading = ref(false)

	let detailLoadToken = 0

	const promo = useMemberShopPromo(shopId, detail)
	const cart = useMemberShopCart(shopId, { productCategories, detail })

	const carouselImages = computed(() => getShopCarouselImages(detail.value))
	const shopAddress = computed(() => {
		const formatted = formatShopAddress(detail.value)
		if (formatted) return formatted
		return (detail.value?.address || '').trim()
	})
	const shopPhone = computed(() => (detail.value?.phone || '').trim())

	const remainingPoints = computed(() => {
		const n = detail.value?.remainingPoints
		if (n == null || n === '') return '0.00'
		return formatPointsAmount(n)
	})

	const canPayQrcode = computed(() => canShowMemberTab())
	const showManageLink = computed(() => isShopClerkCapable(detail.value) && canManageShop())
	const showScanVerifyBtn = computed(() => canStaffVerifyAtShop(detail.value))

	function callShopPhone() {
		const phone = shopPhone.value
		if (!phone) return
		uni.makePhoneCall({ phoneNumber: phone })
	}

	function goManage() {
		const id = shopId.value
		if (!id || !showManageLink.value) {
			uni.showToast({ title: '无店铺管理权限', icon: 'none' })
			return
		}
		const shop = detail.value || { id, shopId: id }
		navigateWithShop({
			url: `/pages/shop/manage?id=${id}`,
			shop
		})
	}

	function goPayQrcode() {
		if (!canGeneratePayQrcode()) {
			uni.showToast({ title: '无付款码权限', icon: 'none' })
			return
		}
		uni.navigateTo({
			url: '/pages/member/pay-qrcode',
			animationType: 'slide-in-right',
			animationDuration: 200
		})
	}

	function scanForVerify() {
		const id = shopId.value
		if (!id || !showScanVerifyBtn.value) return
		scanStaffVerifyCode({ shopId: id })
	}

	function resetPageState() {
		products.value = []
		productCategories.value = []
		productsLoaded.value = false
		productsLoading.value = false
		promo.resetPromo()
		cart.restoreCartFromCache()
	}

	function finishDetailLoad(data) {
		const normalized = rememberShopDetail(data) || normalizeShopDetail(data)
		detail.value = normalized || data
		loading.value = false
		errorMsg.value = ''
	}

	async function loadMemberDetailExtras(token) {
		const id = shopId.value
		if (!id || token !== detailLoadToken || !detail.value) return
		if (productsLoaded.value) {
			productsLoading.value = false
			return
		}

		productsLoading.value = true
		try {
			const productRes = await fetchShopProductCatalog(Number(id))
			if (token !== detailLoadToken) return
			productCategories.value = productRes.ok && Array.isArray(productRes.data) ? productRes.data : []
			products.value = productRes.ok ? productRes.rows : []
			productsLoaded.value = true
			if (!productRes.ok) {
				uni.showToast({ title: productRes.msg || '商品加载失败', icon: 'none' })
			}
			void loadAsyncMemberExtras(Number(id), token)
		} finally {
			productsLoading.value = false
		}
	}

	async function loadAsyncMemberExtras(id, token) {
		const includeCoupons = promo.shouldIncludeCouponsInExtras()
		const extrasRes = await fetchShopMemberExtras(id, { includeCoupons })
		if (token !== detailLoadToken || !detail.value) return

		detail.value = {
			...detail.value,
			activities: extrasRes.activities,
			announcement: extrasRes.announcement,
			coupons: includeCoupons ? extrasRes.coupons : []
		}
		promo.showActivityModalIfNeeded()
		if (includeCoupons && !promo.activityModalVisible.value) {
			promo.showCouponModalIfNeeded()
		}
	}

	async function loadDetailFallback(token) {
		if (token !== detailLoadToken) return
		if (detail.value && isUsableShopDetail(detail.value)) return
		if (!canShowMemberTab()) {
			errorMsg.value = '无会员访问权限'
			loading.value = false
			detail.value = null
			return
		}
		const id = shopId.value
		if (!id) {
			errorMsg.value = '店铺信息无效'
			loading.value = false
			detail.value = null
			return
		}
		loading.value = true
		errorMsg.value = ''
		const res = await fetchShopFromUserList(id)
		if (token !== detailLoadToken) return
		if (!res.ok || !res.data) {
			loading.value = false
			errorMsg.value = res.msg || '加载失败'
			detail.value = null
			return
		}
		if (token !== detailLoadToken) return
		finishDetailLoad(res.data)
		if (token !== detailLoadToken) return
		await loadMemberDetailExtras(token)
	}

	async function applyShop(data) {
		if (!canShowMemberTab()) {
			errorMsg.value = '无会员访问权限'
			loading.value = false
			detail.value = null
			return
		}
		const id = shopId.value
		if (!id) {
			errorMsg.value = '店铺信息无效'
			loading.value = false
			detail.value = null
			return
		}
		if (detail.value && isUsableShopDetail(detail.value)) {
			const currentId = normalizeShopDetail(detail.value)?.id
			if (String(currentId) === String(id)) {
				if (!productsLoaded.value) {
					await loadMemberDetailExtras(detailLoadToken)
				}
				return
			}
		}
		detailLoadToken += 1
		const token = detailLoadToken
		if (!isUsableShopDetail(data)) {
			await loadDetailFallback(token)
			return
		}
		const normalized = normalizeShopDetail(data)
		if (String(normalized.id) !== String(id)) {
			await loadDetailFallback(token)
			return
		}
		loading.value = true
		errorMsg.value = ''
		finishDetailLoad(normalized)
		await loadMemberDetailExtras(token)
	}

	function formatProductPoints(product) {
		const price = product?.price
		if (price == null) return '—'
		const n = Number(price)
		if (Number.isNaN(n)) return '—'
		if (Number.isInteger(n)) return String(n)
		return String(Math.round(n * 100) / 100)
	}

	function openProductDetail(product) {
		const id = shopId.value
		if (!product?.productId || !id) return
		navigateWithShop({
			url: `/pages/shop/product-detail?shopId=${id}&member=1`,
			shop: detail.value,
			success(res) {
				res.eventChannel?.emit('product', product)
			}
		})
	}

	watch(
		shopId,
		(id) => {
			detailLoadToken += 1
			const token = detailLoadToken
			resetPageState()
			detail.value = null
			errorMsg.value = ''
			if (!id) {
				loading.value = false
				errorMsg.value = '店铺信息无效'
				return
			}
			const cached = peekShopDetail(id)
			if (cached && isUsableShopDetail(cached)) {
				loading.value = false
				errorMsg.value = ''
				finishDetailLoad(cached)
				void loadMemberDetailExtras(token)
				return
			}
			loading.value = true
			void loadDetailFallback(token)
		},
		{ immediate: true }
	)

	watch(promo.availableCoupons, (list) => {
		if (!list.length) {
			promo.couponModalVisible.value = false
		}
	})

	return {
		detail,
		loading,
		errorMsg,
		products,
		productCategories,
		productsLoaded,
		productsLoading,
		carouselImages,
		shopAddress,
		shopPhone,
		remainingPoints,
		canPayQrcode,
		showManageLink,
		showScanVerifyBtn,
		callShopPhone,
		goManage,
		goPayQrcode,
		scanForVerify,
		applyShop,
		reload: loadDetailFallback,
		syncCartFromCache: cart.syncCartFromCache,
		formatProductPoints,
		openProductDetail,
		formatActivityTimeRange,
		formatActivityRule,
		...promo,
		...cart
	}
}
