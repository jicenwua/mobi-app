<template>
	<view
		class="detail-root"
		:class="[
			isDark ? 'detail-root--dark' : 'detail-root--light',
			embedded ? 'detail-root--embedded' : 'detail-root--page',
			cartItemCount > 0 ? 'detail-root--with-cart' : ''
		]"
	>
		<view v-if="loading" class="state-wrap">
			<text class="state-text">加载中…</text>
		</view>
		<template v-else-if="detail">
			<view class="hero-wrap">
				<ShopImageSwiper :images="carouselImages" :is-dark="isDark" height="220px" />
				<view class="hero-fade" />
			</view>

			<view class="shop-info-card">
				<view class="shop-title-bar">
					<text class="shop-title">{{ detail.shopName || '未命名店铺' }}</text>
					<view v-if="showManageLink || showScanVerifyBtn" class="shop-title-actions">
						<text
							v-if="showScanVerifyBtn"
							class="shop-action-link"
							hover-class="tap-hover-opacity"
							:hover-stay-time="70"
							@click="scanForVerify"
						>扫码核销</text>
						<text
							v-if="showManageLink"
							class="shop-action-link"
							hover-class="tap-hover-opacity"
							:hover-stay-time="70"
							@click="goManage"
						>管理</text>
					</view>
				</view>
				<view v-if="detail.ratio != null" class="ratio-row">
					<view class="ratio-chip">
						<text class="ratio-text">1 元 = {{ detail.ratio }} 积分</text>
					</view>
					<text class="ratio-note">（线上不提供积分充值服务，只展示积分兑换率，请到店铺线下充值）</text>
				</view>
				<view v-if="shopPhone || shopAddress" class="shop-meta-row">
					<view
						v-if="shopPhone"
						class="shop-meta-phone"
						hover-class="tap-hover-opacity"
						:hover-stay-time="70"
						@click="callShopPhone"
					>
						<text class="shop-meta-icon">📞</text>
						<text class="shop-meta-phone-text">{{ shopPhone }}</text>
					</view>
					<view v-if="shopAddress" class="shop-meta-address">
						<text class="shop-meta-icon">📍</text>
						<text class="shop-meta-address-text">{{ shopAddress }}</text>
					</view>
				</view>
				<view class="points-summary">
					<view class="points-summary-left">
						<text class="points-summary-icon">✦</text>
						<text class="points-summary-label">剩余积分</text>
					</view>
					<view class="points-gold">
						<text class="points-gold-num">{{ remainingPoints }}</text>
						<text class="points-gold-unit">积分</text>
					</view>
				</view>
				<view
					v-if="canPayQrcode"
					class="pay-qrcode-btn"
					hover-class="tap-hover-scale-soft"
					:hover-stay-time="70"
					@click="goPayQrcode"
				>
					<text class="pay-qrcode-text">出示积分码</text>
				</view>
			</view>

			<view class="list-scroll">
				<view v-if="productsLoading && !products.length" class="state-wrap">
					<text class="state-text">加载商品中…</text>
				</view>
				<view v-else-if="!products.length" class="state-wrap">
					<text class="state-text">暂无商品</text>
				</view>
				<view v-else class="catalog-panel-body" :style="catalogViewportStyle">
					<ProductCatalog
						class="catalog-panel-catalog"
						:categories="productCategories"
						:is-dark="isDark"
						:hide-off-shelf="true"
						:purchasable="true"
						:cart="cart"
						@product-click="openProductDetail"
						@change-qty="onCartQtyChange"
					/>
				</view>
			</view>

			<MemberShopCartBar
				:cart-lines="cartLines"
				:cart-total-points-text="cartTotalPointsText"
				:cart-item-count="cartItemCount"
				:is-dark="isDark"
				:checking-out="checkingOut"
				:embedded="embedded"
				@update-qty="onCartQtyUpdate"
				@checkout="submitPurchase"
			/>
		</template>
		<view v-else class="state-wrap">
			<text class="state-text">{{ errorMsg || '店铺不存在' }}</text>
		</view>

		<view v-if="activityModalVisible && currentPromoItem" class="modal-mask activity-modal-mask" @click="closeActivityModal">
			<view class="modal-panel activity-modal-panel" :class="isDark ? 'modal-panel--dark' : ''" @click.stop>
				<text class="modal-title modal-title--center">{{ currentPromoItem.type === 'announcement' ? '公告' : '活动' }}</text>
				<view v-if="promoItems.length > 1" class="activity-nav">
					<view
						class="activity-nav-btn"
						:class="{ 'activity-nav-btn--disabled': promoItems.length <= 1 }"
						:hover-class="promoItems.length <= 1 ? 'none' : 'tap-hover-opacity'"
						:hover-stay-time="70"
						@click.stop="prevPromoItem"
					>
						<text class="activity-nav-arrow">&lt;</text>
					</view>
					<text class="activity-nav-indicator">{{ promoNavLabel }}</text>
					<view
						class="activity-nav-btn"
						:class="{ 'activity-nav-btn--disabled': promoItems.length <= 1 }"
						:hover-class="promoItems.length <= 1 ? 'none' : 'tap-hover-opacity'"
						:hover-stay-time="70"
						@click.stop="nextPromoItem"
					>
						<text class="activity-nav-arrow">&gt;</text>
					</view>
				</view>
				<view class="activity-modal-body">
					<text v-if="currentPromoItem.type === 'activity'" class="activity-name">{{ currentPromoTitle }}</text>
					<view v-if="currentPromoItem.type === 'activity'">
						<view class="activity-meta-row">
							<text class="activity-meta-label">活动时间</text>
							<text class="activity-meta-value">{{ formatActivityTimeRange(currentPromoItem.data) }}</text>
						</view>
						<view class="activity-meta-row">
							<text class="activity-meta-label">活动描述</text>
							<text class="activity-meta-value">{{ currentPromoDescription || '暂无描述' }}</text>
						</view>
						<view class="activity-rules-block">
							<text class="activity-rules-title">活动规则</text>
							<view v-if="!currentPromoRules.length" class="activity-rule-empty">
								<text class="activity-rule-empty-text">暂无具体规则</text>
							</view>
							<view
								v-for="(rule, idx) in currentPromoRules"
								:key="rule.ruleId || idx"
								class="activity-rule-item"
							>
								<text class="activity-rule-text">{{ formatActivityRule(rule, currentPromoItem.data.activityType) }}</text>
							</view>
						</view>
					</view>
					<view v-else>
						<view v-if="currentPromoItem.data?.startTime || currentPromoItem.data?.endTime" class="activity-meta-row">
							<text class="activity-meta-label">公告时间</text>
							<text class="activity-meta-value">{{ formatActivityTimeRange(currentPromoItem.data) }}</text>
						</view>
						<view class="activity-rules-block activity-rules-block--announcement">
							<text class="activity-rules-title">公告内容</text>
							<view class="activity-rule-item">
								<text class="activity-rule-text">{{ currentPromoDescription || '暂无公告内容' }}</text>
							</view>
						</view>
					</view>
				</view>
				<view class="modal-actions">
					<view class="modal-close" @click="closeActivityModal">
						<text>我知道了</text>
					</view>
				</view>
			</view>
		</view>

		<view v-if="couponModalVisible && availableCoupons.length" class="modal-mask coupon-modal-mask" @click="closeCouponModal">
			<view class="modal-panel coupon-modal-panel" :class="isDark ? 'modal-panel--dark' : ''" @click.stop>
				<text class="modal-title">可领取优惠券</text>
				<scroll-view class="coupon-modal-list" scroll-y>
					<view
						v-for="coupon in availableCoupons"
						:key="coupon.templateId"
						class="coupon-modal-item"
					>
						<image class="coupon-modal-icon" :src="couponIconSrc(coupon.type)" mode="aspectFit" />
						<view class="coupon-modal-info">
							<text class="coupon-modal-name">{{ coupon.couponName || '店铺优惠券' }}</text>
							<text class="coupon-modal-desc">{{ formatCouponDesc(coupon) }}</text>
							<text class="coupon-modal-stock">剩余 {{ formatCouponRemaining(coupon) }} 张</text>
						</view>
						<view
							class="coupon-modal-claim"
							:class="{ 'coupon-modal-claim--disabled': couponClaiming }"
							@click.stop="claimCoupon(coupon)"
						>
							<text class="coupon-modal-claim-text">领取</text>
						</view>
					</view>
				</scroll-view>
				<view class="coupon-modal-actions">
					<view
						class="coupon-modal-claim-all"
						:class="{ 'coupon-modal-claim-all--disabled': couponClaiming }"
						@click="claimAllCoupons"
					>
						<text class="coupon-modal-claim-all-text">{{ couponClaiming ? '领取中…' : '一键领取' }}</text>
					</view>
					<view class="modal-close coupon-modal-close" @click="closeCouponModal">
						<text>关闭</text>
					</view>
				</view>
			</view>
		</view>

	</view>
