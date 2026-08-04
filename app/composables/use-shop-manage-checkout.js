import { ref, reactive, computed } from 'vue'
import { scanStaffPayCode, scanStaffVerifyCode } from '@/utils/staff-scan.js'
import { verifyPayQrcode } from '@/api/modules/qrcode.js'
import { consumePoints } from '@/api/modules/points.js'
import { createRequestId } from '@/utils/request-id.js'

/**
 * 店铺管理页 — 收银台：选购、扫码扣款
 */
export function useShopManageCheckout({ shopId, canScanVerify, productCategories, loadProducts, setActiveTab }) {
	const checkoutPayToken = ref('')
	const checkoutMember = ref(null)
	const checkoutCart = reactive({})
	const checkoutSubmitting = ref(false)
	let checkoutRequestId = ''

	const checkoutCartCount = computed(() =>
		Object.values(checkoutCart).reduce((sum, qty) => sum + (Number(qty) || 0), 0)
	)

	const checkoutTotalPoints = computed(() => {
		let sum = 0
		for (const cat of productCategories.value) {
			for (const product of cat.products || []) {
				const qty = Number(checkoutCart[product.productId]) || 0
				if (qty <= 0) continue
				const price = Number(product.price)
				if (Number.isNaN(price)) continue
				sum += Math.round(price) * qty
			}
		}
		return sum
	})

	function applyMemberForCheckout(token, memberInfo) {
		checkoutPayToken.value = token
		checkoutMember.value = memberInfo
		setActiveTab('product')
		uni.showToast({ title: '已识别会员，请确认扣款', icon: 'none' })
	}

	async function applyCheckoutToken(token) {
		const verify = await verifyPayQrcode({ shopId: shopId.value, token })
		if (!verify.ok || !verify.data) {
			uni.showToast({ title: verify.msg || '付款码无效', icon: 'none' })
			return
		}
		applyMemberForCheckout(token, verify.data)
	}

	function exitCheckout() {
		checkoutPayToken.value = ''
		checkoutMember.value = null
		checkoutRequestId = ''
		Object.keys(checkoutCart).forEach((key) => {
			delete checkoutCart[key]
		})
	}

	function onCheckoutQtyChange({ product, delta }) {
		const id = product?.productId
		if (id == null) return
		const cur = Number(checkoutCart[id]) || 0
		const next = cur + delta
		if (next <= 0) {
			delete checkoutCart[id]
		} else {
			checkoutCart[id] = next
		}
	}

	function buildCheckoutItems() {
		return Object.entries(checkoutCart)
			.filter(([, count]) => Number(count) > 0)
			.map(([productId, count]) => ({ productId: Number(productId), count: Number(count) }))
	}

	async function doCheckoutConsume(token) {
		const items = buildCheckoutItems()
		if (!items.length) {
			uni.showToast({ title: '请选择商品', icon: 'none' })
			return
		}
		checkoutSubmitting.value = true
		if (!checkoutRequestId) {
			checkoutRequestId = createRequestId()
		}
		const res = await consumePoints({
			shopId: Number(shopId.value),
			token,
			items,
			requestId: checkoutRequestId
		})
		checkoutSubmitting.value = false
		if (!res.ok) {
			uni.showToast({ title: res.msg || '扣款失败', icon: 'none' })
			return
		}
		checkoutRequestId = ''
		uni.showToast({
			title: `扣款成功，-${res.data ?? checkoutTotalPoints.value} 积分`,
			icon: 'success'
		})
		exitCheckout()
		void loadProducts()
	}

	async function submitCheckoutConsume() {
		if (checkoutSubmitting.value || checkoutTotalPoints.value <= 0) return
		if (!checkoutPayToken.value) {
			scanStaffPayCode({
				shopId: shopId.value,
				onPayVerified: async (token, memberInfo) => {
					applyMemberForCheckout(token, memberInfo)
					await doCheckoutConsume(token)
				}
			})
			return
		}
		await doCheckoutConsume(checkoutPayToken.value)
	}

	function scanForVerify() {
		if (!shopId.value || !canScanVerify.value) return
		scanStaffVerifyCode({
			shopId: shopId.value,
			onPayVerified: (token, memberInfo) => {
				if (checkoutCartCount.value <= 0) {
					uni.showToast({ title: '请先在下方选购商品', icon: 'none' })
					return
				}
				applyMemberForCheckout(token, memberInfo)
			}
		})
	}

	function shouldBlockProductClick() {
		return canScanVerify.value && (checkoutCartCount.value > 0 || checkoutMember.value)
	}

	return {
		checkoutPayToken,
		checkoutMember,
		checkoutCart,
		checkoutSubmitting,
		checkoutCartCount,
		checkoutTotalPoints,
		applyMemberForCheckout,
		applyCheckoutToken,
		exitCheckout,
		onCheckoutQtyChange,
		submitCheckoutConsume,
		scanForVerify,
		shouldBlockProductClick
	}
}
