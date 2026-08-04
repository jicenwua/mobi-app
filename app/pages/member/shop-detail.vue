<template>
	<view />
</template>

<script setup>
import { onLoad } from '@dcloudio/uni-app'

/** 兼容旧链接，统一回到主屏内嵌店铺详情 */
onLoad((options) => {
	const id = options?.id ? String(options.id) : ''
	if (!id) {
		uni.redirectTo({ url: '/pages/main/main' })
		return
	}
	uni.redirectTo({ url: `/pages/main/main?shopId=${id}` })
})
</script>