</template>

<script setup>
import ShopImageSwiper from '@/components/shop/shop-image-swiper.vue'
import ProductCatalog from '@/components/shop/product-catalog.vue'
import MemberShopCartBar from '@/components/member/member-shop-cart-bar.vue'
import {
	couponIconSrc,
	formatCouponDesc,
	formatCouponRemaining
} from '@/utils/coupon-display.js'
import { computed } from 'vue'
import { useMemberShopDetail } from '@/composables/use-member-shop-detail.js'
import { productCatalogFlexViewportStyle } from '@/utils/product-catalog-layout.js'

const catalogViewportStyle = computed(() => productCatalogFlexViewportStyle(props.embedded ? 240 : 320))

const props = defineProps({
	shopId: { type: [String, Number], required: true },
	isDark: { type: Boolean, default: false },
	/** 嵌入主屏会员 Tab，保留底部导航 */
	embedded: { type: Boolean, default: false }
})

const {
	detail,
	loading,
	errorMsg,
	products,
	productCategories,
	productsLoading,
	carouselImages,
	shopAddress,
	shopPhone,
	remainingPoints,
	canPayQrcode,
	showManageLink,
	showScanVerifyBtn,
	callShopPhone,
	goManage,
	goPayQrcode,
	scanForVerify,
	applyShop,
	reload,
	syncCartFromCache,
	openProductDetail,
	formatActivityTimeRange,
	formatActivityRule,
	activityModalVisible,
	activityIndex,
	couponModalVisible,
	couponClaiming,
	promoItems,
	currentPromoItem,
	currentPromoTitle,
	currentPromoDescription,
	currentPromoRules,
	promoNavLabel,
	availableCoupons,
	closeActivityModal,
	closeCouponModal,
	prevPromoItem,
	nextPromoItem,
	claimCoupon,
	claimAllCoupons,
	cart,
	cartLines,
	cartTotalPointsText,
	cartItemCount,
	checkingOut,
	onCartQtyChange,
	onCartQtyUpdate,
	submitPurchase
} = useMemberShopDetail(props)

