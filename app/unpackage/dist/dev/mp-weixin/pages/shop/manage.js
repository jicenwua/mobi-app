"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_shop = require("../../api/modules/shop.js");
const utils_shopRole = require("../../utils/shop-role.js");
const utils_wxPerm = require("../../utils/wx-perm.js");
const utils_navigation = require("../../utils/navigation.js");
const utils_shopPageContext = require("../../utils/shop-page-context.js");
const composables_useShopManageProducts = require("../../composables/use-shop-manage-products.js");
const composables_useShopManageCoupons = require("../../composables/use-shop-manage-coupons.js");
const composables_useShopManageActivities = require("../../composables/use-shop-manage-activities.js");
const composables_useShopManageCheckout = require("../../composables/use-shop-manage-checkout.js");
const utils_productCatalogLayout = require("../../utils/product-catalog-layout.js");
if (!Math) {
  (ShopImageSwiper + SegmentedTabs + ShopManageActivityList + ShopManageCouponList + ProductCatalog + ShopManageProductForm + CategoryManageSheet + ShopManageCouponForm + ShopManageCouponStockSheet)();
}
const ShopImageSwiper = () => "../../components/shop/shop-image-swiper.js";
const ProductCatalog = () => "../../components/shop/product-catalog.js";
const CategoryManageSheet = () => "../../components/shop/category-manage-sheet.js";
const ShopManageActivityList = () => "../../components/shop/shop-manage-activity-list.js";
const ShopManageCouponList = () => "../../components/shop/shop-manage-coupon-list.js";
const ShopManageProductForm = () => "../../components/shop/shop-manage-product-form.js";
const ShopManageCouponForm = () => "../../components/shop/shop-manage-coupon-form.js";
const ShopManageCouponStockSheet = () => "../../components/shop/shop-manage-coupon-stock-sheet.js";
const SegmentedTabs = () => "../../components/common/segmented-tabs.js";
const _sfc_main = {
  __name: "manage",
  setup(__props) {
    const catalogViewportStyle = utils_productCatalogLayout.productCatalogViewportStyle();
    const shopId = common_vendor.ref("");
    const shopInfo = common_vendor.ref(null);
    const loading = common_vendor.ref(true);
    const activeTab = common_vendor.ref("product");
    const listLoading = common_vendor.ref(false);
    const isManager = common_vendor.computed(() => utils_shopRole.isShopManager(shopInfo.value));
    const canScanVerify = common_vendor.computed(() => utils_wxPerm.canStaffVerifyAtShop(shopInfo.value));
    const roleLabel = common_vendor.computed(() => utils_shopRole.getShopRoleLabel(shopInfo.value));
    const canAddCurrentTab = common_vendor.computed(() => isManager.value);
    const tabs = [
      { key: "product", label: "商品" },
      { key: "activity", label: "活动" },
      { key: "coupon", label: "折扣券" }
    ];
    const productsApi = composables_useShopManageProducts.useShopManageProducts(shopId, { shopInfo, listLoading });
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
    } = productsApi;
    const couponsApi = composables_useShopManageCoupons.useShopManageCoupons(shopId, { isManager, listLoading });
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
    } = couponsApi;
    const {
      activities,
      needReloadActivities,
      loadActivities,
      goActivityEdit,
      editActivity,
      deleteActivity,
      stopActivity,
      enableActivity
    } = composables_useShopManageActivities.useShopManageActivities(shopId, { isManager, listLoading, shopInfo });
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
    } = composables_useShopManageCheckout.useShopManageCheckout({
      shopId,
      canScanVerify,
      productCategories,
      loadProducts,
      setActiveTab: (tab) => {
        activeTab.value = tab;
      }
    });
    const pageScrollLocked = common_vendor.computed(
      () => productFormVisible.value || categoryFormVisible.value || couponFormVisible.value || couponStockVisible.value
    );
    const sectionTitle = common_vendor.computed(() => {
      if (activeTab.value === "product")
        return "商品列表";
      if (activeTab.value === "activity")
        return "活动列表";
      return "折扣券列表";
    });
    const shopCarouselImages = common_vendor.computed(() => api_modules_shop.getShopCarouselImages(shopInfo.value));
    const shopAddress = common_vendor.computed(() => api_modules_shop.formatShopAddress(shopInfo.value));
    const currentList = common_vendor.computed(() => {
      if (activeTab.value === "product")
        return currentProductList.value;
      if (activeTab.value === "activity")
        return activities.value;
      return coupons.value;
    });
    common_vendor.onLoad((options) => {
      if (!utils_wxPerm.canManageShop()) {
        common_vendor.index.showToast({ title: "无店铺管理权限", icon: "none" });
        utils_navigation.navigateBackDelayed(800);
        return;
      }
      shopId.value = (options == null ? void 0 : options.id) ? String(options.id) : "";
      const pendingEditProductId = (options == null ? void 0 : options.editProduct) ? String(options.editProduct) : "";
      const cachedShop = shopId.value ? utils_shopPageContext.peekShopDetail(shopId.value) : null;
      if (cachedShop && api_modules_shop.isUsableShopDetail(cachedShop)) {
        shopInfo.value = cachedShop;
        loading.value = false;
      }
      utils_shopPageContext.bindOpenerShop((data) => {
        if (!utils_shopRole.isShopClerkCapable(data)) {
          common_vendor.index.showToast({ title: "无店铺管理权限", icon: "none" });
          utils_navigation.navigateBackDelayed(800);
          return;
        }
        if (api_modules_shop.isUsableShopDetail(data)) {
          shopInfo.value = data;
        }
      });
      if (!shopId.value) {
        loading.value = false;
        common_vendor.index.showToast({ title: "缺少店铺 ID", icon: "none" });
        return;
      }
      const presetCheckoutToken = (options == null ? void 0 : options.checkoutToken) ? decodeURIComponent(String(options.checkoutToken)) : "";
      void initManagePage(pendingEditProductId, presetCheckoutToken);
    });
    async function initManagePage(pendingEditProductId = "", presetCheckoutToken = "") {
      const hasShop = api_modules_shop.isUsableShopDetail(shopInfo.value);
      if (!hasShop)
        loading.value = true;
      await loadShopInfo();
      loading.value = false;
      await loadCategories();
      await loadProducts();
      if (pendingEditProductId) {
        const item = products.value.find((p) => String(p.productId) === pendingEditProductId);
        if (item)
          editProduct(item);
      }
      if (presetCheckoutToken && canScanVerify.value) {
        await applyCheckoutToken(presetCheckoutToken);
      }
    }
    async function loadShopInfo() {
      if (!shopId.value || shopInfo.value)
        return;
      const res = await api_modules_shop.fetchShopFromUserList(shopId.value);
      if (res.ok && res.data) {
        shopInfo.value = utils_shopPageContext.rememberShopDetail(res.data) || res.data;
      }
    }
    common_vendor.onShow(() => {
      if (needReloadActivities.value && shopId.value) {
        needReloadActivities.value = false;
        loadActivities();
      }
    });
    common_vendor.watch(activeTab, () => {
      if (activeTab.value === "product" && !productCategories.value.length)
        loadProducts();
      if (activeTab.value === "activity" && !activities.value.length)
        loadActivities();
      if (activeTab.value === "coupon" && !coupons.value.length)
        loadCoupons();
    });
    function goCustomers() {
      utils_shopPageContext.navigateWithShop({
        url: `/pages/shop/records?shopId=${shopId.value}`,
        shop: shopInfo.value
      });
    }
    function goStatistics() {
      utils_shopPageContext.navigateWithShop({
        url: `/pages/shop/statistics?shopId=${shopId.value}`,
        shop: shopInfo.value
      });
    }
    function goStaff() {
      if (!isManager.value)
        return;
      utils_shopPageContext.navigateWithShop({
        url: `/pages/shop/staff?shopId=${shopId.value}`,
        shop: shopInfo.value
      });
    }
    function onProductCatalogClick(product) {
      if (shouldBlockProductClick())
        return;
      openProductDetail(product);
    }
    function onAdd() {
      if (activeTab.value === "product") {
        promptAddProduct();
      } else if (activeTab.value === "coupon") {
        promptAddCoupon();
      } else if (isManager.value) {
        goActivityEdit();
      }
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: pageScrollLocked.value ? "overflow:hidden;height:100vh;" : "",
        b: loading.value
      }, loading.value ? {} : common_vendor.e({
        c: shopInfo.value
      }, shopInfo.value ? common_vendor.e({
        d: common_vendor.p({
          images: shopCarouselImages.value,
          height: "160px"
        }),
        e: common_vendor.t(shopInfo.value.shopName || "未命名店铺"),
        f: roleLabel.value
      }, roleLabel.value ? {
        g: common_vendor.t(roleLabel.value)
      } : {}, {
        h: canScanVerify.value
      }, canScanVerify.value ? {
        i: common_vendor.o((...args) => common_vendor.unref(scanForVerify) && common_vendor.unref(scanForVerify)(...args), "db")
      } : {}, {
        j: isManager.value
      }, isManager.value ? {
        k: common_vendor.o(goStaff, "6b")
      } : {}, {
        l: common_vendor.o(goCustomers, "21"),
        m: common_vendor.o(goStatistics, "33"),
        n: shopInfo.value.ratio != null
      }, shopInfo.value.ratio != null ? {
        o: common_vendor.t(shopInfo.value.ratio)
      } : {}, {
        p: shopAddress.value
      }, shopAddress.value ? {
        q: common_vendor.t(shopAddress.value)
      } : {}) : {}, {
        r: common_vendor.o(($event) => activeTab.value = $event, "47"),
        s: common_vendor.p({
          tabs,
          modelValue: activeTab.value
        }),
        t: activeTab.value !== "product"
      }, activeTab.value !== "product" ? common_vendor.e({
        v: common_vendor.t(sectionTitle.value),
        w: canAddCurrentTab.value
      }, canAddCurrentTab.value ? {
        x: common_vendor.o(onAdd, "e8")
      } : {}, {
        y: activeTab.value === "activity"
      }, activeTab.value === "activity" ? {
        z: common_vendor.o(common_vendor.unref(editActivity), "a7"),
        A: common_vendor.o(common_vendor.unref(deleteActivity), "45"),
        B: common_vendor.o(common_vendor.unref(stopActivity), "33"),
        C: common_vendor.o(common_vendor.unref(enableActivity), "74"),
        D: common_vendor.o(onAdd, "e9"),
        E: common_vendor.p({
          activities: common_vendor.unref(activities),
          ["list-loading"]: listLoading.value,
          ["is-manager"]: isManager.value
        })
      } : activeTab.value === "coupon" ? {
        G: common_vendor.o(common_vendor.unref(editCoupon), "01"),
        H: common_vendor.o(common_vendor.unref(resumeCouponDistribution), "8f"),
        I: common_vendor.o(common_vendor.unref(promptAddCouponStock), "a7"),
        J: common_vendor.o(common_vendor.unref(deleteCoupon), "cf"),
        K: common_vendor.p({
          coupons: common_vendor.unref(coupons),
          ["list-loading"]: listLoading.value,
          ["is-manager"]: isManager.value
        })
      } : {}, {
        F: activeTab.value === "coupon"
      }) : common_vendor.e({
        L: common_vendor.unref(checkoutMember)
      }, common_vendor.unref(checkoutMember) ? common_vendor.e({
        M: common_vendor.t(common_vendor.unref(checkoutMember).nickname || "会员"),
        N: common_vendor.t(common_vendor.unref(checkoutMember).phone || ""),
        O: common_vendor.unref(checkoutMember).phone
      }, common_vendor.unref(checkoutMember).phone ? {} : {}, {
        P: common_vendor.t(common_vendor.unref(checkoutMember).remainingPoints ?? 0),
        Q: common_vendor.o((...args) => common_vendor.unref(exitCheckout) && common_vendor.unref(exitCheckout)(...args), "2d")
      }) : canScanVerify.value ? {} : {}, {
        R: canScanVerify.value,
        S: common_vendor.t(canScanVerify.value ? "选购商品" : sectionTitle.value),
        T: canAddCurrentTab.value && !(canScanVerify.value && (common_vendor.unref(checkoutCartCount) > 0 || common_vendor.unref(checkoutMember)))
      }, canAddCurrentTab.value && !(canScanVerify.value && (common_vendor.unref(checkoutCartCount) > 0 || common_vendor.unref(checkoutMember))) ? {
        U: common_vendor.o(onAdd, "bc")
      } : {}, {
        V: listLoading.value
      }, listLoading.value ? {} : !currentList.value.length ? {} : {
        X: common_vendor.o(onProductCatalogClick, "94"),
        Y: common_vendor.o(common_vendor.unref(onCheckoutQtyChange), "06"),
        Z: common_vendor.p({
          categories: common_vendor.unref(productCategories),
          purchasable: canScanVerify.value,
          ["show-stock"]: true,
          cart: common_vendor.unref(checkoutCart),
          ["hide-off-shelf"]: canScanVerify.value
        }),
        aa: common_vendor.s(common_vendor.unref(catalogViewportStyle))
      }, {
        W: !currentList.value.length,
        ab: canScanVerify.value && common_vendor.unref(checkoutCartCount) > 0 ? 1 : ""
      }), {
        ac: canScanVerify.value && common_vendor.unref(checkoutCartCount) > 0
      }, canScanVerify.value && common_vendor.unref(checkoutCartCount) > 0 ? {
        ad: common_vendor.t(common_vendor.unref(checkoutTotalPoints)),
        ae: common_vendor.t(common_vendor.unref(checkoutSubmitting) ? "扣款中…" : common_vendor.unref(checkoutPayToken) ? "确认扣款" : "扫码扣款"),
        af: common_vendor.unref(checkoutSubmitting) ? 1 : "",
        ag: common_vendor.o((...args) => common_vendor.unref(submitCheckoutConsume) && common_vendor.unref(submitCheckoutConsume)(...args), "81")
      } : {}), {
        ah: common_vendor.o(common_vendor.unref(closeProductForm), "a2"),
        ai: common_vendor.o(common_vendor.unref(submitProductForm), "b0"),
        aj: common_vendor.o(common_vendor.unref(onProductCategoryChange), "9c"),
        ak: common_vendor.o(common_vendor.unref(pickProductImage), "64"),
        al: common_vendor.o(common_vendor.unref(clearProductImage), "ef"),
        am: common_vendor.o(common_vendor.unref(previewProductImage), "52"),
        an: common_vendor.o(common_vendor.unref(openCategoryForm), "17"),
        ao: common_vendor.p({
          visible: common_vendor.unref(productFormVisible),
          ["editing-product-id"]: common_vendor.unref(editingProductId),
          form: common_vendor.unref(productForm),
          ["category-labels"]: common_vendor.unref(categoryLabels),
          ["has-categories"]: common_vendor.unref(categories).length > 0
        }),
        ap: common_vendor.o(common_vendor.unref(onCategoriesSaved), "3b"),
        aq: common_vendor.o(($event) => common_vendor.isRef(categoryFormVisible) ? categoryFormVisible.value = $event : null, "f2"),
        ar: common_vendor.p({
          ["shop-id"]: shopId.value,
          categories: common_vendor.unref(categories),
          visible: common_vendor.unref(categoryFormVisible)
        }),
        as: common_vendor.o(common_vendor.unref(closeCouponForm), "a4"),
        at: common_vendor.o(common_vendor.unref(submitCouponForm), "32"),
        av: common_vendor.o(common_vendor.unref(onCouponTypeChange), "94"),
        aw: common_vendor.o(common_vendor.unref(onNeverExpireChange), "76"),
        ax: common_vendor.o(common_vendor.unref(stopCouponDistribution), "2a"),
        ay: common_vendor.o(common_vendor.unref(resumeCouponDistributionFromForm), "93"),
        az: common_vendor.p({
          visible: common_vendor.unref(couponFormVisible),
          ["editing-coupon-id"]: common_vendor.unref(editingCouponId),
          ["editing-coupon-item"]: common_vendor.unref(editingCouponItem),
          form: common_vendor.unref(couponForm),
          ["coupon-type-labels"]: common_vendor.unref(couponTypeLabels),
          ["coupon-type-index"]: common_vendor.unref(couponTypeIndex)
        }),
        aA: common_vendor.o(common_vendor.unref(closeCouponStockForm), "0d"),
        aB: common_vendor.o(common_vendor.unref(submitCouponStockForm), "86"),
        aC: common_vendor.o(($event) => common_vendor.unref(couponStockForm).quantity = $event, "a9"),
        aD: common_vendor.p({
          visible: common_vendor.unref(couponStockVisible),
          target: common_vendor.unref(couponStockTarget),
          form: common_vendor.unref(couponStockForm)
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-ccf87464"]]);
wx.createPage(MiniProgramPage);
