<template>
	<page-meta :page-style="pageScrollLocked ? 'overflow:hidden;height:100vh;' : ''" />
	<view class="page">
		<view v-if="loading" class="state-wrap">
			<text class="state-text">加载中…</text>
		</view>
		<template v-else>
			<view v-if="shopInfo" class="shop-header">
				<ShopImageSwiper :images="shopCarouselImages" height="160px" />
				<view class="shop-header-body">
					<view class="shop-header-top">
						<view class="shop-header-title-wrap">
							<text class="shop-header-name">{{ shopInfo.shopName || '未命名店铺' }}</text>
							<text v-if="roleLabel" class="shop-role-tag">{{ roleLabel }}</text>
						</view>
						<view class="shop-header-actions">
							<view v-if="canScanVerify" class="records-btn records-btn--primary" @click="scanForVerify">
								<text class="records-btn-text">扫码核销</text>
							</view>
							<view v-if="isManager" class="records-btn" @click="goStaff">
								<text class="records-btn-text">店员</text>
							</view>
							<view class="records-btn" @click="goCustomers">
								<text class="records-btn-text">顾客</text>
							</view>
							<view class="records-btn" @click="goStatistics">
								<text class="records-btn-text">统计</text>
							</view>
						</view>
					</view>
					<text v-if="shopInfo.ratio != null" class="shop-header-meta">1 元 = {{ shopInfo.ratio }} 积分</text>
					<text v-if="shopAddress" class="shop-header-address">{{ shopAddress }}</text>
				</view>
			</view>

			<view class="manage-tabs-wrap">
				<SegmentedTabs v-model="activeTab" :tabs="tabs" />
			</view>

			<scroll-view v-if="activeTab !== 'product'" class="scroll" scroll-y>
				<view class="section">
					<view class="section-head">
						<text class="section-title">{{ sectionTitle }}</text>
						<view v-if="canAddCurrentTab" class="section-add" @click="onAdd">
							<text class="section-add-text">+ 添加</text>
						</view>
					</view>

					<ShopManageActivityList
						v-if="activeTab === 'activity'"
						:activities="activities"
						:list-loading="listLoading"
						:is-manager="isManager"
						@edit="editActivity"
						@delete="deleteActivity"
						@stop="stopActivity"
						@enable="enableActivity"
						@add="onAdd"
					/>

					<ShopManageCouponList
					v-else-if="activeTab === 'coupon'"
					:coupons="coupons"
					:list-loading="listLoading"
					:is-manager="isManager"
					@edit="editCoupon"
					@resume="resumeCouponDistribution"
					@add-stock="promptAddCouponStock"
					@delete="deleteCoupon"
				/>
				</view>
			</scroll-view>

			<view v-else class="product-panel" :class="{ 'product-panel--checkout': canScanVerify && checkoutCartCount > 0 }">
				<view v-if="checkoutMember" class="checkout-member-bar">
					<view class="checkout-member-info">
						<text class="checkout-member-name">{{ checkoutMember.nickname || '会员' }}</text>
						<text class="checkout-member-meta">
							{{ checkoutMember.phone || '' }}
							<text v-if="checkoutMember.phone"> · </text>
							剩余 {{ checkoutMember.remainingPoints ?? 0 }} 积分
						</text>
					</view>
					<text class="checkout-member-exit" @click="exitCheckout">退出收银</text>
				</view>
				<view v-else-if="canScanVerify" class="checkout-hint-bar">
					<text class="checkout-hint-text">请先在下方选购商品，再扫描顾客付款码完成扣款；订单码可点顶部「扫码核销」直接核销</text>
				</view>

				<view class="catalog-panel">
					<view class="catalog-panel-header">
						<text class="catalog-panel-title">{{ canScanVerify ? '选购商品' : sectionTitle }}</text>
						<view
							v-if="canAddCurrentTab && !(canScanVerify && (checkoutCartCount > 0 || checkoutMember))"
							class="catalog-panel-add"
							@click="onAdd"
						>
							<text class="catalog-panel-add-text">+ 添加</text>
						</view>
					</view>

					<view v-if="listLoading" class="catalog-panel-state">
						<text class="state-text">加载中…</text>
					</view>
					<view v-else-if="!currentList.length" class="catalog-panel-state">
						<text class="state-text">暂无数据</text>
					</view>
					<view v-else class="catalog-panel-body" :style="catalogViewportStyle">
						<ProductCatalog
							:categories="productCategories"
							:purchasable="canScanVerify"
							:show-stock="true"
							:cart="checkoutCart"
							:hide-off-shelf="canScanVerify"
							@product-click="onProductCatalogClick"
							@change-qty="onCheckoutQtyChange"
						/>
					</view>
				</view>
			</view>

			<view
				v-if="canScanVerify && checkoutCartCount > 0"
				class="checkout-bar"
			>
				<view class="checkout-bar-info">
					<text class="checkout-bar-label">合计</text>
					<text class="checkout-bar-points">{{ checkoutTotalPoints }} 积分</text>
				</view>
				<view
					class="checkout-bar-btn"
					:class="{ 'checkout-bar-btn--disabled': checkoutSubmitting }"
					@click="submitCheckoutConsume"
				>
					<text class="checkout-bar-btn-text">{{ checkoutSubmitting ? '扣款中…' : (checkoutPayToken ? '确认扣款' : '扫码扣款') }}</text>
				</view>
			</view>
		</template>

		<ShopManageProductForm
			:visible="productFormVisible"
			:editing-product-id="editingProductId"
			:form="productForm"
			:category-labels="categoryLabels"
			:has-categories="categories.length > 0"
			@close="closeProductForm"
			@submit="submitProductForm"
			@category-change="onProductCategoryChange"
			@pick-image="pickProductImage"
			@clear-image="clearProductImage"
			@preview-image="previewProductImage"
			@open-category-form="openCategoryForm"
		/>

		<CategoryManageSheet
			v-model:visible="categoryFormVisible"
			:shop-id="shopId"
			:categories="categories"
			@saved="onCategoriesSaved"
		/>

		<ShopManageCouponForm
			:visible="couponFormVisible"
			:editing-coupon-id="editingCouponId"
			:editing-coupon-item="editingCouponItem"
			:form="couponForm"
			:coupon-type-labels="couponTypeLabels"
			:coupon-type-index="couponTypeIndex"
			@close="closeCouponForm"
			@submit="submitCouponForm"
			@coupon-type-change="onCouponTypeChange"
			@never-expire-change="onNeverExpireChange"
			@stop-distribution="stopCouponDistribution"
			@resume-distribution="resumeCouponDistributionFromForm"
		/>

		<ShopManageCouponStockSheet
			:visible="couponStockVisible"
			:target="couponStockTarget"
			:form="couponStockForm"
			@close="closeCouponStockForm"
			@submit="submitCouponStockForm"
			@update:quantity="couponStockForm.quantity = $event"
		/>
	</view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { fetchShopFromUserList, getShopCarouselImages, formatShopAddress, isUsableShopDetail } from '@/api/modules/shop.js'
