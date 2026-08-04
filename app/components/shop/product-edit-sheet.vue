<template>
	<view v-if="visible" class="sheet-mask" @click="onClose">
		<scroll-view class="sheet-scroll" scroll-y @click.stop>
			<view class="sheet" @click.stop>
				<view class="sheet-header">
					<view class="sheet-badge">
						<text class="sheet-badge-text">品</text>
					</view>
					<text class="sheet-title">修改商品</text>
					<text class="sheet-desc">设置商品名称、分类、图片、备注与兑换积分</text>
				</view>

				<view class="sheet-block">
					<text class="sheet-block-title">商品信息</text>
					<view class="sheet-field">
						<text class="sheet-label">商品图片</text>
						<text class="sheet-hint">选填，建议上传清晰商品图</text>
						<view class="product-image-row">
							<view v-if="form.imagePath" class="product-image-preview" @click="previewImage">
								<image :src="form.imagePath" mode="aspectFill" class="product-image-img" />
								<view class="product-image-del" @click.stop="clearImage">×</view>
							</view>
							<view v-else class="product-image-add" @click="pickImage">
								<text class="product-image-add-icon">+</text>
								<text class="product-image-add-text">上传图片</text>
							</view>
						</view>
					</view>
					<view class="sheet-field">
						<text class="sheet-label">商品分类</text>
						<picker
							v-if="categories.length"
							:range="categoryLabels"
							:value="form.categoryIndex"
							@change="onCategoryChange"
						>
							<view class="sheet-picker">
								<text class="sheet-picker-text">{{ categoryLabels[form.categoryIndex] }}</text>
								<text class="sheet-picker-arrow">›</text>
							</view>
						</picker>
						<view v-else class="sheet-picker sheet-picker--disabled">
							<text class="sheet-picker-text sheet-picker-text--muted">暂无分类</text>
						</view>
					</view>
					<view class="sheet-field">
						<text class="sheet-label">商品名称</text>
						<input
							v-model="form.name"
							class="sheet-input"
							placeholder="请输入商品名称"
							placeholder-class="sheet-placeholder"
							maxlength="50"
						/>
					</view>
					<view class="sheet-field">
						<text class="sheet-label">备注说明</text>
						<text class="sheet-hint">选填，可填写商品说明或兑换须知</text>
						<textarea
							v-model="form.description"
							class="sheet-textarea"
							placeholder="如：含洗剪吹，不含烫染"
							placeholder-class="sheet-placeholder"
							maxlength="200"
							:auto-height="true"
						/>
					</view>
					<view class="sheet-field">
						<text class="sheet-label">兑换积分</text>
						<text class="sheet-hint">顾客兑换该商品需消耗的积分</text>
						<view class="sheet-input-wrap">
							<input
								v-model="form.points"
								class="sheet-input sheet-input--inset"
								type="number"
								placeholder="20"
								placeholder-class="sheet-placeholder"
							/>
							<text class="sheet-input-suffix">积分</text>
						</view>
					</view>
					<view class="sheet-field">
						<text class="sheet-label">追加库存</text>
						<text class="sheet-hint">留空表示不追加；在现有库存基础上增加</text>
						<view class="sheet-input-wrap">
							<input
								v-model="form.addStock"
								class="sheet-input sheet-input--inset"
								type="number"
								placeholder="0"
								placeholder-class="sheet-placeholder"
							/>
							<text class="sheet-input-suffix">件</text>
						</view>
					</view>
				</view>

				<view class="sheet-footer">
					<view class="sheet-footer-btn sheet-footer-btn--ghost" @click="onClose">取消</view>
					<view class="sheet-footer-btn sheet-footer-btn--primary" @click="submit">保存修改</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import {
	fetchManageProductCategories,
	updateManageProduct,
	updateManageProductStock,
	parseProductPoints
} from '@/api/modules/shop-manage.js'

const props = defineProps({
	visible: { type: Boolean, default: false },
	shopId: { type: String, default: '' },
	product: { type: Object, default: null }
})

const emit = defineEmits(['update:visible', 'saved'])

const submitting = ref(false)
const categories = ref([])
const form = ref({
	name: '',
	points: '',
	addStock: '',
	description: '',
	imagePath: '',
	categoryIndex: 0
})

const categoryLabels = computed(() => categories.value.map((c) => c.categoryName))

watch(
	() => [props.visible, props.product],
	([visible, product]) => {
		if (!visible || !product) return
		void initForm(product)
	},
	{ immediate: true }
)

async function initForm(product) {
	await loadCategories()
	const idx = categories.value.findIndex((c) => c.categoryId === product.categoryId)
	form.value = {
		name: product.productName || '',
		points: product.price != null ? String(product.price) : '',
		addStock: '',
		description: product.description || '',
		imagePath: product.imageUrl || '',
		categoryIndex: idx >= 0 ? idx : 0
	}
}

async function loadCategories() {
	if (!props.shopId) return
	const res = await fetchManageProductCategories(Number(props.shopId))
	if (res.ok) categories.value = res.rows || []
}

function selectedCategoryId() {
	const cat = categories.value[form.value.categoryIndex]
	return cat?.categoryId ?? null
}

function onCategoryChange(e) {
	form.value.categoryIndex = Number(e.detail.value) || 0
}

function pickImage() {
	uni.chooseImage({
		count: 1,
		sizeType: ['compressed'],
		sourceType: ['album', 'camera'],
		success: (res) => {
			const path = res.tempFilePaths?.[0]
			if (path) form.value.imagePath = path
		}
	})
}

function clearImage() {
	form.value.imagePath = ''
}

function previewImage() {
	const src = form.value.imagePath
	if (src) uni.previewImage({ urls: [src], current: src })
}

function onClose() {
	if (submitting.value) return
	emit('update:visible', false)
}

