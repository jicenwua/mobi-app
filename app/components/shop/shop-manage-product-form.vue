<template>
	<view v-if="visible" class="coupon-mask" @click="$emit('close')">
		<scroll-view class="coupon-sheet-scroll" scroll-y @click.stop>
			<view class="coupon-sheet" @click.stop>
				<view class="coupon-sheet-header">
					<view class="coupon-sheet-badge coupon-sheet-badge--product">
						<text class="coupon-sheet-badge-text">品</text>
					</view>
					<text class="coupon-sheet-title">{{ editingProductId ? '修改商品' : '添加商品' }}</text>
					<text class="coupon-sheet-desc">设置商品名称、分类、图片、备注与兑换积分</text>
				</view>

				<view class="coupon-block">
					<text class="coupon-block-title">商品信息</text>
					<view class="coupon-field">
						<text class="coupon-label">商品图片</text>
						<text class="coupon-hint">选填，建议上传清晰商品图</text>
						<view class="product-image-row">
							<view v-if="form.imagePath" class="product-image-preview" @click="$emit('preview-image')">
								<image :src="form.imagePath" mode="aspectFill" class="product-image-img" />
								<view class="product-image-del" @click.stop="$emit('clear-image')">×</view>
							</view>
							<view v-else class="product-image-add" @click="$emit('pick-image')">
								<text class="product-image-add-icon">+</text>
								<text class="product-image-add-text">上传图片</text>
							</view>
						</view>
					</view>
					<view class="coupon-field">
						<view class="coupon-label-row">
							<text class="coupon-label coupon-label--inline">商品分类</text>
							<text class="category-manage-link" @click="$emit('open-category-form')">管理分类</text>
						</view>
						<picker
							v-if="hasCategories"
							:range="categoryLabels"
							:value="form.categoryIndex"
							@change="$emit('category-change', $event)"
						>
							<view class="coupon-picker">
								<text class="coupon-picker-text">{{ categoryLabels[form.categoryIndex] }}</text>
								<text class="coupon-picker-arrow">›</text>
							</view>
						</picker>
						<view v-else class="coupon-picker coupon-picker--disabled" @click="$emit('open-category-form')">
							<text class="coupon-picker-text coupon-picker-text--muted">暂无分类，点击管理分类添加</text>
							<text class="coupon-picker-arrow">›</text>
						</view>
					</view>
					<view class="coupon-field">
						<text class="coupon-label">商品名称</text>
						<input
							v-model="form.name"
							class="coupon-input"
							placeholder="请输入商品名称"
							placeholder-class="coupon-placeholder"
							maxlength="50"
						/>
					</view>
					<view class="coupon-field">
						<text class="coupon-label">备注说明</text>
						<text class="coupon-hint">选填，可填写商品说明或兑换须知</text>
						<textarea
							v-model="form.description"
							class="coupon-textarea"
							placeholder="如：含洗剪吹，不含烫染"
							placeholder-class="coupon-placeholder"
							maxlength="200"
							:auto-height="true"
						/>
					</view>
					<view class="coupon-field">
						<text class="coupon-label">兑换积分</text>
						<text class="coupon-hint">顾客兑换该商品需消耗的积分</text>
						<view class="coupon-input-wrap coupon-input-wrap--suffix">
							<input
								v-model="form.points"
								class="coupon-input coupon-input--inset"
								type="number"
								placeholder="20"
								placeholder-class="coupon-placeholder"
							/>
							<text class="coupon-input-suffix">积分</text>
						</view>
					</view>
					<view v-if="!editingProductId" class="coupon-field">
						<text class="coupon-label">初始库存</text>
						<text class="coupon-hint">不填表示无限库存；填写正整数为有限库存</text>
						<view class="coupon-input-wrap coupon-input-wrap--suffix">
							<input
								v-model="form.stock"
								class="coupon-input coupon-input--inset"
								type="number"
								placeholder="不限"
								placeholder-class="coupon-placeholder"
							/>
							<text class="coupon-input-suffix">件</text>
						</view>
					</view>
					<view v-if="editingProductId" class="coupon-field">
						<text class="coupon-label">追加库存</text>
						<text class="coupon-hint">留空表示不追加；在现有库存基础上增加</text>
						<view class="coupon-input-wrap coupon-input-wrap--suffix">
							<input
								v-model="form.addStock"
								class="coupon-input coupon-input--inset"
								type="number"
								placeholder="0"
								placeholder-class="coupon-placeholder"
							/>
							<text class="coupon-input-suffix">件</text>
						</view>
					</view>
				</view>

				<view class="coupon-sheet-footer">
					<view class="coupon-footer-btn coupon-footer-btn--ghost" @click="$emit('close')">取消</view>
					<view class="coupon-footer-btn coupon-footer-btn--primary" @click="$emit('submit')">
						{{ editingProductId ? '保存修改' : '添加商品' }}
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
defineProps({
	visible: { type: Boolean, default: false },
	editingProductId: { type: [String, Number], default: null },
	form: { type: Object, required: true },
	categoryLabels: { type: Array, default: () => [] },
	hasCategories: { type: Boolean, default: false }
})