import ShopImageSwiper from '@/components/shop/shop-image-swiper.vue'
import ProductCatalog from '@/components/shop/product-catalog.vue'
import CategoryManageSheet from '@/components/shop/category-manage-sheet.vue'
import ShopManageActivityList from '@/components/shop/shop-manage-activity-list.vue'
import ShopManageCouponList from '@/components/shop/shop-manage-coupon-list.vue'
import ShopManageProductForm from '@/components/shop/shop-manage-product-form.vue'
import ShopManageCouponForm from '@/components/shop/shop-manage-coupon-form.vue'
import ShopManageCouponStockSheet from '@/components/shop/shop-manage-coupon-stock-sheet.vue'
import SegmentedTabs from '@/components/common/segmented-tabs.vue'
import { isShopClerkCapable, isShopManager, getShopRoleLabel } from '@/utils/shop-role.js'
import { canManageShop, canStaffVerifyAtShop } from '@/utils/wx-perm.js'
import { navigateBackDelayed } from '@/utils/navigation.js'
import { rememberShopDetail, navigateWithShop, bindOpenerShop, peekShopDetail } from '@/utils/shop-page-context.js'
import { useShopManageProducts } from '@/composables/use-shop-manage-products.js'
import { useShopManageCoupons } from '@/composables/use-shop-manage-coupons.js'
import { useShopManageActivities } from '@/composables/use-shop-manage-activities.js'
import { useShopManageCheckout } from '@/composables/use-shop-manage-checkout.js'
import { productCatalogViewportStyle } from '@/utils/product-catalog-layout.js'

