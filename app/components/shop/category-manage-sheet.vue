<template>
	<view v-if="visible" class="sheet-mask" @click="onMaskClick" @touchmove.stop="noop">
		<view class="sheet-panel" @click.stop>
			<view class="sheet-header">
				<view class="sheet-badge">
					<text class="sheet-badge-text">类</text>
				</view>
				<text class="sheet-title">管理分类</text>
				<text class="sheet-desc">使用上移/下移调整顺序，完成后统一保存</text>
			</view>

			<view class="sheet-body">
				<view class="sheet-block">
					<text class="sheet-block-title">分类列表</text>
					<view v-if="!draftItems.length" class="category-empty">
						<text class="category-empty-text">暂无分类，可在下方添加</text>
					</view>
					<view v-for="(item, index) in draftItems" :key="item.key" class="category-row">
						<view class="category-sort-btns">
							<view
								class="sort-btn"
								:class="{ 'sort-btn--disabled': index === 0 }"
								@click="moveItemUp(index)"
							>
								<text class="sort-btn-icon">↑</text>
							</view>
							<view
								class="sort-btn"
								:class="{ 'sort-btn--disabled': index === draftItems.length - 1 }"
								@click="moveItemDown(index)"
							>
								<text class="sort-btn-icon">↓</text>
							</view>
						</view>
						<input
							v-model="item.categoryName"
							class="category-name-input"
							placeholder="分类名称"
							placeholder-class="category-name-placeholder"
							maxlength="20"
						/>
						<text class="action-btn action-btn--danger" @click="removeItem(index)">删除</text>
					</view>
				</view>

				<view class="sheet-block">
					<text class="sheet-block-title">新增分类</text>
					<view class="category-add-row">
						<input
							v-model="newName"
							class="category-add-input"
							placeholder="如：洗剪吹、烫染"
							placeholder-class="category-name-placeholder"
							maxlength="20"
							confirm-type="done"
							@confirm="addToDraft"
						/>
						<view class="category-add-btn" @click="addToDraft">
							<text class="category-add-btn-text">加入列表</text>
						</view>
					</view>
					<text class="category-add-hint">可连续添加多个分类，点击完成后统一上传</text>
				</view>
			</view>

			<view class="sheet-footer">
				<view class="sheet-footer-btn sheet-footer-btn--ghost" @click="onCancel">取消</view>
				<view class="sheet-footer-btn sheet-footer-btn--primary" @click="onComplete">
					{{ saving ? '保存中…' : '完成' }}
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, watch } from 'vue'
import {
	addManageProductCategories,
	updateManageProductCategories,
	deleteManageProductCategory
} from '@/api/modules/shop-manage.js'