defineEmits(['close', 'submit', 'category-change', 'pick-image', 'clear-image', 'preview-image', 'open-category-form'])
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

.coupon-sheet-scroll {
	width: 100%;
	max-height: 92vh;
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

.coupon-sheet-badge--product {
	background: linear-gradient(135deg, #5ee0a0 0%, #34c759 100%);
	box-shadow: 0 6px 16px rgba(52, 199, 89, 0.28);
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

.coupon-block-title {
	display: block;
	font-size: 13px;
	font-weight: 600;
	color: #007aff;
	margin-bottom: 12px;
	padding-left: 10px;
	position: relative;
}

.coupon-block-title::before {
	content: '';
	position: absolute;
	left: 0;
	top: 50%;
	transform: translateY(-50%);
	width: 3px;
	height: 14px;
	border-radius: 2px;
	background: linear-gradient(180deg, #6eb5ff, #007aff);
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

.coupon-label--inline {
	margin-bottom: 0;
}

.coupon-hint {
	display: block;
	font-size: 12px;
	color: #9aa3b2;
	margin-bottom: 8px;
}

.coupon-label-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 8px;
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
	transition: border-color 0.2s;
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

.coupon-picker {
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 44px;
	padding: 0 14px;
	box-sizing: border-box;
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
}

.coupon-picker--disabled {
	background: #f0f3f7;
	border-color: #e8ecf0;
}

.coupon-picker-text {
	flex: 1;
	min-width: 0;
	font-size: 15px;
	color: #1a1a1a;
}

.coupon-picker-text--muted {
	color: #b8c0cc;
}

.coupon-picker-arrow {
	font-size: 20px;
	color: #c5cdd8;
	font-weight: 300;
	line-height: 1;
	margin-left: 8px;
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

.coupon-textarea {
	width: 100%;
	min-height: 72px;
	padding: 10px 14px;
	box-sizing: border-box;
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
	font-size: 15px;
	color: #1a1a1a;
	line-height: 1.5;
}

.category-manage-link {
	font-size: 13px;
	color: #007aff;
	font-weight: 500;
}

.product-image-row {
	display: flex;
	align-items: center;
}

.product-image-add,
.product-image-preview {
	width: 88px;
	height: 88px;
	border-radius: 10px;
	overflow: hidden;
	position: relative;
}

.product-image-add {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	background: #f7f9fc;
	border: 1px dashed #c5cdd8;
}

.product-image-add-icon {
	font-size: 28px;
	color: #007aff;
	line-height: 1;
}

.product-image-add-text {
	margin-top: 4px;
	font-size: 12px;
	color: #888;
}

.product-image-img {
	width: 100%;
	height: 100%;
}

.product-image-del {
	position: absolute;
	top: 4px;
	right: 4px;
	width: 22px;
	height: 22px;
	line-height: 20px;
	text-align: center;
	border-radius: 50%;
	background: rgba(0, 0, 0, 0.55);
	color: #fff;
	font-size: 16px;
}
</style>
