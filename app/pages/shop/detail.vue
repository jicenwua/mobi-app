<template>
	<ShopDetailContent ref="detailRef" :shop-id="shopId" />
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ShopDetailContent from '@/components/shop/shop-detail-content.vue'
import { bindOpenerShop, peekShopDetail } from '@/utils/shop-page-context.js'

const shopId = ref('')
const detailRef = ref(null)

function applyShopToDetail(data) {
	nextTick(() => {
		detailRef.value?.applyShop(data)
	})
}

onLoad((options) => {
	shopId.value = options?.id ? String(options.id) : ''
	if (!shopId.value) {
		applyShopToDetail(null)
		return
	}
	const cached = peekShopDetail(shopId.value)
	if (cached) {
		applyShopToDetail(cached)
	}
	bindOpenerShop((data) => {
		applyShopToDetail(data)
	})
})
</script>