defineExpose({ applyShop, reload, syncCartFromCache })

</script>

<style scoped>
.detail-root {
	display: flex;
	flex-direction: column;
	box-sizing: border-box;
}

.detail-root--page {
	min-height: 100vh;
}

.detail-root--embedded {
	position: relative;
	flex: 1;
	min-height: 0;
	height: 100%;
	width: 100%;
}

.detail-root--light {
	background-color: #f2f4f8;
	color: #1a1a2e;
}

.detail-root--dark {
	background-color: #0f0f14;
	color: #ececf0;
}

.hero-wrap {
	position: relative;
	flex-shrink: 0;
}

.hero-fade {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	height: 72px;
	pointer-events: none;
	z-index: 1;
}

.detail-root--light .hero-fade {
	background: linear-gradient(to bottom, rgba(242, 244, 248, 0), rgba(242, 244, 248, 0.92));
}

.detail-root--dark .hero-fade {
	background: linear-gradient(to bottom, rgba(15, 15, 20, 0), rgba(15, 15, 20, 0.92));
}

.shop-info-card {
	position: relative;
	z-index: 2;
	flex-shrink: 0;
	margin: -36px 16px 0;
	padding: 18px 16px 16px;
	border-radius: 16px;
	box-sizing: border-box;
}

.detail-root--light .shop-info-card {
	background: #fff;
	box-shadow: 0 8px 32px rgba(15, 23, 42, 0.08), 0 2px 8px rgba(15, 23, 42, 0.04);
}

