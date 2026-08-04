<template>
	<canvas
		:canvas-id="canvasId"
		:id="canvasId"
		class="qrcode-canvas"
		:style="{ width: size + 'px', height: size + 'px' }"
	/>
</template>

<script setup>
import { watch, getCurrentInstance, nextTick } from 'vue'
import UQRCode from '@uqrcode/js'

const props = defineProps({
	text: { type: String, default: '' },
	size: { type: Number, default: 200 },
	canvasId: { type: String, default: 'qrcode-canvas' }
})

const instance = getCurrentInstance()

async function draw() {
	const content = (props.text || '').trim()
	if (!content) return
	await nextTick()
	const qr = new UQRCode()
	qr.data = content
	qr.size = props.size
	qr.margin = 8
	qr.make()
	const ctx = uni.createCanvasContext(props.canvasId, instance?.proxy)
	qr.canvasContext = ctx
	qr.drawCanvas()
}

watch(
	() => [props.text, props.size],
	() => {
		draw()
	},
	{ immediate: true }
)
</script>

<style scoped>
.qrcode-canvas {
	display: block;
}
</style>
