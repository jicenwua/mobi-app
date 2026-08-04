import { ref, computed } from 'vue'

/**
 * 分页列表：下拉刷新、触底加载、reload
 * @param {object} options
 * @param {(pageNum: number, pageSize: number) => Promise<{ ok: boolean, rows?: array, total?: number, msg?: string }>} options.fetchPage
 * @param {number} [options.pageSize=20]
 */
export function usePaginatedList({ fetchPage, pageSize = 20 }) {
	const rows = ref([])
	const listLoading = ref(false)
	const loadingMore = ref(false)
	const refreshing = ref(false)
	const pageNum = ref(1)
	const total = ref(0)

	const finished = computed(() => rows.value.length >= total.value)

	async function loadPage(page, append) {
		if (append) {
			loadingMore.value = true
		} else {
			listLoading.value = true
		}
		const res = await fetchPage(page, pageSize)
		if (append) {
			loadingMore.value = false
		} else {
			listLoading.value = false
		}
		if (!res.ok) {
			uni.showToast({ title: res.msg || '加载失败', icon: 'none' })
			return false
		}
		pageNum.value = page
		const nextRows = res.rows || []
		const reportedTotal = Number(res.total)
		if (reportedTotal > 0) {
			total.value = reportedTotal
		} else if (!append) {
			total.value = nextRows.length
		}
		rows.value = append ? rows.value.concat(nextRows) : nextRows
		return true
	}

	async function reloadList() {
		pageNum.value = 1
		total.value = 0
		rows.value = []
		return loadPage(1, false)
	}

	async function onRefresh() {
		refreshing.value = true
		await reloadList()
		refreshing.value = false
	}

	async function loadMore() {
		if (listLoading.value || loadingMore.value || finished.value) return
		await loadPage(pageNum.value + 1, true)
	}

	return {
		rows,
		listLoading,
		loadingMore,
		refreshing,
		pageNum,
		total,
		finished,
		reloadList,
		onRefresh,
		loadMore
	}
}
