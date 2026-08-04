"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_productStatus = require("../../utils/product-status.js");
if (!Array) {
  const _easycom_uni_icons2 = common_vendor.resolveComponent("uni-icons");
  _easycom_uni_icons2();
}
const _easycom_uni_icons = () => "../../uni_modules/uni-icons/components/uni-icons/uni-icons.js";
if (!Math) {
  _easycom_uni_icons();
}
const _sfc_main = {
  __name: "product-catalog",
  props: {
    categories: { type: Array, default: () => [] },
    isDark: { type: Boolean, default: false },
    /** 是否隐藏无商品的分类 */
    hideEmpty: { type: Boolean, default: true },
    /** 是否隐藏已下架商品（顾客端） */
    hideOffShelf: { type: Boolean, default: false },
    /** 是否展示库存（管理端） */
    showStock: { type: Boolean, default: false },
    /** 是否展示购买按钮与数量选择 */
    purchasable: { type: Boolean, default: false },
    /** 购物车数量映射 { [productId]: count } */
    cart: { type: Object, default: () => ({}) }
  },
  emits: ["product-click", "change-qty"],
  setup(__props, { emit: __emit }) {
    const props = __props;
    const emit = __emit;
    const instance = common_vendor.getCurrentInstance();
    const queryContext = common_vendor.computed(() => (instance == null ? void 0 : instance.proxy) || instance);
    const activeCategoryId = common_vendor.ref(null);
    const mainScrollTop = common_vendor.ref(0);
    const sidebarScrollIntoView = common_vendor.ref("");
    const scrollWithAnimation = common_vendor.ref(true);
    const sectionOffsets = common_vendor.ref({});
    const bottomSpacerHeight = common_vendor.ref(0);
    const mainScrollHeight = common_vendor.ref(0);
    const isProgrammaticScroll = common_vendor.ref(false);
    let programmaticScrollTimer = null;
    let measureToken = 0;
    const mainScrollStyle = common_vendor.computed(() => {
      if (mainScrollHeight.value > 0) {
        return { height: `${mainScrollHeight.value}px` };
      }
      return { height: "100%" };
    });
    const visibleCategories = common_vendor.computed(() => {
      const list = Array.isArray(props.categories) ? props.categories : [];
      const sorted = [...list].sort((a, b) => {
        const sa = (a == null ? void 0 : a.sortOrder) ?? 0;
        const sb = (b == null ? void 0 : b.sortOrder) ?? 0;
        if (sa !== sb)
          return sa - sb;
        return ((a == null ? void 0 : a.categoryId) ?? 0) - ((b == null ? void 0 : b.categoryId) ?? 0);
      });
      const mapped = sorted.map((cat) => {
        let products = Array.isArray(cat == null ? void 0 : cat.products) ? cat.products : [];
        if (props.hideOffShelf) {
          products = products.filter((p) => (p == null ? void 0 : p.status) !== utils_productStatus.PRODUCT_STATUS.OFF_SHELF);
        }
        products = utils_productStatus.sortProductsForDisplay(products);
        return { ...cat, products };
      });
      if (!props.hideEmpty)
        return mapped;
      return mapped.filter((cat) => cat.products.length > 0);
    });
    common_vendor.watch(
      visibleCategories,
      (list) => {
        if (!list.length) {
          activeCategoryId.value = null;
          sectionOffsets.value = {};
          bottomSpacerHeight.value = 0;
          return;
        }
        if (!list.some((c) => c.categoryId === activeCategoryId.value)) {
          activeCategoryId.value = list[0].categoryId;
        }
        scheduleMeasureLayout();
      },
      { immediate: true, deep: true }
    );
    common_vendor.onMounted(() => {
      scheduleMeasureLayout();
    });
    function scheduleMeasureLayout() {
      return measureLayoutWithRetry();
    }
    function measureLayoutWithRetry(delays = [0, 80, 180]) {
      const token = ++measureToken;
      const run = async () => {
        for (const delay of delays) {
          if (token !== measureToken)
            return false;
          if (delay > 0) {
            await new Promise((resolve) => setTimeout(resolve, delay));
          }
          await common_vendor.nextTick$1();
          const ok = await measureLayout();
          if (ok) {
            await common_vendor.nextTick$1();
            await new Promise((resolve) => setTimeout(resolve, 60));
            if (token !== measureToken)
              return false;
            await measureLayout();
            return true;
          }
        }
        return false;
      };
      return run();
    }
    function measureLayout() {
      return new Promise((resolve) => {
        const cats = visibleCategories.value;
        const context = queryContext.value;
        if (!cats.length || !context) {
          resolve(false);
          return;
        }
        const query = common_vendor.index.createSelectorQuery().in(context);
        query.select(".catalog").boundingClientRect();
        query.select(".catalog-main-inner").boundingClientRect();
        cats.forEach((cat) => {
          query.select(`#cat-${cat.categoryId}`).boundingClientRect();
        });
        query.exec((res) => {
          const catalogRect = res == null ? void 0 : res[0];
          const innerRect = res == null ? void 0 : res[1];
          if (!(catalogRect == null ? void 0 : catalogRect.height) || !innerRect) {
            resolve(false);
            return;
          }
          mainScrollHeight.value = Math.ceil(catalogRect.height);
          const offsets = {};
          cats.forEach((cat, index) => {
            const rect = res[index + 2];
            if (rect) {
              offsets[cat.categoryId] = Math.max(0, Math.round(rect.top - innerRect.top));
            }
          });
          sectionOffsets.value = offsets;
          const lastRect = res[cats.length + 1];
          const lastSectionHeight = (lastRect == null ? void 0 : lastRect.height) || 0;
          bottomSpacerHeight.value = Math.max(0, Math.ceil(catalogRect.height - lastSectionHeight));
          resolve(Object.keys(offsets).length > 0);
        });
      });
    }
    function setMainScrollTop(top, animated = true) {
      const nextTop = Math.max(0, Math.round(top));
      scrollWithAnimation.value = animated;
      isProgrammaticScroll.value = true;
      if (programmaticScrollTimer) {
        clearTimeout(programmaticScrollTimer);
      }
      if (mainScrollTop.value === nextTop) {
        mainScrollTop.value = nextTop + 1;
        common_vendor.nextTick$1(() => {
          mainScrollTop.value = nextTop;
        });
      } else {
        mainScrollTop.value = nextTop;
      }
      programmaticScrollTimer = setTimeout(() => {
        isProgrammaticScroll.value = false;
      }, animated ? 360 : 80);
    }
    async function jumpToCategory(cat) {
      if (!(cat == null ? void 0 : cat.categoryId) && (cat == null ? void 0 : cat.categoryId) !== 0)
        return;
      activeCategoryId.value = cat.categoryId;
      await measureLayoutWithRetry([0, 60, 140]);
      const offset = sectionOffsets.value[cat.categoryId] ?? 0;
      setMainScrollTop(offset);
      sidebarScrollIntoView.value = "";
      common_vendor.nextTick$1(() => {
        sidebarScrollIntoView.value = `sidebar-${cat.categoryId}`;
      });
    }
    function onMainScroll(e) {
      var _a;
      if (isProgrammaticScroll.value)
        return;
      const scrollTop = ((_a = e == null ? void 0 : e.detail) == null ? void 0 : _a.scrollTop) ?? 0;
      const cats = visibleCategories.value;
      if (!cats.length)
        return;
      let currentId = cats[0].categoryId;
      for (let i = cats.length - 1; i >= 0; i--) {
        const cat = cats[i];
        const offset = sectionOffsets.value[cat.categoryId];
        if (offset != null && scrollTop >= offset - 8) {
          currentId = cat.categoryId;
          break;
        }
      }
      if (currentId === activeCategoryId.value)
        return;
      activeCategoryId.value = currentId;
      sidebarScrollIntoView.value = "";
      common_vendor.nextTick$1(() => {
        sidebarScrollIntoView.value = `sidebar-${currentId}`;
      });
    }
    function onProductClick(product) {
      emit("product-click", product);
    }
    function statusMask(status) {
      return utils_productStatus.productStatusMask(status);
    }
    function formatStock(stock) {
      if (stock == null || stock === "")
        return "—";
      if (Number(stock) === -1)
        return "不限";
      return String(stock);
    }
    function formatSold(sold) {
      if (sold == null || sold === "")
        return "0";
      return String(sold);
    }
    function formatPoints(price) {
      if (price == null || price === "")
        return "—";
      const n = Number(price);
      if (Number.isNaN(n))
        return String(price);
      return Number.isInteger(n) ? String(n) : String(Math.round(n * 100) / 100);
    }
    function formatLinePoints(product) {
      const price = product == null ? void 0 : product.price;
      if (price == null || price === "")
        return "—";
      return formatPoints(price);
    }
    function cartQty(product) {
      var _a;
      const id = product == null ? void 0 : product.productId;
      if (id == null)
        return 0;
      return Number((_a = props.cart) == null ? void 0 : _a[id]) || 0;
    }
    function canPurchase(product) {
      const status = product == null ? void 0 : product.status;
      if (status === utils_productStatus.PRODUCT_STATUS.OFF_SHELF || status === utils_productStatus.PRODUCT_STATUS.SOLD_OUT)
        return false;
      const stock = product == null ? void 0 : product.stock;
      if (stock != null && stock !== "" && Number(stock) !== -1 && Number(stock) <= 0)
        return false;
      return true;
    }
    function canIncrease(product) {
      if (!canPurchase(product))
        return false;
      const stock = product == null ? void 0 : product.stock;
      if (stock == null || stock === "" || Number(stock) === -1)
        return true;
      return cartQty(product) < Number(stock);
    }
    function onBuy(product) {
      if (!canPurchase(product)) {
        common_vendor.index.showToast({ title: "该商品暂不可购买", icon: "none" });
        return;
      }
      emit("change-qty", { product, delta: 1 });
    }
    function onQtyChange(product, delta) {
      if (delta > 0 && !canIncrease(product)) {
        common_vendor.index.showToast({ title: "库存不足", icon: "none" });
        return;
      }
      const next = cartQty(product) + delta;
      if (next < 0)
        return;
      emit("change-qty", { product, delta });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.f(visibleCategories.value, (cat, k0, i0) => {
          return {
            a: common_vendor.t(cat.categoryName),
            b: cat.categoryId,
            c: `sidebar-${cat.categoryId}`,
            d: activeCategoryId.value === cat.categoryId ? 1 : "",
            e: common_vendor.o(($event) => jumpToCategory(cat), cat.categoryId)
          };
        }),
        b: common_vendor.s(mainScrollStyle.value),
        c: sidebarScrollIntoView.value,
        d: !visibleCategories.value.length
      }, !visibleCategories.value.length ? {} : {
        e: common_vendor.f(visibleCategories.value, (cat, k0, i0) => {
          return {
            a: common_vendor.t(cat.categoryName),
            b: common_vendor.f(cat.products, (product, k1, i1) => {
              return common_vendor.e({
                a: product.imageUrl
              }, product.imageUrl ? {
                b: product.imageUrl
              } : {}, {
                c: statusMask(product.status)
              }, statusMask(product.status) ? {
                d: statusMask(product.status)
              } : {}, {
                e: common_vendor.t(product.productName || "—"),
                f: common_vendor.t(formatSold(product.soldCount))
              }, __props.showStock ? {
                g: common_vendor.t(formatStock(product.stock))
              } : {}, {
                h: product.description
              }, product.description ? {
                i: common_vendor.t(product.description)
              } : {}, {
                j: common_vendor.t(formatLinePoints(product))
              }, __props.purchasable ? common_vendor.e({
                k: !cartQty(product)
              }, !cartQty(product) ? {
                l: "84253c4e-0-" + i0 + "-" + i1,
                m: common_vendor.p({
                  type: "cart-filled",
                  size: 14,
                  color: "#ffffff"
                }),
                n: !canPurchase(product) ? 1 : "",
                o: canPurchase(product) ? "tap-hover-scale" : "none",
                p: common_vendor.o(($event) => onBuy(product), product.productId)
              } : {
                q: common_vendor.o(($event) => onQtyChange(product, -1), product.productId),
                r: common_vendor.t(cartQty(product)),
                s: !canIncrease(product) ? 1 : "",
                t: canIncrease(product) ? "tap-hover-opacity" : "none",
                v: common_vendor.o(($event) => onQtyChange(product, 1), product.productId)
              }, {
                w: common_vendor.o(() => {
                }, product.productId)
              }) : {}, {
                x: product.productId,
                y: common_vendor.o(($event) => onProductClick(product), product.productId)
              });
            }),
            c: cat.categoryId,
            d: `cat-${cat.categoryId}`
          };
        }),
        f: __props.showStock,
        g: __props.purchasable,
        h: `${bottomSpacerHeight.value}px`
      }, {
        i: common_vendor.s(mainScrollStyle.value),
        j: mainScrollTop.value,
        k: scrollWithAnimation.value,
        l: common_vendor.o(onMainScroll, "38"),
        m: __props.isDark ? 1 : ""
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-84253c4e"]]);
wx.createComponent(Component);
