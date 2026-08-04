<template>
	<view class="segmented-tabs">
		<view
			v-for="tab in tabs"
			:key="tab.key"
			class="segmented-tabs__item"
			:class="{ 'segmented-tabs__item--active': modelValue === tab.key }"
			@click="$emit('update:modelValue', tab.key)"
		>
			<text class="segmented-tabs__text">{{ tab.label }}</text>
		</view>
		<view class="segmented-tabs__slider" :style="sliderStyle" />
	</view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
	tabs: { type: Array, default: () => [] },
	modelValue: { type: String, default: '' }
})

defineEmits(['update:modelValue'])

const sliderStyle = computed(() => {
	const idx = Math.max(
		0,
		props.tabs.findIndex((t) => t.key === props.modelValue)
	)
	const count = props.tabs.length || 1
	const width = 100 / count
	return {
		width: `calc(${width}% - 3px)`,
		transform: `translateX(${idx * 100}%)`
	}
})
</script>

<style scoped>
.segmented-tabs {
	display: flex;
	position: relative;
	width: 100%;
	box-sizing: border-box;
	background: #e8e8e8;
	border-radius: 8px;
	padding: 3px;
}

.segmented-tabs__item {
	flex: 1;
	min-width: 0;
	z-index: 1;
	padding: 8px 0;
	text-align: center;
}

.segmented-tabs__text {
	font-size: 14px;
	color: #666;
	white-space: nowrap;
}

.segmented-tabs__item--active .segmented-tabs__text {
	color: #fff;
	font-weight: 600;
}

.segmented-tabs__slider {
	position: absolute;
	top: 3px;
	left: 3px;
	height: calc(100% - 6px);
	background: #007aff;
	border-radius: 6px;
	transition: transform 0.25s ease;
}
</style>
