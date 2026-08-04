"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_shopManage = require("../../api/modules/shop-manage.js");
const utils_shopRole = require("../../utils/shop-role.js");
const utils_shopPageContext = require("../../utils/shop-page-context.js");
const utils_productStatus = require("../../utils/product-status.js");
const utils_shopCartCache = require("../../utils/shop-cart-cache.js");
const utils_navigation = require("../../utils/navigation.js");
if (!Array) {
  const _easycom_uni_icons2 = common_vendor.resolveComponent("uni-icons");
  _easycom_uni_icons2();
}
const _easycom_uni_icons = () => "../../uni_modules/uni-icons/components/uni-icons/uni-icons.js";
if (!Math) {
  (_easycom_uni_icons + ProductEditSheet)();
}
const ProductEditSheet = () => "../../components/shop/product-edit-sheet.js";
const _sfc_main = {
  __name: "product-detail",
  setup(__props) {
    const shopId = common_vendor.ref("");
    const product = common_vendor.ref(null);
    const shopInfo = common_vendor.ref(null);
    const isManager = common_vendor.ref(false);
    const memberMode = common_vendor.ref(false);
    const editVisible = common_vendor.ref(false);
    const shelfSubmitting = common_vendor.ref(false);
    const cartQty = common_vendor.ref(0);
    const isOffShelf = common_vendor.computed(() => {
      var _a;
      return utils_productStatus.isProductOffShelf((_a = product.value) == null ? void 0 : _a.status);
    });
    const statusMaskSrc = common_vendor.computed(() => {
      var _a;
      return utils_productStatus.productStatusMask((_a = product.value) == null ? void 0 : _a.status);
    });
    const canPurchase = common_vendor.computed(() => {
      const p = product.value;
      if (!p || !utils_productStatus.isProductOnSale(p.status))
        return false;
      const stock = p.stock;
      if (stock != null && stock !== "" && Number(stock) !== -1 && Number(stock) <= 0)
        return false;
      return true;
    });
    const canIncrease = common_vendor.computed(() => {
      var _a;
      if (!canPurchase.value)
        return false;
      const stock = (_a = product.value) == null ? void 0 : _a.stock;
      if (stock == null || stock === "" || Number(stock) === -1)
        return true;
      return cartQty.value < Number(stock);
    });
    common_vendor.onLoad((options) => {
      shopId.value = (options == null ? void 0 : options.shopId) ? String(options.shopId) : "";
      memberMode.value = (options == null ? void 0 : options.member) === "1" && (options == null ? void 0 : options.fromManage) !== "1";
      const channel = utils_shopPageContext.getOpenerEventChannel();
      channel == null ? void 0 : channel.on("product", (data) => {
        if (data)
          product.value = data;
        refreshCartQty();
      });
      utils_shopPageContext.bindOpenerShop((data) => {
        shopInfo.value = data;
        isManager.value = utils_shopRole.isShopManager(data);
        if (isManager.value)
          memberMode.value = false;
      });
      if ((options == null ? void 0 : options.fromManage) === "1") {
        isManager.value = true;
        memberMode.value = false;
      }
      refreshCartQty();
    });
    common_vendor.onShow(() => {
      refreshCartQty();
    });
    function refreshCartQty() {
      var _a;
      cartQty.value = utils_shopCartCache.getShopCartQty(shopId.value, (_a = product.value) == null ? void 0 : _a.productId);
    }
    function addToCart() {
      if (!canPurchase.value) {
        common_vendor.index.showToast({ title: "该商品暂不可购买", icon: "none" });
        return;
      }
      changeCartQty(1);
    }
    function changeCartQty(delta) {
      var _a;
      if (!((_a = product.value) == null ? void 0 : _a.productId) || !shopId.value)
        return;
      if (delta > 0 && !canIncrease.value) {
        common_vendor.index.showToast({ title: "库存不足", icon: "none" });
        return;
      }
      utils_shopCartCache.changeShopCartQty(shopId.value, product.value.productId, delta);
      refreshCartQty();
      if (delta > 0 && cartQty.value === 1) {
        common_vendor.index.showToast({ title: "已加入购物车", icon: "success", duration: 1200 });
      }
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
    function onEdit() {
      var _a;
      if (!((_a = product.value) == null ? void 0 : _a.productId) || !shopId.value)
        return;
      editVisible.value = true;
    }
    function onProductSaved(patch) {
      if (!product.value)
        return;
      product.value = { ...product.value, ...patch };
    }
    function onToggleShelf() {
      var _a;
      if (!((_a = product.value) == null ? void 0 : _a.productId) || shelfSubmitting.value)
        return;
      const offShelf = isOffShelf.value;
      const title = offShelf ? "上架商品" : "下架商品";
      const content = offShelf ? `确定将「${product.value.productName}」重新上架吗？` : `确定将「${product.value.productName}」下架吗？下架后顾客将无法兑换。`;
      common_vendor.index.showModal({
        title,
        content,
        success: async (r) => {
          var _a2, _b;
          if (!r.confirm)
            return;
          shelfSubmitting.value = true;
          const nextStatus = offShelf ? utils_productStatus.PRODUCT_STATUS.ON_SALE : utils_productStatus.PRODUCT_STATUS.OFF_SHELF;
          const res = await api_modules_shopManage.updateManageProductStatus(product.value.productId, nextStatus);
          shelfSubmitting.value = false;
          if (!res.ok) {
            common_vendor.index.showToast({ title: res.msg || "操作失败", icon: "none" });
            return;
          }
          product.value = { ...product.value, status: nextStatus };
          const pages = getCurrentPages();
          const page = pages[pages.length - 1];
          (_b = (_a2 = page == null ? void 0 : page.getOpenerEventChannel) == null ? void 0 : _a2.call(page)) == null ? void 0 : _b.emit("product-updated", {
            productId: product.value.productId,
            status: nextStatus
          });
          common_vendor.index.showToast({ title: offShelf ? "已上架" : "已下架", icon: "success" });
        }
      });
    }
    function onDelete() {
      var _a;
      if (!((_a = product.value) == null ? void 0 : _a.productId))
        return;
      common_vendor.index.showModal({
        title: "删除商品",
        content: `确定删除「${product.value.productName}」吗？`,
        success: async (r) => {
          if (!r.confirm)
            return;
          const res = await api_modules_shopManage.deleteManageProduct(product.value.productId);
          if (!res.ok) {
            common_vendor.index.showToast({ title: res.msg || "删除失败", icon: "none" });
            return;
          }
          common_vendor.index.showToast({ title: "已删除", icon: "success" });
          utils_navigation.navigateBackDelayed(400);
        }
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: !product.value
      }, !product.value ? {} : common_vendor.e({
        b: product.value.imageUrl
      }, product.value.imageUrl ? {
        c: product.value.imageUrl
      } : {}, {
        d: statusMaskSrc.value
      }, statusMaskSrc.value ? {
        e: statusMaskSrc.value
      } : {}, {
        f: common_vendor.t(formatPoints(product.value.price)),
        g: common_vendor.t(common_vendor.unref(utils_productStatus.productStatusLabel)(product.value.status)),
        h: product.value.status === common_vendor.unref(utils_productStatus.PRODUCT_STATUS).SOLD_OUT ? 1 : "",
        i: product.value.status === common_vendor.unref(utils_productStatus.PRODUCT_STATUS).OFF_SHELF ? 1 : "",
        j: common_vendor.t(product.value.productName || "—"),
        k: common_vendor.t(formatSold(product.value.soldCount)),
        l: !memberMode.value
      }, !memberMode.value ? {
        m: common_vendor.t(formatStock(product.value.stock))
      } : {}, {
        n: product.value.description
      }, product.value.description ? {
        o: common_vendor.t(product.value.description)
      } : {}, {
        p: memberMode.value && product.value
      }, memberMode.value && product.value ? common_vendor.e({
        q: !cartQty.value
      }, !cartQty.value ? {
        r: common_vendor.p({
          type: "cart-filled",
          size: 16,
          color: "#ffffff"
        }),
        s: !canPurchase.value ? 1 : "",
        t: common_vendor.o(addToCart, "1e")
      } : {
        v: common_vendor.o(($event) => changeCartQty(-1), "b0"),
        w: common_vendor.t(cartQty.value),
        x: !canIncrease.value ? 1 : "",
        y: common_vendor.o(($event) => changeCartQty(1), "de")
      }) : {}, {
        z: isManager.value
      }, isManager.value ? {
        A: common_vendor.o(onEdit, "39"),
        B: common_vendor.t(isOffShelf.value ? "上架" : "下架"),
        C: common_vendor.n(isOffShelf.value ? "manager-btn--primary" : "manager-btn--warn"),
        D: common_vendor.o(onToggleShelf, "28"),
        E: common_vendor.o(onDelete, "51")
      } : {}, {
        F: common_vendor.o(onProductSaved, "0c"),
        G: common_vendor.o(($event) => editVisible.value = $event, "9a"),
        H: common_vendor.p({
          ["shop-id"]: shopId.value,
          product: product.value,
          visible: editVisible.value
        })
      }), {
        I: memberMode.value && product.value ? 1 : "",
        J: isManager.value && product.value ? 1 : ""
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-7d1d36da"]]);
wx.createPage(MiniProgramPage);
