import { ref, computed, watch } from 'vue'
import {
	fetchMemberShops,
	fetchEnterShopByCode,
	joinShop,
	getShopCarouselImages,
	reportShopEnterTimeAsync
} from '@/api/modules/shop.js'
import { resolveShopInvite } from '@/api/modules/qrcode.js'
import {
	parseShopCodeFromScan,
	parseShopInviteFromScan,
	savePendingShopInvite,
	takePendingShopCode,
	takePendingShopInvite
} from '@/utils/qrcode-scan.js'
import { canShowMemberTab } from '@/utils/wx-perm.js'

/**
 * 主屏 — 会员店铺列表、搜索加入
 */
export function useMainMemberShops({ activeTab, canMemberSearch, showAddShopBtn, canMemberJoin }) {
	const memberList = ref([])
	const memberPageNum = ref(1)
	const memberLoading = ref(false)
	const memberLoadingMore = ref(false)
	const memberFinished = ref(false)
	const memberLoadError = ref('')
	const memberSearchOpen = ref(false)
	const memberSearchKeyword = ref('')
	const memberSearchFocus = ref(false)
	const memberSearchLoading = ref(false)
	const memberEnterPreview = ref(null)
	const memberJoining = ref(false)
	const memberRefreshing = ref(false)

	let memberListLoaded = false

	const memberDetailShopId = ref('')
	const memberDetailShop = ref(null)

	const memberEmptyText = computed(() => {
		if (memberLoadError.value) return memberLoadError.value
		if (canMemberSearch.value) return '暂无已加入的店铺，可通过搜索店铺代码加入'
		if (canMemberJoin.value) return '暂无已加入的店铺，可通过扫码加入'
		return '暂无已加入的店铺'
	})

	const canReturnToMemberList = computed(() => {
		if (!memberDetailShopId.value) return false
		if (memberList.value.length > 1) return true
		return canMemberJoin.value
	})

	const memberPreviewImages = computed(() => getShopCarouselImages(memberEnterPreview.value))

	const memberAlreadyJoined = computed(() => {
		const id = memberEnterPreview.value?.shopId
		if (!id) return false
		return memberList.value.some((s) => s.id === id || s.shopId === id)
	})

	const memberJoinBtnText = computed(() => {
		if (memberJoining.value) return '加入中…'
		if (memberAlreadyJoined.value) return '已加入'
		return '加入店铺'
	})

	function invalidateMemberListCache() {
		memberListLoaded = false
	}

	async function reloadMemberList(force = false) {
		if (!force && memberListLoaded) return
		memberPageNum.value = 1
		memberList.value = []
		memberFinished.value = false
		memberLoadError.value = ''
		await loadMemberPage(true)
		memberListLoaded = true
	}

	async function onMemberRefresh() {
		memberRefreshing.value = true
		invalidateMemberListCache()
		await reloadMemberList(true)
		memberRefreshing.value = false
	}

	async function loadMemberPage(reset = false) {
		if (!canShowMemberTab()) return
		if (memberLoading.value || memberLoadingMore.value) return
		if (!reset && memberFinished.value) return

		if (reset) memberLoading.value = true
		else memberLoadingMore.value = true

		const pageNum = reset ? 1 : memberPageNum.value
		const result = await fetchMemberShops({ pageNum })

		if (reset) memberLoading.value = false
		else memberLoadingMore.value = false

		if (!result.ok) {
			if (reset) memberLoadError.value = result.msg || '加载失败'
			else uni.showToast({ title: result.msg || '加载失败', icon: 'none' })
			return
		}

		memberLoadError.value = ''
		memberList.value = reset ? result.rows : [...memberList.value, ...result.rows]
		memberFinished.value = !result.hasMore
		memberPageNum.value = pageNum + 1
	}

	function loadMoreMemberShops() {
		if (activeTab.value !== 'member' || memberEnterPreview.value || memberDetailShopId.value) return
		loadMemberPage(false)
	}

	function openMemberDetail(shop) {
		const id = shop?.id ?? shop?.shopId
		if (!id) {
			uni.showToast({ title: '店铺信息无效', icon: 'none' })
			return
		}
		reportShopEnterTimeAsync(id)
		memberDetailShopId.value = String(id)
		memberDetailShop.value = shop
		closeMemberSearch()
	}

	function closeMemberDetail() {
		if (!canReturnToMemberList.value) {
			uni.showToast({ title: '无返回列表权限', icon: 'none' })
			return
		}
		memberDetailShopId.value = ''
		memberDetailShop.value = null
	}

	function tryAutoEnterSingleShop() {
		if (memberEnterPreview.value || memberDetailShopId.value) return
		if (memberList.value.length !== 1) return
		openMemberDetail(memberList.value[0])
	}

	function openMemberDetailById(shopId) {
		const id = shopId ? String(shopId) : ''
		if (!id) return
		const shop = memberList.value.find((s) => String(s.id) === id || String(s.shopId) === id)
		openMemberDetail(shop || { id })
	}

	function goAddShop() {
		if (!showAddShopBtn.value) {
			uni.showToast({ title: '无添加店铺权限', icon: 'none' })
			return
		}
		// #ifdef MP-WEIXIN
		try {
			uni.preloadPage({ url: '/pages/shop/add' })
		} catch {
			/* 预加载失败不影响跳转 */
		}
		// #endif
		uni.navigateTo({
			url: '/pages/shop/add',
			animationType: 'slide-in-right',
			animationDuration: 200
		})
	}

	function openMemberSearch() {
		if (!canMemberSearch.value) {
			uni.showToast({ title: '无搜索店铺权限', icon: 'none' })
			return
		}
		memberSearchOpen.value = true
		memberSearchFocus.value = false
		setTimeout(() => {
			memberSearchFocus.value = true
		}, 50)
	}

	function closeMemberSearch() {
		memberSearchOpen.value = false
		memberSearchKeyword.value = ''
		memberSearchFocus.value = false
		memberEnterPreview.value = null
		memberSearchLoading.value = false
	}

	async function onMemberSearchConfirm() {
		if (!canMemberSearch.value) {
			uni.showToast({ title: '无搜索店铺权限', icon: 'none' })
			return
		}
		const code = memberSearchKeyword.value.trim()
		if (!code) {
			uni.showToast({ title: '请输入店铺代码', icon: 'none' })
			return
		}
		await previewShopByCode(code)
	}

	async function previewShopByCode(code) {
		const trimmed = (code || '').trim()
		if (!trimmed) return
		uni.hideKeyboard()
		memberSearchLoading.value = true
		memberEnterPreview.value = null
		const result = await fetchEnterShopByCode(trimmed)
		memberSearchLoading.value = false
		if (!result.ok || !result.data) {
			uni.showToast({ title: result.msg || '未找到店铺', icon: 'none' })
			return
		}
		activeTab.value = 'member'
		memberSearchOpen.value = false
		memberEnterPreview.value = result.data
	}

	async function previewShopByInviteToken(token) {
		const trimmed = (token || '').trim()
		if (!trimmed) return
		uni.hideKeyboard()
		memberSearchLoading.value = true
		memberEnterPreview.value = null
		const result = await resolveShopInvite(trimmed)
		memberSearchLoading.value = false
		if (!result.ok || !result.data) {
			uni.showToast({ title: result.msg || '邀请码已过期或无效', icon: 'none' })
			return
		}
		activeTab.value = 'member'
		memberSearchOpen.value = false
		memberEnterPreview.value = result.data
	}

	function scanShopCode() {
		if (!canMemberJoin.value) {
			uni.showToast({ title: '无扫码加入权限', icon: 'none' })
			return
		}
		uni.scanCode({
			onlyFromCamera: false,
			success: (res) => {
				const inviteToken = parseShopInviteFromScan(res.result)
				if (inviteToken) {
					previewShopByInviteToken(inviteToken)
					return
				}
				const code = parseShopCodeFromScan(res.result)
				if (!code) {
					uni.showToast({ title: '无效的店铺码', icon: 'none' })
					return
				}
				previewShopByCode(code)
			},
			fail: () => {
				uni.showToast({ title: '扫码取消', icon: 'none' })
			}
		})
	}

	async function handlePendingShopCode() {
		const inviteToken = takePendingShopInvite()
		if (inviteToken) {
			if (canMemberSearch.value || canMemberJoin.value) {
				await previewShopByInviteToken(inviteToken)
			}
			return
		}
		const code = takePendingShopCode()
		if (code && canMemberSearch.value) await previewShopByCode(code)
	}

	function formatPreviewProductPoints(p) {
		const price = p?.price
		if (price == null || price === '') return '—'
		const points = Number(price)
		if (Number.isNaN(points)) return '—'
		return Number.isInteger(points) ? String(points) : String(Math.round(points * 100) / 100)
	}

	async function onJoinShop() {
		if (!canMemberJoin.value) {
			uni.showToast({ title: '无加入店铺权限', icon: 'none' })
			return
		}
		if (memberJoining.value || memberAlreadyJoined.value) return
		const shopId = memberEnterPreview.value?.shopId
		if (!shopId) {
			uni.showToast({ title: '店铺信息无效', icon: 'none' })
			return
		}
		memberJoining.value = true
		const result = await joinShop(shopId)
		memberJoining.value = false
		if (!result.ok) {
			uni.showToast({ title: result.msg || '加入失败', icon: 'none' })
			return
		}
		uni.showToast({ title: '加入成功', icon: 'success' })
		closeMemberSearch()
		invalidateMemberListCache()
		await reloadMemberList(true)
		tryAutoEnterSingleShop()
	}

	watch(activeTab, (tab) => {
		if (tab !== 'member') closeMemberSearch()
	})

	return {
		memberList,
		memberLoading,
		memberLoadingMore,
		memberFinished,
		memberLoadError,
		memberSearchOpen,
		memberSearchKeyword,
		memberSearchFocus,
		memberSearchLoading,
		memberEnterPreview,
		memberJoining,
		memberRefreshing,
		memberDetailShopId,
		memberDetailShop,
		memberEmptyText,
		memberPreviewImages,
		memberAlreadyJoined,
		memberJoinBtnText,
		canReturnToMemberList,
		invalidateMemberListCache,
		reloadMemberList,
		onMemberRefresh,
		loadMoreMemberShops,
		goAddShop,
		openMemberSearch,
		closeMemberSearch,
		onMemberSearchConfirm,
		scanShopCode,
		handlePendingShopCode,
		formatPreviewProductPoints,
		onJoinShop,
		openMemberDetail,
		closeMemberDetail,
		tryAutoEnterSingleShop,
		openMemberDetailById
	}
}
