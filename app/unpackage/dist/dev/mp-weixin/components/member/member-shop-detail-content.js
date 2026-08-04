"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_couponDisplay = require("../../utils/coupon-display.js");
const composables_useMemberShopDetail = require("../../composables/use-member-shop-detail.js");
const utils_productCatalogLayout = require("../../utils/product-catalog-layout.js");
if (!Math) {
  (ShopImageSwiper + ProductCatalog + MemberShopCartBar)();
}
const ShopImageSwiper = () => "../shop/shop-image-swiper.js";
const ProductCatalog = () => "../shop/product-catalog.js";
const MemberShopCartBar = () => "./member-shop-cart-bar.js";
const _sfc_main = {
  __name: "member-shop-detail-content",
  props: {
    shopId: { type: [String, Number], required: true },
    isDark: { type: Boolean, default: false },
    /** 嵌入主屏会员 Tab，保留底部导航 */
    embedded: { type: Boolean, default: false }
  },
  setup(__props, { expose: __expose }) {
    const catalogViewportStyle = utils_productCatalogLayout.productCatalogViewportStyle();
    const props = __props;
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
    } = composables_useMemberShopDetail.useMemberShopDetail(props);
    __expose({ applyShop, reload, syncCartFromCache });
    return (_ctx, _cache) => {
      var _a, _b, _c, _d;
      return common_vendor.e({
        a: common_vendor.unref(loading)
      }, common_vendor.unref(loading) ? {} : common_vendor.unref(detail) ? common_vendor.e({
        c: common_vendor.p({
          images: common_vendor.unref(carouselImages),
          ["is-dark"]: __props.isDark,
          height: "220px"
        }),
        d: common_vendor.t(common_vendor.unref(detail).shopName || "未命名店铺"),
        e: common_vendor.unref(showManageLink) || common_vendor.unref(showScanVerifyBtn)
      }, common_vendor.unref(showManageLink) || common_vendor.unref(showScanVerifyBtn) ? common_vendor.e({
        f: common_vendor.unref(showScanVerifyBtn)
      }, common_vendor.unref(showScanVerifyBtn) ? {
        g: common_vendor.o((...args) => common_vendor.unref(scanForVerify) && common_vendor.unref(scanForVerify)(...args), "15")
      } : {}, {
        h: common_vendor.unref(showManageLink)
      }, common_vendor.unref(showManageLink) ? {
        i: common_vendor.o((...args) => common_vendor.unref(goManage) && common_vendor.unref(goManage)(...args), "07")
      } : {}) : {}, {
        j: common_vendor.unref(detail).ratio != null
      }, common_vendor.unref(detail).ratio != null ? {
        k: common_vendor.t(common_vendor.unref(detail).ratio)
      } : {}, {
        l: common_vendor.unref(shopPhone) || common_vendor.unref(shopAddress)
      }, common_vendor.unref(shopPhone) || common_vendor.unref(shopAddress) ? common_vendor.e({
        m: common_vendor.unref(shopPhone)
      }, common_vendor.unref(shopPhone) ? {
        n: common_vendor.t(common_vendor.unref(shopPhone)),
        o: common_vendor.o((...args) => common_vendor.unref(callShopPhone) && common_vendor.unref(callShopPhone)(...args), "7c")
      } : {}, {
        p: common_vendor.unref(shopAddress)
      }, common_vendor.unref(shopAddress) ? {
        q: common_vendor.t(common_vendor.unref(shopAddress))
      } : {}) : {}, {
        r: common_vendor.t(common_vendor.unref(remainingPoints)),
        s: common_vendor.unref(canPayQrcode)
      }, common_vendor.unref(canPayQrcode) ? {
        t: common_vendor.o((...args) => common_vendor.unref(goPayQrcode) && common_vendor.unref(goPayQrcode)(...args), "ae")
      } : {}, {
        v: common_vendor.unref(productsLoading) && !common_vendor.unref(products).length
      }, common_vendor.unref(productsLoading) && !common_vendor.unref(products).length ? {} : !common_vendor.unref(products).length ? {} : {
        x: common_vendor.o(common_vendor.unref(openProductDetail), "78"),
        y: common_vendor.o(common_vendor.unref(onCartQtyChange), "dd"),
        z: common_vendor.p({
          categories: common_vendor.unref(productCategories),
          ["is-dark"]: __props.isDark,
          ["hide-off-shelf"]: true,
          purchasable: true,
          cart: common_vendor.unref(cart)
        }),
        A: common_vendor.s(common_vendor.unref(catalogViewportStyle))
      }, {
        w: !common_vendor.unref(products).length,
        B: common_vendor.o(common_vendor.unref(onCartQtyUpdate), "02"),
        C: common_vendor.o(common_vendor.unref(submitPurchase), "d6"),
        D: common_vendor.p({
          ["cart-lines"]: common_vendor.unref(cartLines),
          ["cart-total-points-text"]: common_vendor.unref(cartTotalPointsText),
          ["cart-item-count"]: common_vendor.unref(cartItemCount),
          ["is-dark"]: __props.isDark,
          ["checking-out"]: common_vendor.unref(checkingOut)
        })
      }) : {
        E: common_vendor.t(common_vendor.unref(errorMsg) || "店铺不存在")
      }, {
        b: common_vendor.unref(detail),
        F: common_vendor.unref(activityModalVisible) && common_vendor.unref(currentPromoItem)
      }, common_vendor.unref(activityModalVisible) && common_vendor.unref(currentPromoItem) ? common_vendor.e({
        G: common_vendor.t(common_vendor.unref(currentPromoItem).type === "announcement" ? "公告" : "活动"),
        H: common_vendor.unref(promoItems).length > 1
      }, common_vendor.unref(promoItems).length > 1 ? {
        I: common_vendor.unref(promoItems).length <= 1 ? 1 : "",
        J: common_vendor.unref(promoItems).length <= 1 ? "none" : "tap-hover-opacity",
        K: common_vendor.o((...args) => common_vendor.unref(prevPromoItem) && common_vendor.unref(prevPromoItem)(...args), "21"),
        L: common_vendor.t(common_vendor.unref(activityIndex) + 1),
        M: common_vendor.t(common_vendor.unref(promoItems).length),
        N: common_vendor.unref(promoItems).length <= 1 ? 1 : "",
        O: common_vendor.unref(promoItems).length <= 1 ? "none" : "tap-hover-opacity",
        P: common_vendor.o((...args) => common_vendor.unref(nextPromoItem) && common_vendor.unref(nextPromoItem)(...args), "87")
      } : {}, {
        Q: common_vendor.t(common_vendor.unref(currentPromoTitle)),
        R: common_vendor.unref(currentPromoItem).type === "activity"
      }, common_vendor.unref(currentPromoItem).type === "activity" ? common_vendor.e({
        S: common_vendor.t(common_vendor.unref(formatActivityTimeRange)(common_vendor.unref(currentPromoItem).data)),
        T: common_vendor.t(common_vendor.unref(currentPromoDescription) || "暂无描述"),
        U: !common_vendor.unref(currentPromoRules).length
      }, !common_vendor.unref(currentPromoRules).length ? {} : {}, {
        V: common_vendor.f(common_vendor.unref(currentPromoRules), (rule, idx, i0) => {
          return {
            a: common_vendor.t(common_vendor.unref(formatActivityRule)(rule, common_vendor.unref(currentPromoItem).data.activityType)),
            b: rule.ruleId || idx
          };
        })
      }) : common_vendor.e({
        W: ((_a = common_vendor.unref(currentPromoItem).data) == null ? void 0 : _a.startTime) || ((_b = common_vendor.unref(currentPromoItem).data) == null ? void 0 : _b.endTime)
      }, ((_c = common_vendor.unref(currentPromoItem).data) == null ? void 0 : _c.startTime) || ((_d = common_vendor.unref(currentPromoItem).data) == null ? void 0 : _d.endTime) ? {
        X: common_vendor.t(common_vendor.unref(formatActivityTimeRange)(common_vendor.unref(currentPromoItem).data))
      } : {}, {
        Y: common_vendor.t(common_vendor.unref(currentPromoDescription) || "暂无公告内容")
      }), {
        Z: common_vendor.o((...args) => common_vendor.unref(closeActivityModal) && common_vendor.unref(closeActivityModal)(...args), "2e"),
        aa: common_vendor.n(__props.isDark ? "modal-panel--dark" : ""),
        ab: common_vendor.o(() => {
        }, "e1"),
        ac: common_vendor.o((...args) => common_vendor.unref(closeActivityModal) && common_vendor.unref(closeActivityModal)(...args), "e7")
      }) : {}, {
        ad: common_vendor.unref(couponModalVisible) && common_vendor.unref(availableCoupons).length
      }, common_vendor.unref(couponModalVisible) && common_vendor.unref(availableCoupons).length ? {
        ae: common_vendor.f(common_vendor.unref(availableCoupons), (coupon, k0, i0) => {
          return {
            a: common_vendor.unref(utils_couponDisplay.couponIconSrc)(coupon.type),
            b: common_vendor.t(coupon.couponName || "店铺优惠券"),
            c: common_vendor.t(common_vendor.unref(utils_couponDisplay.formatCouponDesc)(coupon)),
            d: common_vendor.t(common_vendor.unref(utils_couponDisplay.formatCouponRemaining)(coupon)),
            e: common_vendor.o(($event) => common_vendor.unref(claimCoupon)(coupon), coupon.templateId),
            f: coupon.templateId
          };
        }),
        af: common_vendor.unref(couponClaiming) ? 1 : "",
        ag: common_vendor.t(common_vendor.unref(couponClaiming) ? "领取中…" : "一键领取"),
        ah: common_vendor.unref(couponClaiming) ? 1 : "",
        ai: common_vendor.o((...args) => common_vendor.unref(claimAllCoupons) && common_vendor.unref(claimAllCoupons)(...args), "12"),
        aj: common_vendor.o((...args) => common_vendor.unref(closeCouponModal) && common_vendor.unref(closeCouponModal)(...args), "c6"),
        ak: common_vendor.n(__props.isDark ? "modal-panel--dark" : ""),
        al: common_vendor.o(() => {
        }, "18"),
        am: common_vendor.o((...args) => common_vendor.unref(closeCouponModal) && common_vendor.unref(closeCouponModal)(...args), "b9")
      } : {}, {
        an: common_vendor.n(__props.isDark ? "detail-root--dark" : "detail-root--light"),
        ao: common_vendor.n(__props.embedded ? "detail-root--embedded" : "detail-root--page"),
        ap: common_vendor.n(common_vendor.unref(cartItemCount) > 0 ? "detail-root--with-cart" : "")
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1ce5a5d8"]]);
wx.createComponent(Component);