const props = defineProps({
	visible: { type: Boolean, default: false },
	shopId: { type: String, default: '' },
	categories: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:visible', 'saved'])

const draftItems = ref([])
const deletedIds = ref([])
const newName = ref('')
const saving = ref(false)
let newKeyCounter = 0

watch(
	() => props.visible,
	(visible) => {
		if (visible) initDraft(props.categories)
	}
)

function noop() {}

function initDraft(categories) {
	newKeyCounter = 0
	deletedIds.value = []
	newName.value = ''
	draftItems.value = (categories || []).map((c, i) => ({
		key: `id-${c.categoryId}`,
		categoryId: c.categoryId,
		categoryName: c.categoryName || '',
		originalName: c.categoryName || '',
		originalIndex: i
	}))
}

function isDirty() {
	if (deletedIds.value.length) return true
	if (newName.value.trim()) return true
	return draftItems.value.some((item, index) => {
		if (!item.categoryId) return true
		return item.categoryName.trim() !== item.originalName || index !== item.originalIndex
	})
}

function moveItem(from, to) {
	if (from === to || from < 0 || to < 0 || to >= draftItems.value.length) return
	const list = [...draftItems.value]
	const [item] = list.splice(from, 1)
	list.splice(to, 0, item)
	draftItems.value = list
}

function moveItemUp(index) {
	if (index <= 0) return
	moveItem(index, index - 1)
}

function moveItemDown(index) {
	if (index >= draftItems.value.length - 1) return
	moveItem(index, index + 1)
}

function addToDraft() {
	const name = newName.value?.trim()
	if (!name) {
		uni.showToast({ title: '请输入分类名称', icon: 'none' })
		return
	}
	if (hasDuplicateName(name)) {
		uni.showToast({ title: '分类名称已存在', icon: 'none' })
		return
	}
	newKeyCounter += 1
	draftItems.value.push({
		key: `new-${newKeyCounter}`,
		categoryId: null,
		categoryName: name,
		originalName: null,
		originalIndex: null
	})
	newName.value = ''
}

function hasDuplicateName(name, ignoreIndex = -1) {
	const normalized = name.trim()
	return draftItems.value.some((item, index) => index !== ignoreIndex && item.categoryName.trim() === normalized)
}

function removeItem(index) {
	const item = draftItems.value[index]
	const doRemove = () => {
		if (item.categoryId) deletedIds.value.push(item.categoryId)
		draftItems.value.splice(index, 1)
	}
	if (!item.categoryId) {
		doRemove()
		return
	}
	uni.showModal({
		title: '删除分类',
		content: `确定删除「${item.categoryName}」吗？分类下仍有商品时无法删除。`,
		success: (r) => {
			if (r.confirm) doRemove()
		}
	})
}

function onMaskClick() {
	onCancel()
}

function onCancel() {
	if (saving.value) return
	if (!isDirty()) {
		emit('update:visible', false)
		return
	}
	uni.showModal({
		title: '放弃修改',
		content: '有未保存的修改，确定放弃吗？',
		success: (r) => {
			if (r.confirm) emit('update:visible', false)
		}
	})
}

async function onComplete() {
	if (saving.value) return
	const pendingName = newName.value?.trim()
	if (pendingName) {
		if (hasDuplicateName(pendingName)) {
			uni.showToast({ title: '分类名称已存在', icon: 'none' })
			return
		}
		addToDraft()
	}
	const items = draftItems.value
	if (!items.length && !deletedIds.value.length) {
		emit('update:visible', false)
		return
	}
	for (let i = 0; i < items.length; i++) {
		const name = items[i].categoryName?.trim()
		if (!name) {
			uni.showToast({ title: `第 ${i + 1} 项分类名称不能为空`, icon: 'none' })
			return
		}
		if (hasDuplicateName(name, i)) {
			uni.showToast({ title: '分类名称不能重复', icon: 'none' })
			return
		}
	}
	if (!props.shopId) {
		uni.showToast({ title: '缺少店铺 ID', icon: 'none' })
		return
	}
	if (!isDirty()) {
		emit('update:visible', false)
		return
	}
	saving.value = true
	const shopIdNum = Number(props.shopId)
	for (const categoryId of deletedIds.value) {
		const res = await deleteManageProductCategory(categoryId)
		if (!res.ok) {
			saving.value = false
			uni.showToast({ title: res.msg || '删除失败', icon: 'none' })
			return
		}
	}
	const newEntries = items
		.map((item, index) => ({ item, index }))
		.filter(({ item }) => !item.categoryId)
	if (newEntries.length) {
		const res = await addManageProductCategories(
			newEntries.map(({ item, index }) => ({
				shopId: shopIdNum,
				categoryName: item.categoryName.trim(),
				sortOrder: index
			}))
		)
		if (!res.ok) {
			saving.value = false
			uni.showToast({ title: res.msg || '添加失败', icon: 'none' })
			return
		}
	}
	const updates = []
	for (let i = 0; i < items.length; i++) {
		const item = items[i]
		if (!item.categoryId) continue
		const name = item.categoryName.trim()
		const nameChanged = name !== item.originalName
		const sortChanged = i !== item.originalIndex
		if (!nameChanged && !sortChanged) continue
		const entry = { categoryId: item.categoryId }
		if (nameChanged) entry.categoryName = name
		if (sortChanged) entry.sortOrder = i
		updates.push(entry)
	}
	if (updates.length) {
		const res = await updateManageProductCategories(updates)
		if (!res.ok) {
			saving.value = false
			uni.showToast({ title: res.msg || '保存失败', icon: 'none' })
			return
		}
	}
	saving.value = false
	uni.showToast({ title: '已保存', icon: 'success' })
	emit('saved')
	emit('update:visible', false)
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

.sheet-panel {
	width: 100%;
	max-height: 92vh;
	display: flex;
	flex-direction: column;
	background: #f4f6f9;
	border-radius: 20px 20px 0 0;
	padding: 0 16px calc(16px + env(safe-area-inset-bottom));
	box-sizing: border-box;
	overflow: hidden;
}

.sheet-body {
	flex: 1;
	min-height: 0;
	overflow-y: auto;
}

.sheet-header {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 20px 8px 16px;
	background: linear-gradient(180deg, #fff 0%, #f4f6f9 100%);
	flex-shrink: 0;
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
	text-align: center;
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

.category-empty {
	padding: 8px 0 12px;
}

.category-empty-text {
	font-size: 13px;
	color: #999;
}

.category-row {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 8px 0;
	border-top: 1px solid #f0f0f0;
}

.category-row:first-of-type {
	border-top: none;
}

.category-sort-btns {
	display: flex;
	flex-direction: column;
	gap: 2px;
	flex-shrink: 0;
}

.sort-btn {
	width: 28px;
	height: 22px;
	display: flex;
	align-items: center;
	justify-content: center;
	background: #f0f3f7;
	border-radius: 6px;
}

.sort-btn--disabled {
	opacity: 0.35;
}

.sort-btn-icon {
	font-size: 14px;
	color: #007aff;
	line-height: 1;
	font-weight: 700;
}

.category-name-input {
	flex: 1;
	min-width: 0;
	height: 44px;
	padding: 0 12px;
	box-sizing: border-box;
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
	font-size: 15px;
	color: #1a1a1a;
}

.category-name-placeholder {
	color: #b8c0cc;
	font-size: 15px;
}

.action-btn {
	font-size: 13px;
	color: #007aff;
	flex-shrink: 0;
}

.action-btn--danger {
	color: #e64340;
}

.category-add-row {
	display: flex;
	gap: 10px;
	align-items: center;
	margin-bottom: 8px;
}

.category-add-input {
	flex: 1;
	min-width: 0;
	height: 44px;
	padding: 0 14px;
	box-sizing: border-box;
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
	font-size: 15px;
	color: #1a1a1a;
}

.category-add-btn {
	flex-shrink: 0;
	padding: 0 14px;
	height: 44px;
	line-height: 44px;
	background: #007aff;
	border-radius: 10px;
}

.category-add-btn-text {
	font-size: 14px;
	color: #fff;
	font-weight: 600;
}

.category-add-hint {
	display: block;
	font-size: 12px;
	color: #9aa3b2;
	margin-bottom: 10px;
}

.sheet-footer {
	display: flex;
	gap: 12px;
	padding: 4px 0 8px;
	flex-shrink: 0;
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
