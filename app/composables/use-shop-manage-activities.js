import { ref } from 'vue'
import {
	fetchManageActivities,
	deleteManageActivity,
	stopManageActivity,
	enableManageActivity
} from '@/api/modules/shop-manage.js'
import { withClickLock } from '@/utils/with-click-lock.js'
import { navigateWithShop } from '@/utils/shop-page-context.js'

function showModalAsync(options) {
	return new Promise((resolve) => {
		uni.showModal({
			...options,
			success: resolve
		})
	})
}

/**
 * 店铺管理页 — 活动列表与操作
 */
export function useShopManageActivities(shopId, { isManager, listLoading, shopInfo }) {
	const activities = ref([])
	const needReloadActivities = ref(false)

	async function loadActivities() {
		if (listLoading.value) return
		listLoading.value = true
		const res = await fetchManageActivities(Number(shopId.value), 1, 50)
		listLoading.value = false
		if (!res.ok) {
			uni.showToast({ title: res.msg || '加载失败', icon: 'none' })
			return
		}
		activities.value = res.rows
	}

	function goActivityEdit(activityId) {
		if (!isManager.value) {
			uni.showToast({ title: '仅店长可编辑活动', icon: 'none' })
			return
		}
		const q = activityId ? `&activityId=${activityId}` : ''
		needReloadActivities.value = true
		navigateWithShop({
			url: `/pages/shop/activity-edit?shopId=${shopId.value}${q}`,
			shop: shopInfo?.value
		})
	}

	function editActivity(item) {
		goActivityEdit(item.activityId)
	}

	const deleteActivity = withClickLock(async (item) => {
		if (!isManager.value) {
			uni.showToast({ title: '仅店长可删除活动', icon: 'none' })
			return
		}
		const r = await showModalAsync({
			title: '删除活动',
			content: `确定删除「${item.activityName}」吗？`
		})
		if (!r.confirm) return
		const res = await deleteManageActivity(item.activityId)
		if (!res.ok) {
			uni.showToast({ title: res.msg || '删除失败', icon: 'none' })
			return
		}
		uni.showToast({ title: '已删除', icon: 'success' })
		loadActivities()
	})

	const stopActivity = withClickLock(async (item) => {
		if (!isManager.value) {
			uni.showToast({ title: '仅店长可停止活动', icon: 'none' })
			return
		}
		const r = await showModalAsync({
			title: '停止活动',
			content: `确定手动停止「${item.activityName}」吗？`
		})
		if (!r.confirm) return
		const res = await stopManageActivity(item.activityId, shopId.value)
		if (!res.ok) {
			uni.showToast({ title: res.msg || '操作失败', icon: 'none' })
			return
		}
		uni.showToast({ title: '已停止', icon: 'success' })
		loadActivities()
	})

	const enableActivity = withClickLock(async (item) => {
		if (!isManager.value) {
			uni.showToast({ title: '仅店长可启用活动', icon: 'none' })
			return
		}
		const r = await showModalAsync({
			title: '启用活动',
			content: '启用后将按当前活动时间重新生效，确定启用吗？'
		})
		if (!r.confirm) return
		const res = await enableManageActivity(item.activityId, shopId.value)
		if (!res.ok) {
			uni.showToast({ title: res.msg || '操作失败', icon: 'none' })
			return
		}
		uni.showToast({ title: '已启用', icon: 'success' })
		loadActivities()
	})

	return {
		activities,
		needReloadActivities,
		loadActivities,
		goActivityEdit,
		editActivity,
		deleteActivity,
		stopActivity,
		enableActivity
	}
}
