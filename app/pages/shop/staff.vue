<template>
	<view class="page">
		<view class="action-bar">
			<view class="scan-btn" @click="scanForAddStaff">
				<text class="scan-btn-text">扫码添加店员</text>
			</view>
			<text class="action-hint">请让对方在「我的」页面出示店员邀请码</text>
		</view>

		<scroll-view
			class="scroll"
			scroll-y
			:refresher-enabled="true"
			:refresher-triggered="refreshing"
			@refresherrefresh="onRefresh"
		>
			<view class="scroll-inner">
				<view v-if="listLoading && !rows.length" class="state-wrap">
					<text class="state-text">加载中…</text>
				</view>
				<view v-else-if="!rows.length" class="state-wrap">
					<text class="state-text">暂无店员</text>
				</view>
				<view v-for="item in rows" :key="item.userId" class="list-card">
					<view class="card-head">
						<text class="card-name">{{ item.nickname || '店员' }}</text>
						<text class="card-tag">店员</text>
					</view>
					<view v-if="item.phone" class="card-sub">
						<text class="card-meta">{{ item.phone }}</text>
					</view>
					<view v-if="item.createTime" class="card-sub">
						<text class="card-meta card-meta--muted">加入时间 {{ formatDateTime(item.createTime) }}</text>
					</view>
					<view class="card-actions">
						<text class="action-btn action-btn--danger" @click="confirmRemove(item)">退出店铺</text>
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { fetchShopStaffList, removeShopStaff } from '@/api/modules/shop.js'
import { isShopManager } from '@/utils/shop-role.js'
import { canManageShop } from '@/utils/wx-perm.js'
import { scanStaffInviteCode } from '@/utils/staff-scan.js'
import { formatDateTime } from '@/utils/datetime-format.js'
import { navigateBackDelayed } from '@/utils/navigation.js'
import { bindOpenerShop } from '@/utils/shop-page-context.js'

const shopId = ref('')
const shopInfo = ref(null)
const rows = ref([])
const listLoading = ref(false)
const refreshing = ref(false)
const needReload = ref(false)

onLoad((options) => {
	if (!canManageShop()) {
		uni.showToast({ title: '无店铺管理权限', icon: 'none' })
		navigateBackDelayed(800)
		return
	}
	shopId.value = options?.shopId ? String(options.shopId) : ''
	if (!shopId.value) {
		uni.showToast({ title: '缺少店铺 ID', icon: 'none' })
		return
	}
	bindOpenerShop((data) => {
		shopInfo.value = data
		if (!isShopManager(data)) {
			uni.showToast({ title: '仅店长可管理店员', icon: 'none' })
			navigateBackDelayed(800)
		}
	})
	void reloadList()
})

onShow(() => {
	if (needReload.value && shopId.value) {
		needReload.value = false
		void reloadList()
	}
})

async function reloadList() {
	listLoading.value = true
	const res = await fetchShopStaffList(Number(shopId.value))
	listLoading.value = false
	if (!res.ok) {
		uni.showToast({ title: res.msg || '加载失败', icon: 'none' })
		return
	}
	rows.value = res.rows || []
}

async function onRefresh() {
	refreshing.value = true
	await reloadList()
	refreshing.value = false
}

function scanForAddStaff() {
	if (!shopId.value) return
	scanStaffInviteCode({
		shopId: shopId.value,
		onAdded: () => {
			needReload.value = true
			void reloadList()
		}
	})
}

function confirmRemove(item) {
	if (!item?.userId) return
	uni.showModal({
		title: '退出店铺',
		content: `确定将「${item.nickname || '该店员'}」移出店铺吗？移除后将失去店员权限。`,
		success: async (r) => {
			if (!r.confirm) return
			const res = await removeShopStaff({
				shopId: Number(shopId.value),
				userId: item.userId
			})
			if (!res.ok) {
				uni.showToast({ title: res.msg || '操作失败', icon: 'none' })
				return
			}
			uni.showToast({ title: '已移除店员', icon: 'success' })
			void reloadList()
		}
	})
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	background: #f5f5f5;
	display: flex;
	flex-direction: column;
	padding: 0 16px;
	box-sizing: border-box;
}

.action-bar {
	margin-top: 12px;
	padding: 14px;
	background: #fff;
	border-radius: 10px;
	flex-shrink: 0;
}

.scan-btn {
	height: 42px;
	border-radius: 21px;
	background: linear-gradient(135deg, #6eb5ff 0%, #007aff 100%);
	display: flex;
	align-items: center;
	justify-content: center;
}

.scan-btn-text {
	color: #fff;
	font-size: 15px;
	font-weight: 600;
}

.action-hint {
	display: block;
	margin-top: 10px;
	font-size: 12px;
	color: #888;
	line-height: 1.45;
}

.scroll {
	flex: 1;
	height: 0;
	margin: 10px 0 16px;
	background: #fff;
	border-radius: 10px;
}

.scroll-inner {
	padding: 0 14px 16px;
	box-sizing: border-box;
}

.list-card {
	border-top: 1px solid #f0f0f0;
	padding: 12px 0;
}

.list-card:first-of-type {
	border-top: none;
	padding-top: 8px;
}

.card-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 8px;
	margin-bottom: 6px;
}

.card-name {
	font-size: 15px;
	font-weight: 500;
	color: #333;
	flex: 1;
	min-width: 0;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.card-tag {
	font-size: 11px;
	color: #007aff;
	background: rgba(0, 122, 255, 0.1);
	padding: 2px 8px;
	border-radius: 10px;
	flex-shrink: 0;
}

.card-sub {
	margin-bottom: 4px;
}

.card-meta {
	font-size: 12px;
	color: #888;
}

.card-meta--muted {
	color: #aaa;
}

.card-actions {
	display: flex;
	justify-content: flex-end;
	margin-top: 8px;
}

.action-btn {
	font-size: 13px;
	color: #007aff;
}

.action-btn--danger {
	color: #e64340;
}

.state-wrap {
	padding: 40px 0;
	text-align: center;
}

.state-text {
	font-size: 14px;
	color: #999;
}
</style>