.detail-root--dark .shop-info-card {
	background: #1c1c24;
	box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35), 0 2px 8px rgba(0, 0, 0, 0.2);
}

.shop-title-bar {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
}

.shop-title {
	flex: 1;
	min-width: 0;
	font-size: 22px;
	font-weight: 700;
	line-height: 1.3;
	letter-spacing: 0.3px;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.shop-title-actions {
	flex-shrink: 0;
	display: flex;
	align-items: center;
	gap: 10px;
}

.shop-action-link {
	font-size: 13px;
	color: #007aff;
}

.detail-root--dark .shop-action-link {
	color: #5ac8fa;
}

.ratio-row {
	display: flex;
	flex-direction: column;
	align-items: flex-start;
	gap: 6px;
	margin-top: 8px;
}

.ratio-chip {
	display: inline-flex;
	padding: 3px 10px;
	border-radius: 20px;
}

.detail-root--light .ratio-chip {
	background: rgba(59, 130, 246, 0.08);
}

.detail-root--dark .ratio-chip {
	background: rgba(96, 165, 250, 0.12);
}

.ratio-text {
	font-size: 12px;
	font-weight: 500;
	opacity: 0.7;
}

.detail-root--light .ratio-text {
	color: #3b82f6;
	opacity: 1;
}

.detail-root--dark .ratio-text {
	color: #93c5fd;
	opacity: 1;
}

.ratio-note {
	font-size: 11px;
	line-height: 1.45;
}

.detail-root--light .ratio-note {
	color: #999999;
}

.detail-root--dark .ratio-note {
	color: #888888;
}

.shop-meta-row {
	display: flex;
	flex-direction: column;
	align-items: flex-start;
	gap: 8px;
	margin-top: 10px;
	min-width: 0;
}

.shop-meta-phone {
	display: flex;
	align-items: center;
	gap: 4px;
	flex-shrink: 0;
}

.shop-meta-address {
	display: flex;
	align-items: flex-start;
	gap: 4px;
	width: 100%;
	min-width: 0;
}

.shop-meta-icon {
	font-size: 13px;
	line-height: 1.45;
	flex-shrink: 0;
}

.shop-meta-phone-text {
	font-size: 13px;
	line-height: 1.45;
	white-space: nowrap;
}

.detail-root--light .shop-meta-phone-text {
	color: #007aff;
}

.detail-root--dark .shop-meta-phone-text {
	color: #5ac8fa;
}

.shop-meta-address-text {
	flex: 1;
	min-width: 0;
	font-size: 13px;
	line-height: 1.45;
	opacity: 0.72;
	white-space: normal;
	word-break: break-all;
}

.points-summary {
	margin-top: 12px;
	padding: 8px 12px;
	border-radius: 10px;
	display: flex;
	align-items: center;
	justify-content: space-between;
	min-height: 0;
}

.detail-root--light .points-summary {
	background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 50%, #fde68a 100%);
	border: 1px solid rgba(217, 169, 56, 0.2);
}

.detail-root--dark .points-summary {
	background: linear-gradient(135deg, #2a2418 0%, #3d3420 50%, #4a3f28 100%);
	border: 1px solid rgba(224, 184, 74, 0.25);
}

.points-summary-left {
	display: flex;
	align-items: center;
	gap: 6px;
}

.points-summary-icon {
	font-size: 12px;
	color: #b8860b;
	opacity: 0.85;
}

.detail-root--dark .points-summary-icon {
	color: #e0b84a;
}

.points-summary-label {
	font-size: 13px;
	font-weight: 500;
	opacity: 0.75;
	line-height: 1.2;
}

.pay-qrcode-btn {
	margin-top: 14px;
	padding: 13px 0;
	border-radius: 12px;
	text-align: center;
	background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
	box-shadow: 0 4px 16px rgba(37, 99, 235, 0.35);
}

.pay-qrcode-text {
	color: #fff;
	font-size: 15px;
	font-weight: 600;
	letter-spacing: 0.5px;
}

.points-gold {
	display: flex;
	align-items: baseline;
	flex-shrink: 0;
}

.points-gold-num {
	font-size: 22px;
	font-weight: 800;
	line-height: 1;
	color: #b8860b;
}

.points-gold--product .points-gold-num {
	font-size: 16px;
	font-weight: 700;
}

.points-gold-unit {
	margin-left: 3px;
	font-size: 11px;
	font-weight: 600;
	line-height: 1;
	color: #b8860b;
	opacity: 0.85;
}

.detail-root--dark .points-gold-num,
.detail-root--dark .points-gold-unit {
	color: #f0c850;
}

.list-scroll {
	flex: 1;
	min-height: 0;
	padding: 16px 16px 12px;
	box-sizing: border-box;
	overflow: hidden;
	display: flex;
	flex-direction: column;
}

.catalog-panel-body {
	flex: 1;
	min-height: 0;
	display: flex;
	flex-direction: column;
	overflow: hidden;
	box-sizing: border-box;
}

.catalog-panel-catalog {
	flex: 1;
	min-height: 0;
	height: 100%;
	width: 100%;
	display: flex;
}

.detail-root--with-cart .list-scroll {
	padding-bottom: 88px;
}

.coupon-modal-mask {
	z-index: 111;
}

.coupon-modal-panel {
	max-width: 360px;
	max-height: 72vh;
	display: flex;
	flex-direction: column;
}

.coupon-modal-list {
	flex: 1;
	min-height: 0;
	max-height: 46vh;
	margin-bottom: 12px;
}

.coupon-modal-item {
	display: flex;
	align-items: center;
	gap: 10px;
	padding: 10px 0;
	border-bottom: 1px solid rgba(128, 128, 128, 0.15);
}

.coupon-modal-item:last-child {
	border-bottom: none;
}

.coupon-modal-icon {
	width: 44px;
	height: 44px;
	flex-shrink: 0;
}

.coupon-modal-info {
	flex: 1;
	min-width: 0;
}

.coupon-modal-name {
	display: block;
	font-size: 15px;
	font-weight: 600;
	line-height: 1.35;
	margin-bottom: 2px;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.coupon-modal-desc {
	display: block;
	font-size: 12px;
	opacity: 0.65;
	line-height: 1.35;
}

.coupon-modal-stock {
	display: block;
	margin-top: 2px;
	font-size: 11px;
	opacity: 0.5;
}

.coupon-modal-claim {
	flex-shrink: 0;
	padding: 6px 12px;
	border-radius: 16px;
	background: linear-gradient(135deg, #ff8a3d, #ff6b00);
}

.coupon-modal-claim--disabled {
	opacity: 0.65;
	pointer-events: none;
}

.coupon-modal-claim-text {
	font-size: 13px;
	font-weight: 600;
	color: #fff;
	white-space: nowrap;
}

.coupon-modal-actions {
	display: flex;
	flex-direction: column;
	gap: 8px;
}

.coupon-modal-claim-all {
	text-align: center;
	padding: 10px;
	border-radius: 8px;
	background: linear-gradient(135deg, #ff8a3d, #ff6b00);
}

.coupon-modal-claim-all--disabled {
	opacity: 0.65;
	pointer-events: none;
}

.coupon-modal-claim-all-text {
	font-size: 14px;
	font-weight: 600;
	color: #fff;
}

.coupon-modal-close {
	margin-top: 0;
}

.activity-rules-block--announcement {
	border-top: none;
	padding-top: 0;
	margin-top: 0;
}

.state-wrap {
	padding: 48px 16px;
	text-align: center;
}

.state-text {
	font-size: 14px;
}

.detail-root--light .state-text {
	color: #666666;
}

.detail-root--dark .state-text {
	color: #a0a0a0;
}

.modal-mask {
	position: fixed;
	inset: 0;
	background: rgba(0, 0, 0, 0.45);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 100;
	padding: 24px;
	box-sizing: border-box;
}

.activity-modal-mask {
	z-index: 110;
}

.activity-modal-panel {
	max-height: 72vh;
	display: flex;
	flex-direction: column;
}

.activity-nav {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 16px;
	margin-bottom: 12px;
}

.activity-nav-btn {
	width: 36px;
	height: 36px;
	border-radius: 18px;
	display: flex;
	align-items: center;
	justify-content: center;
}

.detail-root--light .activity-nav-btn {
	background: #f0f2f5;
}

.detail-root--dark .activity-nav-btn {
	background: #2a2a2a;
}

.activity-nav-btn--disabled {
	opacity: 0.35;
	pointer-events: none;
}

.activity-nav-arrow {
	font-size: 22px;
	line-height: 1;
	font-weight: 600;
}

.activity-nav-indicator {
	font-size: 13px;
	min-width: 48px;
	text-align: center;
}

.detail-root--light .activity-nav-indicator {
	color: #666666;
}

.detail-root--dark .activity-nav-indicator {
	color: #a0a0a0;
}

.activity-modal-body {
	flex: 1;
	min-height: 0;
	max-height: 50vh;
	overflow-y: auto;
}

.activity-name {
	font-size: 16px;
	font-weight: 600;
	line-height: 1.4;
	display: block;
	margin-bottom: 12px;
}

.activity-meta-row {
	display: flex;
	flex-direction: column;
	gap: 4px;
	margin-bottom: 10px;
}

.activity-meta-label {
	font-size: 12px;
	opacity: 0.55;
}

.activity-meta-value {
	font-size: 13px;
	line-height: 1.45;
}

.activity-rules-block {
	margin-top: 4px;
	padding-top: 10px;
	border-top: 1px solid rgba(128, 128, 128, 0.2);
}

.activity-rules-title {
	font-size: 13px;
	font-weight: 600;
	display: block;
	margin-bottom: 8px;
}

.activity-rule-item {
	padding: 8px 10px;
	border-radius: 8px;
	margin-bottom: 8px;
}

.detail-root--light .activity-rule-item {
	background: #f5f7fa;
}

.detail-root--dark .activity-rule-item {
	background: #2a2a2a;
}

.activity-rule-text {
	font-size: 13px;
	line-height: 1.45;
}

.activity-rule-empty {
	padding: 12px 0;
	text-align: center;
}

.activity-rule-empty-text {
	font-size: 13px;
	opacity: 0.5;
}

.modal-panel {
	width: 100%;
	max-width: 340px;
	background: #fff;
	border-radius: 12px;
	padding: 16px;
}

.modal-panel--dark {
	background: #1e1e1e;
	color: #e8e8e8;
}

.modal-title {
	font-size: 17px;
	font-weight: 600;
	margin-bottom: 12px;
	display: block;
}

.modal-title--center {
	text-align: center;
}

.modal-actions {
	margin-top: 16px;
}

.modal-close {
	text-align: center;
	padding: 10px;
	border-radius: 8px;
	font-size: 14px;
}

.detail-root--light .modal-close {
	background: #f5f5f5;
	color: #333;
}

.detail-root--dark .modal-close {
	background: #2a2a2a;
	color: #e8e8e8;
}

.detail-root--with-cart .list-scroll {
	padding-bottom: 88px;
}
</style>