const catalogViewportStyle = productCatalogViewportStyle()

const shopId = ref('')
const shopInfo = ref(null)
const loading = ref(true)
const activeTab = ref('product')
const listLoading = ref(false)

const isManager = computed(() => isShopManager(shopInfo.value))
const canScanVerify = computed(() => canStaffVerifyAtShop(shopInfo.value))
const roleLabel = computed(() => getShopRoleLabel(shopInfo.value))
const canAddCurrentTab = computed(() => isManager.value)

const tabs = [
	{ key: 'product', label: '商品' },
	{ key: 'activity', label: '活动' },
	{ key: 'coupon', label: '折扣券' }
]

const productsApi = useShopManageProducts(shopId, { shopInfo, listLoading })
const {
	products,
	productCategories,
	categories,
	categoryLabels,
	currentProductList,
	productFormVisible,
	categoryFormVisible,
	editingProductId,
	productForm,
	loadCategories,
	loadProducts,
	promptAddProduct,
	editProduct,
	openProductDetail,
	onProductCategoryChange,
	pickProductImage,
	clearProductImage,
	previewProductImage,
	openCategoryForm,
	onCategoriesSaved,
	closeProductForm,
	submitProductForm
} = productsApi

const couponsApi = useShopManageCoupons(shopId, { isManager, listLoading })
const {
	coupons,
	couponStockVisible,
	couponStockTarget,
	couponStockForm,
	couponFormVisible,
	editingCouponId,
	editingCouponItem,
	couponTypeIndex,
	couponTypeLabels,
	couponForm,
	loadCoupons,
	onCouponTypeChange,
	promptAddCoupon,
	editCoupon,
	onNeverExpireChange,
	closeCouponForm,
	stopCouponDistribution,
	resumeCouponDistributionFromForm,
	resumeCouponDistribution,
	submitCouponForm,
	deleteCoupon,
	promptAddCouponStock,
	closeCouponStockForm,
	submitCouponStockForm
} = couponsApi

const {
	activities,
	needReloadActivities,
	loadActivities,
	goActivityEdit,
	editActivity,
	deleteActivity,
	stopActivity,
	enableActivity
} = useShopManageActivities(shopId, { isManager, listLoading, shopInfo })

const {
	checkoutPayToken,
	checkoutMember,
	checkoutCart,
	checkoutSubmitting,
	checkoutCartCount,
	checkoutTotalPoints,
	applyCheckoutToken,
	exitCheckout,
	onCheckoutQtyChange,
	submitCheckoutConsume,
	scanForVerify,
	shouldBlockProductClick
} = useShopManageCheckout({
	shopId,
	canScanVerify,
	productCategories,
	loadProducts,
	setActiveTab: (tab) => {
		activeTab.value = tab
	}
})

const pageScrollLocked = computed(
	() =>
		productFormVisible.value ||
		categoryFormVisible.value ||
		couponFormVisible.value ||
		couponStockVisible.value
)

const sectionTitle = computed(() => {
	if (activeTab.value === 'product') return '商品列表'
	if (activeTab.value === 'activity') return '活动列表'
	return '折扣券列表'
})

const shopCarouselImages = computed(() => getShopCarouselImages(shopInfo.value))
const shopAddress = computed(() => formatShopAddress(shopInfo.value))

const currentList = computed(() => {
	if (activeTab.value === 'product') return currentProductList.value
	if (activeTab.value === 'activity') return activities.value
	return coupons.value
})

