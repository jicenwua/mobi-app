<template>
	<view v-if="visible" class="coupon-mask" @click="$emit('close')">
		<view class="product-sheet-anchor" @click.stop>
			<view class="coupon-sheet">
				<view class="coupon-sheet-header">
					<view class="coupon-sheet-badge">
						<text class="coupon-sheet-badge-text">券</text>
					</view>
					<text class="coupon-sheet-title">追加库存</text>
					<text class="coupon-sheet-desc">{{ target?.couponName || '' }}</text>
				</view>
				<view class="coupon-block">
					<view class="coupon-field">
						<text class="coupon-label">追加数量</text>
						<view class="coupon-input-wrap coupon-input-wrap--suffix">
							<input
								:value="form.quantity"
								class="coupon-input coupon-input--inset"
								type="number"
								placeholder="50"
								placeholder-class="coupon-placeholder"
								@input="$emit('update:quantity', $event.detail.value)"
							/>
							<text class="coupon-input-suffix">张</text>
						</view>
					</view>
				</view>
				<view class="coupon-sheet-footer">
					<view class="coupon-footer-btn coupon-footer-btn--ghost" @click="$emit('close')">取消</view>
					<view class="coupon-footer-btn coupon-footer-btn--primary" @click="$emit('submit')">确认追加</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
defineProps({
	visible: { type: Boolean, default: false },
	target: { type: Object, default: null },
	form: { type: Object, default: () => ({ quantity: '' }) }
})

defineEmits(['close', 'submit', 'update:quantity'])
</script>

<style scoped>
.coupon-mask {
	position: fixed;
	inset: 0;
	z-index: 999;
	background: rgba(15, 23, 42, 0.52);
	display: flex;
	align-items: flex-end;
	justify-content: center;
	box-sizing: border-box;
}

.product-sheet-anchor {
	width: 100%;
	box-sizing: border-box;
}

.coupon-sheet {
	width: 100%;
	background: #f4f6f9;
	border-radius: 20px 20px 0 0;
	padding: 0 16px calc(16px + env(safe-area-inset-bottom));
	box-sizing: border-box;
	overflow: hidden;
}

.coupon-sheet-header {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 20px 8px 16px;
	background: linear-gradient(180deg, #fff 0%, #f4f6f9 100%);
}

.coupon-sheet-badge {
	width: 48px;
	height: 48px;
	border-radius: 14px;
	background: linear-gradient(135deg, #6eb5ff 0%, #007aff 100%);
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 10px;
	box-shadow: 0 6px 16px rgba(0, 122, 255, 0.28);
}

.coupon-sheet-badge-text {
	font-size: 22px;
	font-weight: 700;
	color: #fff;
	line-height: 1;
}

.coupon-sheet-title {
	font-size: 18px;
	font-weight: 700;
	color: #1a1a1a;
	letter-spacing: 0.3px;
}

.coupon-sheet-desc {
	margin-top: 4px;
	font-size: 13px;
	color: #8a94a6;
}

.coupon-block {
	background: #fff;
	border-radius: 14px;
	padding: 14px 14px 4px;
	margin-bottom: 12px;
	box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

.coupon-field {
	margin-bottom: 14px;
}

.coupon-label {
	display: block;
	font-size: 14px;
	font-weight: 600;
	color: #2c3e50;
	margin-bottom: 2px;
}

.coupon-input {
	width: 100%;
	height: 44px;
	padding: 0 14px;
	box-sizing: border-box;
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
	font-size: 15px;
	color: #1a1a1a;
}

.coupon-input--inset {
	border: none;
	background: transparent;
	height: 44px;
	padding: 0 12px 0 14px;
	flex: 1;
	min-width: 0;
}

.coupon-placeholder {
	color: #b8c0cc;
	font-size: 15px;
}

.coupon-input-wrap {
	display: flex;
	align-items: center;
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
	overflow: hidden;
}

.coupon-input-wrap--suffix .coupon-input-suffix {
	padding-right: 14px;
	font-size: 14px;
	font-weight: 500;
	color: #007aff;
	flex-shrink: 0;
}

.coupon-sheet-footer {
	display: flex;
	gap: 12px;
	padding: 4px 0 8px;
}

.coupon-footer-btn {
	flex: 1;
	height: 46px;
	line-height: 46px;
	text-align: center;
	border-radius: 12px;
	font-size: 15px;
	font-weight: 600;
	box-sizing: border-box;
}

.coupon-footer-btn--ghost {
	background: #fff;
	color: #5c6678;
	border: 1px solid #e4e9f0;
}

.coupon-footer-btn--primary {
	background: linear-gradient(135deg, #6eb5ff 0%, #007aff 100%);
	color: #fff;
	box-shadow: 0 4px 14px rgba(0, 122, 255, 0.32);
}
</style>
