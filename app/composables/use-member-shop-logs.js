import { ref } from 'vue'
import { fetchMyConsumeLogList, fetchConsumeLogDetail } from '@/api/modules/points.js'
import { pointsLogStatusLabel, isPointsLogGain, pointsLogChangeAmount } from '@/utils/points-log-status.js'
import { formatPointsAmount } from '@/utils/points-format.js'
import { formatDateTime } from '@/utils/datetime-format.js'

/**
 * 会员店铺详情 — 消费记录列表与详情弹层
 */
export function useMemberShopLogs(shopId, activeTab) {
	const listRows = ref([])
	const listLoading = ref(false)
	const listLoadingMore = ref(false)
	const listFinished = ref(false)
	const pageNum = ref(1)
	const pageSize = 10
	const logsLoaded = ref(false)
	const detailVisible = ref(false)
	const logDetail = ref(null)

	function resetLogs() {
		listRows.value = []
		listLoading.value = false
		listLoadingMore.value = false
		listFinished.value = false
		pageNum.value = 1
		logsLoaded.value = false
		detailVisible.value = false
		logDetail.value = null
	}

	function reloadList() {
		pageNum.value = 1
		listRows.value = []
		listFinished.value = false
		loadList(false)
	}

	async function loadList(more) {
		const id = shopId.value
		if (!id || activeTab.value !== 'log') return
		if (listLoading.value || listLoadingMore.value) return
		if (more && listFinished.value) return

		if (more) listLoadingMore.value = true
		else listLoading.value = true

		const res = await fetchMyConsumeLogList({
			shopId: Number(id),
			pageNum: pageNum.value,
			pageSize
		})

		if (more) listLoadingMore.value = false
		else listLoading.value = false

		if (!res.ok) {
			uni.showToast({ title: res.msg || '加载失败', icon: 'none' })
			return
		}

		const rows = res.rows || []
		if (more) {
			listRows.value = listRows.value.concat(rows)
		} else {
			listRows.value = rows
		}
		listFinished.value = rows.length < pageSize
		if (!more) logsLoaded.value = true
	}

	function loadMore() {
		if (activeTab.value === 'product') return
		if (listFinished.value || listLoading.value || listLoadingMore.value) return
		pageNum.value += 1
		loadList(true)
	}

	function actionTypeLabel(type) {
		if (type === 1) return '增加'
		if (type === 2) return '消耗'
		return '—'
	}

	function statusLabel(status) {
		return pointsLogStatusLabel(status)
	}

	function pointsChangeText(row) {
		const amount = pointsLogChangeAmount(row)
		if (amount == null) return '—'
		const prefix = isPointsLogGain(row) ? '+' : '-'
		return `${prefix}${formatPointsAmount(amount)} 积分`
	}

	function pointsClass(row) {
		return isPointsLogGain(row) ? 'row-points--gain' : ''
	}

	function logPointsLabel(row) {
		if (row.status === 3) return '充值积分'
		return row.actionType === 1 ? '增加积分' : '消耗积分'
	}

	function logPointsDisplay(row) {
		const amount = pointsLogChangeAmount(row)
		if (amount == null) return '—'
		const prefix = isPointsLogGain(row) ? '+' : '-'
		return `${prefix}${formatPointsAmount(amount)} 积分`
	}

	function logPointsClass(row) {
		return isPointsLogGain(row) ? 'detail-value--gain' : ''
	}

	function formatDate(iso) {
		return formatDateTime(iso, { maxLen: 16 })
	}

	function formatReceiptPoints(item) {
		const price = item?.price
		if (price == null) return '—'
		const n = Number(price)
		if (Number.isInteger(n)) return String(n)
		return String(Math.round(n * 100) / 100)
	}

	async function openLogDetail(row) {
		if (!row?.logId) return
		const res = await fetchConsumeLogDetail(row.logId)
		if (!res.ok || !res.data) {
			uni.showToast({ title: res.msg || '加载失败', icon: 'none' })
			return
		}
		logDetail.value = res.data
		detailVisible.value = true
	}

	return {
		listRows,
		listLoading,
		listLoadingMore,
		listFinished,
		logsLoaded,
		detailVisible,
		logDetail,
		resetLogs,
		reloadList,
		loadMore,
		actionTypeLabel,
		statusLabel,
		pointsChangeText,
		pointsClass,
		logPointsLabel,
		logPointsDisplay,
		logPointsClass,
		formatDate,
		formatReceiptPoints,
		openLogDetail
	}
}
