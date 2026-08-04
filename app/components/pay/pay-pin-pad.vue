<template>
	<view class="pin-page">
		<view class="pin-header">
			<text class="pin-cancel" @click="onCancel">取消</text>
			<text class="pin-title">{{ title }}</text>
			<text class="pin-placeholder" />
		</view>

		<view class="pin-body">
			<view class="pin-cells">
				<view
					v-for="i in 6"
					:key="i"
					class="pin-cell"
					:class="{ 'pin-cell--filled': digits.length >= i }"
				/>
			</view>
			<text v-if="errorHint" class="pin-error">{{ errorHint }}</text>
		</view>

		<view class="keypad" :class="{ 'keypad--disabled': busy }">
			<view class="keypad-row" v-for="(row, ri) in keypadRows" :key="ri">
				<view
					v-for="key in row"
					:key="key.id"
					class="key"
					:class="{
						'key--blank': key.blank,
						'key--action': key.action
					}"
					hover-class="busy ? '' : 'key--hover'"
					@click="onKeyTap(key)"
				>
					<text v-if="key.label" class="key-label">{{ key.label }}</text>
				</view>
			</view>
		</view>

		<view v-if="busy" class="pin-busy-mask">
			<PageLoading :text="busyText" :inline="false" :size="28" />
		</view>
	</view>
</template>

<script setup>
import { ref, watch } from 'vue'
import PageLoading from '@/components/common/page-loading.vue'

const props = defineProps({
	title: { type: String, default: '请输入支付密码' },
	/** 外部校验失败时展示 */
	errorHint: { type: String, default: '' },
	/** 递增以强制清空输入格（避免错误文案相同时 watch 不触发） */
	resetKey: { type: Number, default: 0 },
	/** 提交接口请求中，禁用键盘并展示等待动画 */
	busy: { type: Boolean, default: false },
	/** 等待遮罩文案 */
	busyText: { type: String, default: '支付处理中…' }
})

const emit = defineEmits(['complete', 'cancel'])

const digits = ref('')

const keypadRows = [
	[
		{ id: '1', label: '1' },
		{ id: '2', label: '2' },
		{ id: '3', label: '3' }
	],
	[
		{ id: '4', label: '4' },
		{ id: '5', label: '5' },
		{ id: '6', label: '6' }
	],
	[
		{ id: '7', label: '7' },
		{ id: '8', label: '8' },
		{ id: '9', label: '9' }
	],
	[
		{ id: 'blank', blank: true },
		{ id: '0', label: '0' },
		{ id: 'del', action: true, label: '⌫' }
	]
]

function clearDigits() {
	digits.value = ''
}

watch(
	() => props.resetKey,
	() => {
		clearDigits()
	}
)

watch(
	() => props.errorHint,
	(hint) => {
		if (hint) {
			clearDigits()
		}
	}
)

function onKeyTap(key) {
	if (props.busy) return
	if (key.blank) return
	if (key.action) {
		digits.value = digits.value.slice(0, -1)
		return
	}
	if (digits.value.length >= 6) return
	digits.value += key.label
	if (digits.value.length === 6) {
		emit('complete', digits.value)
	}
}

function onCancel() {
	emit('cancel')
}

/** 供父组件在步骤切换或校验失败时清空 */
function reset() {
	clearDigits()
}

defineExpose({ reset })
</script>

<style scoped>
.pin-page {
	position: relative;
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	background: #fff;
}

.pin-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 12px 16px;
	border-bottom: 1px solid #f0f0f0;
}

.pin-cancel {
	font-size: 15px;
	color: #576b95;
	min-width: 48px;
}

.pin-title {
	font-size: 17px;
	font-weight: 600;
	color: #111;
}

.pin-placeholder {
	min-width: 48px;
}

.pin-body {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding: 32px 24px 16px;
}

.pin-cells {
	display: flex;
	gap: 12px;
}

.pin-cell {
	width: 44px;
	height: 44px;
	border: 1px solid #dcdcdc;
	border-radius: 6px;
	box-sizing: border-box;
	position: relative;
}

.pin-cell--filled::after {
	content: '';
	position: absolute;
	left: 50%;
	top: 50%;
	width: 10px;
	height: 10px;
	margin: -5px 0 0 -5px;
	border-radius: 50%;
	background: #111;
}

.pin-error {
	margin-top: 16px;
	font-size: 13px;
	color: #e64340;
}

.keypad {
	background: #f7f7f7;
	padding-bottom: env(safe-area-inset-bottom);
}

.keypad-row {
	display: flex;
	border-top: 1px solid #e5e5e5;
}

.key {
	flex: 1;
	height: 108rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: #fff;
	border-right: 1px solid #e5e5e5;
	box-sizing: border-box;
}

.key:last-child {
	border-right: none;
}

.key--blank {
	background: #f7f7f7;
}

.key--action {
	background: #f7f7f7;
}

.key--hover {
	background: #ececec;
}

.key-label {
	font-size: 24px;
	color: #111;
	font-weight: 500;
}

.keypad--disabled {
	pointer-events: none;
	opacity: 0.45;
}

.pin-busy-mask {
	position: absolute;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	z-index: 100;
	display: flex;
	align-items: center;
	justify-content: center;
	background-color: rgba(255, 255, 255, 0.82);
}
</style>
