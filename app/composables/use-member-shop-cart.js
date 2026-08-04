import { reactive, computed, ref } from 'vue'
import { getShopCart, setShopCart } from '@/utils/shop-cart-cache.js'
import { setShopPurchaseContext } from '@/utils/shop-purchase-context.js'
import { formatPointsAmount } from '@/utils/points-format.js'

/**
 * 会员店铺详情 — 购物车与结算
 */
export function useMemberShopCart(shopId, { productCategories, detail }) {
	const cart = reactive({})
	const checkingOut = ref(false)

	const cartItemCount = computed(() =>
		Object.values(cart).reduce((sum, qty) => sum + (Number(qty) || 0), 0)
	)

	const cartTotalPoints = computed(() => {
		let sum = 0
		for (const cat of productCategories.value) {
			const list = Array.isArray(cat?.products) ? cat.products : []
			for (const product of list) {
				const qty = cart[product.productId]
				if (!qty) continue
				const price = Number(product.price)
				if (Number.isNaN(price)) continue
				sum += Math.round(price) * qty
			}
		}
		return sum
	})

	const cartTotalPointsText = computed(() => formatPointsAmount(cartTotalPoints.value))

	const cartLines = computed(() => {
		const lines = []
		for (const cat of productCategories.value) {
			const list = Array.isArray(cat?.products) ? cat.products : []
			for (const product of list) {
				const count = Number(cart[product.productId]) || 0
				if (count <= 0) continue
				const price = Number(product.price)
				const unitPoints = Number.isNaN(price) ? 0 : Math.round(price)
				lines.push({
					productId: product.productId,
					productName: product.productName || '商品',
					count,
					unitPoints,
					linePoints: unitPoints * count,
					product
				})
			}
		}
		return lines
	})

	function restoreCartFromCache() {
		Object.keys(cart).forEach((key) => {
			delete cart[key]
		})
		const saved = getShopCart(shopId.value)
		Object.assign(cart, saved)
	}

	function persistCart() {
		setShopCart(shopId.value, cart)
	}

	function syncCartFromCache() {
		restoreCartFromCache()
	}

	function onCartQtyChange({ product, delta }) {
		const id = product?.productId
		if (id == null) return
		const cur = Number(cart[id]) || 0
		const next = cur + delta
		if (next <= 0) {
			delete cart[id]
		} else {
			cart[id] = next
		}
		persistCart()
	}

	function onCartQtyUpdate({ productId, delta }) {
		const line = cartLines.value.find((item) => item.productId === productId)
		if (!line?.product) return
		onCartQtyChange({ product: line.product, delta })
	}

	function goPurchaseConfirm() {
		if (cartItemCount.value <= 0) {
			uni.showToast({ title: '请选择商品', icon: 'none' })
			return
		}
		const id = shopId.value
		if (!id) return
		setShopPurchaseContext({
			shopId: id,
			shopName: detail.value?.shopName || '',
			cartLines: cartLines.value,
			totalPoints: cartTotalPoints.value
		})
		uni.navigateTo({
			url: `/pages/member/purchase-confirm?shopId=${id}`,
			animationType: 'slide-in-right',
			animationDuration: 200
		})
	}

	async function submitPurchase() {
		void goPurchaseConfirm()
	}

	return {
		cart,
		checkingOut,
		cartItemCount,
		cartTotalPoints,
		cartTotalPointsText,
		cartLines,
		restoreCartFromCache,
		syncCartFromCache,
		onCartQtyChange,
		onCartQtyUpdate,
		submitPurchase
	}
}
