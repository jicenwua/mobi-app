<template>
	<view
		class="shop-image-swiper"
		:class="[
			isDark ? 'shop-image-swiper--dark' : 'shop-image-swiper--light',
			compact ? 'shop-image-swiper--compact' : ''
		]"
		:style="{ height }"
	>
		<swiper
			v-if="images.length"
			class="shop-image-swiper__inner"
			:current="current"
			:indicator-dots="images.length > 1"
			indicator-color="rgba(255,255,255,0.45)"
			indicator-active-color="#ffffff"
			:circular="images.length > 1"
			:autoplay="shouldAutoplay"
			:interval="interval"
			:duration="duration"
			@change="onSwiperChange"
		>
			<swiper-item v-for="(src, idx) in images" :key="idx">
				<image class="shop-image-swiper__img" :src="src" mode="aspectFill" lazy-load />
			</swiper-item>
		</swiper>
		<view v-else class="shop-image-swiper__empty">
			<text class="shop-image-swiper__placeholder">暂无图片</text>
		</view>

		<template v-if="images.length > 1 && !compact">
			<view class="shop-image-swiper__nav shop-image-swiper__nav--prev" hover-class="tap-hover-nav" :hover-stay-time="70" @click.stop="goPrev">
				<text class="shop-image-swiper__nav-icon">‹</text>
			</view>
			<view class="shop-image-swiper__nav shop-image-swiper__nav--next" hover-class="tap-hover-nav" :hover-stay-time="70" @click.stop="goNext">
				<text class="shop-image-swiper__nav-icon">›</text>
			</view>
		</template>
	</view>
</template>

<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
	images: { type: Array, default: () => [] },
	height: { type: String, default: '180px' },
	isDark: { type: Boolean, default: false },
	/** 卡片内紧凑展示：隐藏左右切换按钮 */
	compact: { type: Boolean, default: false },
	/** 多图时是否自动轮播 */
	autoplay: { type: Boolean, default: true },
	/** 自动轮播间隔（毫秒） */
	interval: { type: Number, default: 3500 },
	/** 切换动画时长（毫秒） */
	duration: { type: Number, default: 400 }
})

const shouldAutoplay = computed(() => props.autoplay && props.images.length > 1)

const current = ref(0)

watch(
	() => props.images?.length ?? 0,
	() => {
		current.value = 0
	}
)

function onSwiperChange(e) {
	current.value = e.detail?.current ?? 0
}

function goPrev() {
	const n = props.images.length
	if (n <= 1) return
	current.value = current.value <= 0 ? n - 1 : current.value - 1
}

function goNext() {
	const n = props.images.length
	if (n <= 1) return
	current.value = current.value >= n - 1 ? 0 : current.value + 1
}
</script>

<style scoped>
.shop-image-swiper {
	position: relative;
	width: 100%;
	height: 100%;
	overflow: hidden;
}

.shop-image-swiper--compact .shop-image-swiper__placeholder {
	font-size: 11px;
}

.shop-image-swiper__inner,
.shop-image-swiper__img {
	width: 100%;
	height: 100%;
}

.shop-image-swiper__empty {
	width: 100%;
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
}

.shop-image-swiper--light .shop-image-swiper__empty {
	background: linear-gradient(160deg, #dbeafe 0%, #eff6ff 40%, #f0f4f8 100%);
}

.shop-image-swiper--dark .shop-image-swiper__empty {
	background: linear-gradient(160deg, #1e293b 0%, #0f172a 60%, #0f0f14 100%);
}

.shop-image-swiper__placeholder {
	font-size: 13px;
	color: #5c6b7a;
}

.shop-image-swiper__nav {
	position: absolute;
	top: 50%;
	transform: translateY(-50%);
	z-index: 2;
	width: 34px;
	height: 34px;
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
	background: rgba(255, 255, 255, 0.22);
	backdrop-filter: blur(8px);
	border: 1px solid rgba(255, 255, 255, 0.3);
}

.shop-image-swiper__nav--prev {
	left: 12px;
}

.shop-image-swiper__nav--next {
	right: 12px;
}

.shop-image-swiper__nav-icon {
	font-size: 22px;
	line-height: 1;
	color: #fff;
	font-weight: 600;
	margin-top: -2px;
}
</style>