onLoad((options) => {
	if (!canManageShop()) {
		uni.showToast({ title: '无店铺管理权限', icon: 'none' })
		navigateBackDelayed(800)
		return
	}
	shopId.value = options?.id ? String(options.id) : ''
	const pendingEditProductId = options?.editProduct ? String(options.editProduct) : ''
	const cachedShop = shopId.value ? peekShopDetail(shopId.value) : null
	if (cachedShop && isUsableShopDetail(cachedShop)) {
		shopInfo.value = cachedShop
		loading.value = false
	}
	bindOpenerShop((data) => {
		if (!isShopClerkCapable(data)) {
			uni.showToast({ title: '无店铺管理权限', icon: 'none' })
			navigateBackDelayed(800)
			return
		}
		if (isUsableShopDetail(data)) {
			shopInfo.value = data
		}
	})
	if (!shopId.value) {
		loading.value = false
		uni.showToast({ title: '缺少店铺 ID', icon: 'none' })
		return
	}
	const presetCheckoutToken = options?.checkoutToken
		? decodeURIComponent(String(options.checkoutToken))
		: ''
	void initManagePage(pendingEditProductId, presetCheckoutToken)
})

async function initManagePage(pendingEditProductId = '', presetCheckoutToken = '') {
	const hasShop = isUsableShopDetail(shopInfo.value)
	if (!hasShop) loading.value = true
	await loadShopInfo()
	loading.value = false
	await loadCategories()
	await loadProducts()
	if (pendingEditProductId) {
		const item = products.value.find((p) => String(p.productId) === pendingEditProductId)
		if (item) editProduct(item)
	}
	if (presetCheckoutToken && canScanVerify.value) {
		await applyCheckoutToken(presetCheckoutToken)
	}
}

async function loadShopInfo() {
	if (!shopId.value || shopInfo.value) return
	const res = await fetchShopFromUserList(shopId.value)
	if (res.ok && res.data) {
		shopInfo.value = rememberShopDetail(res.data) || res.data
	}
}

onShow(() => {
	if (needReloadActivities.value && shopId.value) {
		needReloadActivities.value = false
		loadActivities()
	}
})

watch(activeTab, () => {
	if (activeTab.value === 'product' && !productCategories.value.length) loadProducts()
	if (activeTab.value === 'activity' && !activities.value.length) loadActivities()
	if (activeTab.value === 'coupon' && !coupons.value.length) loadCoupons()
})

function goCustomers() {
	navigateWithShop({
		url: `/pages/shop/records?shopId=${shopId.value}`,
		shop: shopInfo.value
	})
}

function goStatistics() {
	navigateWithShop({
		url: `/pages/shop/statistics?shopId=${shopId.value}`,
		shop: shopInfo.value
	})
}

function goStaff() {
	if (!isManager.value) return
	navigateWithShop({
		url: `/pages/shop/staff?shopId=${shopId.value}`,
		shop: shopInfo.value
	})
}

function onProductCatalogClick(product) {
	if (shouldBlockProductClick()) return
	openProductDetail(product)
}

function onAdd() {
	if (activeTab.value === 'product') {
		promptAddProduct()
	} else if (activeTab.value === 'coupon') {
		promptAddCoupon()
	} else if (isManager.value) {
		goActivityEdit()
	}
}

</script>

<style scoped>
.page {
	min-height: 100vh;
	background: #f5f5f5;
	display: flex;
	flex-direction: column;
}

.product-panel--checkout {
	padding-bottom: 88px;
}

.checkout-member-bar {
	margin: 0 16px 8px;
	padding: 12px 14px;
	background: linear-gradient(135deg, #e8f2ff 0%, #f0f7ff 100%);
	border-radius: 10px;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 10px;
}

.checkout-member-info {
	flex: 1;
	min-width: 0;
}

.checkout-member-name {
	display: block;
	font-size: 15px;
	font-weight: 600;
	color: #1a3a6b;
	margin-bottom: 2px;
}

.checkout-member-meta {
	display: block;
	font-size: 12px;
	color: #4a6a9a;
}

.checkout-member-exit {
	flex-shrink: 0;
	font-size: 12px;
	color: #007aff;
	padding: 4px 8px;
}

.checkout-hint-bar {
	margin: 0 16px 8px;
	padding: 10px 12px;
	background: #fff8e8;
	border-radius: 8px;
}

.checkout-hint-text {
	font-size: 12px;
	color: #9a6b1a;
	line-height: 1.45;
}

.checkout-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 30;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
	padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
	background: #fff;
	box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.08);
}

