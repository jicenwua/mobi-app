<template>
	<view
		class="page-loading"
		:class="{
			'page-loading--inline': inline && !overlay,
			'page-loading--overlay': overlay,
			'page-loading--compact': compact
		}"
		:style="rootStyle"
	>
		<view class="page-loading__spinner" :style="spinnerStyle" />
		<text v-if="text" class="page-loading__text">{{ text }}</text>
	</view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
	text: { type: String, default: '加载中…' },
	/** 列表/区块内占位 */
	inline: { type: Boolean, default: true },
	/** 全屏半透明遮罩 */
	overlay: { type: Boolean, default: false },
	/** 底部「加载更多」等紧凑场景 */
	compact: { type: Boolean, default: false },
	size: { type: Number, default: 32 },
	color: { type: String, default: '#007aff' },
	overlayBg: { type: String, default: '' }
})

const rootStyle = computed(() => {
	if (!props.overlay || !props.overlayBg) return {}
	return { backgroundColor: props.overlayBg }
})

const spinnerStyle = computed(() => ({
	width: `${props.size}px`,
	height: `${props.size}px`,
	borderColor: `${props.color}26`,
	borderTopColor: props.color
}))
</script>

<style scoped>
.page-loading {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 12px;
	box-sizing: border-box;
}

.page-loading--inline {
	padding: 48px 16px;
}

.page-loading--compact {
	padding: 16px 0 8px;
	gap: 8px;
}

.page-loading--overlay {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	z-index: 999;
	background-color: rgba(255, 255, 255, 0.72);
	padding: 0;
}

.page-loading__spinner {
	border-width: 3px;
	border-style: solid;
	border-radius: 50%;
	animation: page-loading-spin 0.75s linear infinite;
	flex-shrink: 0;
}

.page-loading--compact .page-loading__spinner {
	border-width: 2px;
}

.page-loading__text {
	font-size: 14px;
	color: #888888;
	line-height: 1.4;
}

.page-loading--compact .page-loading__text {
	font-size: 12px;
}

@keyframes page-loading-spin {
	to {
		transform: rotate(360deg);
	}
}
</style>