async function submit() {
	const productId = props.product?.productId
	if (!productId || !props.shopId) return
	const name = form.value.name?.trim()
	if (!name) {
		uni.showToast({ title: '请输入商品名称', icon: 'none' })
		return
	}
	const points = parseProductPoints(form.value.points)
	if (!points) {
		uni.showToast({ title: '请输入有效积分', icon: 'none' })
		return
	}
	if (submitting.value) return
	submitting.value = true
	const descriptionRaw = form.value.description?.trim()
	const imagePath = form.value.imagePath?.trim() || ''
	const basePayload = {
		shopId: Number(props.shopId),
		productName: name,
		price: points,
		description: descriptionRaw ?? '',
		categoryId: selectedCategoryId(),
		imagePath: imagePath || undefined
	}
	if (!imagePath) basePayload.imageUrl = ''
	const res = await updateManageProduct(productId, basePayload)
	if (res.ok) {
		const addRaw = form.value.addStock?.trim()
		if (addRaw !== '') {
			const addVal = parseInt(addRaw, 10)
			if (Number.isNaN(addVal) || addVal <= 0) {
				submitting.value = false
				uni.showToast({ title: '追加数量须为正整数', icon: 'none' })
				return
			}
			const stockRes = await updateManageProductStock(productId, addVal)
			if (!stockRes.ok) {
				submitting.value = false
				uni.showToast({ title: stockRes.msg || '库存更新失败', icon: 'none' })
				return
			}
		}
	}
	submitting.value = false
	if (!res.ok) {
		uni.showToast({ title: res.msg || '保存失败', icon: 'none' })
		return
	}
	uni.showToast({ title: '已保存', icon: 'success' })
	emit('update:visible', false)
	emit('saved', {
		productName: name,
		price: points,
		description: descriptionRaw ?? '',
		imageUrl: imagePath || props.product?.imageUrl || '',
		categoryId: selectedCategoryId()
	})
}
</script>

<style scoped>
.sheet-mask {
	position: fixed;
	inset: 0;
	z-index: 999;
	background: rgba(15, 23, 42, 0.52);
	display: flex;
	align-items: flex-end;
	justify-content: center;
	box-sizing: border-box;
}

.sheet-scroll {
	width: 100%;
	max-height: 92vh;
	box-sizing: border-box;
}

.sheet {
	width: 100%;
	background: #f4f6f9;
	border-radius: 20px 20px 0 0;
	padding: 0 16px calc(16px + env(safe-area-inset-bottom));
	box-sizing: border-box;
	overflow: hidden;
}

.sheet-header {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 20px 8px 16px;
	background: linear-gradient(180deg, #fff 0%, #f4f6f9 100%);
}

.sheet-badge {
	width: 48px;
	height: 48px;
	border-radius: 14px;
	background: linear-gradient(135deg, #5ee0a0 0%, #34c759 100%);
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 10px;
	box-shadow: 0 6px 16px rgba(52, 199, 89, 0.28);
}

.sheet-badge-text {
	font-size: 22px;
	font-weight: 700;
	color: #fff;
	line-height: 1;
}

.sheet-title {
	font-size: 18px;
	font-weight: 700;
	color: #1a1a1a;
}

.sheet-desc {
	margin-top: 4px;
	font-size: 13px;
	color: #8a94a6;
}

.sheet-block {
	background: #fff;
	border-radius: 14px;
	padding: 14px 14px 4px;
	margin-bottom: 12px;
	box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

.sheet-block-title {
	display: block;
	font-size: 13px;
	font-weight: 600;
	color: #007aff;
	margin-bottom: 12px;
	padding-left: 10px;
	position: relative;
}

.sheet-block-title::before {
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

.sheet-field {
	margin-bottom: 14px;
}

.sheet-label {
	display: block;
	font-size: 14px;
	font-weight: 600;
	color: #2c3e50;
	margin-bottom: 2px;
}

.sheet-hint {
	display: block;
	font-size: 12px;
	color: #9aa3b2;
	margin-bottom: 8px;
}

.sheet-input {
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

.sheet-input--inset {
	border: none;
	background: transparent;
	height: 44px;
	padding: 0 12px 0 14px;
	flex: 1;
	min-width: 0;
}

.sheet-placeholder {
	color: #b8c0cc;
	font-size: 15px;
}

.sheet-input-wrap {
	display: flex;
	align-items: center;
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
	overflow: hidden;
}

.sheet-input-suffix {
	padding-right: 14px;
	font-size: 14px;
	font-weight: 500;
	color: #007aff;
	flex-shrink: 0;
}

.sheet-picker {
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

.sheet-picker--disabled {
	background: #f0f3f7;
}

.sheet-picker-text {
	flex: 1;
	min-width: 0;
	font-size: 15px;
	color: #1a1a1a;
}

.sheet-picker-text--muted {
	color: #b8c0cc;
}

.sheet-picker-arrow {
	font-size: 20px;
	color: #c5cdd8;
	font-weight: 300;
	margin-left: 8px;
}

.sheet-textarea {
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

.sheet-footer {
	display: flex;
	gap: 12px;
	padding: 4px 0 8px;
}

.sheet-footer-btn {
	flex: 1;
	height: 46px;
	line-height: 46px;
	text-align: center;
	border-radius: 12px;
	font-size: 15px;
	font-weight: 600;
	box-sizing: border-box;
}

.sheet-footer-btn--ghost {
	background: #fff;
	color: #5c6678;
	border: 1px solid #e4e9f0;
}

.sheet-footer-btn--primary {
	background: linear-gradient(135deg, #6eb5ff 0%, #007aff 100%);
	color: #fff;
	box-shadow: 0 4px 14px rgba(0, 122, 255, 0.32);
}
</style>