.checkout-bar-info {
	display: flex;
	align-items: baseline;
	gap: 8px;
}

.checkout-bar-label {
	font-size: 14px;
	color: #666;
}

.checkout-bar-points {
	font-size: 20px;
	font-weight: 700;
	color: #e64340;
}

.checkout-bar-btn {
	flex-shrink: 0;
	min-width: 108px;
	height: 40px;
	padding: 0 18px;
	border-radius: 20px;
	background: linear-gradient(135deg, #3b82f6, #2563eb);
	display: flex;
	align-items: center;
	justify-content: center;
}

.checkout-bar-btn--disabled {
	opacity: 0.55;
	pointer-events: none;
}

.checkout-bar-btn-text {
	color: #fff;
	font-size: 15px;
	font-weight: 600;
}

.records-btn--primary {
	background: rgba(0, 122, 255, 0.12);
}

.records-btn--primary .records-btn-text {
	color: #007aff;
	font-weight: 600;
}

.product-panel {
	flex: 1;
	min-height: 0;
	display: flex;
	flex-direction: column;
	padding-bottom: 24px;
}

.catalog-panel {
	margin: 12px 16px 0;
	flex: 0 0 auto;
}

.catalog-panel-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 10px;
	padding: 0 2px;
}

.catalog-panel-title {
	font-size: 15px;
	font-weight: 600;
	color: #333;
}

.catalog-panel-add {
	padding: 4px 10px;
	background: #007aff;
	border-radius: 6px;
}

.catalog-panel-add-text {
	font-size: 12px;
	color: #fff;
}

.catalog-panel-state {
	padding: 20px 0;
	text-align: center;
}

.scroll {
	height: 100vh;
	box-sizing: border-box;
	padding-bottom: 24px;
}

.shop-header {
	margin: 12px 16px 0;
	background: #fff;
	border-radius: 10px;
	overflow: hidden;
}

.shop-header-body {
	padding: 12px 14px 14px;
}

.shop-header-top {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 10px;
	margin-bottom: 6px;
}

.shop-header-actions {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	justify-content: flex-end;
	gap: 8px;
	flex-shrink: 0;
	max-width: 58%;
}

.shop-header-title-wrap {
	flex: 1;
	min-width: 0;
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 6px;
}

.shop-header-name {
	font-size: 18px;
	font-weight: 600;
	color: #333;
}

.shop-role-tag {
	font-size: 11px;
	color: #007aff;
	background: rgba(0, 122, 255, 0.1);
	padding: 2px 8px;
	border-radius: 10px;
}

.records-btn {
	flex-shrink: 0;
	padding: 5px 12px;
	background: #f0f3f7;
	border-radius: 14px;
}

.records-btn-text {
	font-size: 13px;
	color: #007aff;
	font-weight: 500;
}

.shop-header-meta {
	display: block;
	font-size: 13px;
	color: #007aff;
	margin-bottom: 4px;
}

.shop-header-address {
	display: block;
	font-size: 12px;
	color: #888;
	line-height: 1.45;
}

.section {
	margin: 12px 16px 0;
	background: #fff;
	border-radius: 10px;
	padding: 14px;
}

.section-title {
	font-size: 15px;
	font-weight: 600;
	color: #333;
}

.section-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 10px;
}

.section-add {
	padding: 4px 10px;
	background: #007aff;
	border-radius: 6px;
}

.section-add-text {
	font-size: 12px;
	color: #fff;
}

.manage-tabs-wrap {
	margin: 12px 16px 0;
}

.state-wrap {
	padding: 40px 16px;
	text-align: center;
}

.state-wrap--inline {
	padding: 20px 0;
}

.state-text {
	font-size: 14px;
	color: #999;
}
</style>
