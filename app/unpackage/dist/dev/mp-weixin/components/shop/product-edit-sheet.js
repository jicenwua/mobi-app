"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_shopManage = require("../../api/modules/shop-manage.js");
const _sfc_main = {
  __name: "product-edit-sheet",
  props: {
    visible: { type: Boolean, default: false },
    shopId: { type: String, default: "" },
    product: { type: Object, default: null }
  },
  emits: ["update:visible", "saved"],
  setup(__props, { emit: __emit }) {
    const props = __props;
    const emit = __emit;
    const submitting = common_vendor.ref(false);
    const categories = common_vendor.ref([]);
    const form = common_vendor.ref({
      name: "",
      points: "",
      addStock: "",
      description: "",
      imagePath: "",
      categoryIndex: 0
    });
    const categoryLabels = common_vendor.computed(() => categories.value.map((c) => c.categoryName));
    common_vendor.watch(
      () => [props.visible, props.product],
      ([visible, product]) => {
        if (!visible || !product)
          return;
        void initForm(product);
      },
      { immediate: true }
    );
    async function initForm(product) {
      await loadCategories();
      const idx = categories.value.findIndex((c) => c.categoryId === product.categoryId);
      form.value = {
        name: product.productName || "",
        points: product.price != null ? String(product.price) : "",
        addStock: "",
        description: product.description || "",
        imagePath: product.imageUrl || "",
        categoryIndex: idx >= 0 ? idx : 0
      };
    }
    async function loadCategories() {
      if (!props.shopId)
        return;
      const res = await api_modules_shopManage.fetchManageProductCategories(Number(props.shopId));
      if (res.ok)
        categories.value = res.rows || [];
    }
    function selectedCategoryId() {
      const cat = categories.value[form.value.categoryIndex];
      return (cat == null ? void 0 : cat.categoryId) ?? null;
    }
    function onCategoryChange(e) {
      form.value.categoryIndex = Number(e.detail.value) || 0;
    }
    function pickImage() {
      common_vendor.index.chooseImage({
        count: 1,
        sizeType: ["compressed"],
        sourceType: ["album", "camera"],
        success: (res) => {
          var _a;
          const path = (_a = res.tempFilePaths) == null ? void 0 : _a[0];
          if (path)
            form.value.imagePath = path;
        }
      });
    }
    function clearImage() {
      form.value.imagePath = "";
    }
    function previewImage() {
      const src = form.value.imagePath;
      if (src)
        common_vendor.index.previewImage({ urls: [src], current: src });
    }
    function onClose() {
      if (submitting.value)
        return;
      emit("update:visible", false);
    }
    async function submit() {
      var _a, _b, _c, _d, _e, _f;
      const productId = (_a = props.product) == null ? void 0 : _a.productId;
      if (!productId || !props.shopId)
        return;
      const name = (_b = form.value.name) == null ? void 0 : _b.trim();
      if (!name) {
        common_vendor.index.showToast({ title: "请输入商品名称", icon: "none" });
        return;
      }
      const points = api_modules_shopManage.parseProductPoints(form.value.points);
      if (!points) {
        common_vendor.index.showToast({ title: "请输入有效积分", icon: "none" });
        return;
      }
      if (submitting.value)
        return;
      submitting.value = true;
      const descriptionRaw = (_c = form.value.description) == null ? void 0 : _c.trim();
      const imagePath = ((_d = form.value.imagePath) == null ? void 0 : _d.trim()) || "";
      const basePayload = {
        shopId: Number(props.shopId),
        productName: name,
        price: points,
        description: descriptionRaw ?? "",
        categoryId: selectedCategoryId(),
        imagePath: imagePath || void 0
      };
      if (!imagePath)
        basePayload.imageUrl = "";
      const res = await api_modules_shopManage.updateManageProduct(productId, basePayload);
      if (res.ok) {
        const addRaw = (_e = form.value.addStock) == null ? void 0 : _e.trim();
        if (addRaw !== "") {
          const addVal = parseInt(addRaw, 10);
          if (Number.isNaN(addVal) || addVal <= 0) {
            submitting.value = false;
            common_vendor.index.showToast({ title: "追加数量须为正整数", icon: "none" });
            return;
          }
          const stockRes = await api_modules_shopManage.updateManageProductStock(productId, addVal);
          if (!stockRes.ok) {
            submitting.value = false;
            common_vendor.index.showToast({ title: stockRes.msg || "库存更新失败", icon: "none" });
            return;
          }
        }
      }
      submitting.value = false;
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "保存失败", icon: "none" });
        return;
      }
      common_vendor.index.showToast({ title: "已保存", icon: "success" });
      emit("update:visible", false);
      emit("saved", {
        productName: name,
        price: points,
        description: descriptionRaw ?? "",
        imageUrl: imagePath || ((_f = props.product) == null ? void 0 : _f.imageUrl) || "",
        categoryId: selectedCategoryId()
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.visible
      }, __props.visible ? common_vendor.e({
        b: form.value.imagePath
      }, form.value.imagePath ? {
        c: form.value.imagePath,
        d: common_vendor.o(clearImage, "4a"),
        e: common_vendor.o(previewImage, "5c")
      } : {
        f: common_vendor.o(pickImage, "a9")
      }, {
        g: categories.value.length
      }, categories.value.length ? {
        h: common_vendor.t(categoryLabels.value[form.value.categoryIndex]),
        i: categoryLabels.value,
        j: form.value.categoryIndex,
        k: common_vendor.o(onCategoryChange, "12")
      } : {}, {
        l: form.value.name,
        m: common_vendor.o(($event) => form.value.name = $event.detail.value, "01"),
        n: form.value.description,
        o: common_vendor.o(($event) => form.value.description = $event.detail.value, "d1"),
        p: form.value.points,
        q: common_vendor.o(($event) => form.value.points = $event.detail.value, "86"),
        r: form.value.addStock,
        s: common_vendor.o(($event) => form.value.addStock = $event.detail.value, "12"),
        t: common_vendor.o(onClose, "14"),
        v: common_vendor.o(submit, "d3"),
        w: common_vendor.o(() => {
        }, "59"),
        x: common_vendor.o(() => {
        }, "46"),
        y: common_vendor.o(onClose, "bc")
      }) : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-59f82df3"]]);
wx.createComponent(Component);
