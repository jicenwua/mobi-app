"use strict";
const common_vendor = require("../common/vendor.js");
const utils_shopPageContext = require("../utils/shop-page-context.js");
const api_modules_shopManage = require("../api/modules/shop-manage.js");
function emptyProductForm() {
  return {
    name: "",
    points: "",
    stock: "",
    addStock: "",
    description: "",
    imagePath: "",
    imageUrl: "",
    categoryIndex: 0
  };
}
function useShopManageProducts(shopId, { shopInfo, listLoading }) {
  const products = common_vendor.ref([]);
  const productCategories = common_vendor.ref([]);
  const categories = common_vendor.ref([]);
  const productFormVisible = common_vendor.ref(false);
  const categoryFormVisible = common_vendor.ref(false);
  const editingProductId = common_vendor.ref(null);
  const productForm = common_vendor.ref(emptyProductForm());
  const productSubmitting = common_vendor.ref(false);
  const categoryLabels = common_vendor.computed(() => categories.value.map((c) => c.categoryName));
  const currentProductList = common_vendor.computed(
    () => productCategories.value.flatMap((c) => c.products || [])
  );
  async function loadCategories() {
    if (!shopId.value)
      return;
    const res = await api_modules_shopManage.fetchManageProductCategories(Number(shopId.value));
    if (res.ok) {
      categories.value = res.rows || [];
    }
  }
  async function loadProducts() {
    if (listLoading.value)
      return;
    listLoading.value = true;
    const res = await api_modules_shopManage.fetchManageProducts(Number(shopId.value), 1, 50);
    listLoading.value = false;
    if (!res.ok) {
      common_vendor.index.showToast({ title: res.msg || "加载失败", icon: "none" });
      return;
    }
    products.value = res.rows;
    productCategories.value = res.categories || [];
  }
  function findCategoryIndex(categoryId) {
    if (categoryId == null || categoryId === 0)
      return 0;
    const idx = categories.value.findIndex((c) => c.categoryId === categoryId);
    return idx >= 0 ? idx : 0;
  }
  function onProductCategoryChange(e) {
    productForm.value.categoryIndex = Number(e.detail.value) || 0;
  }
  function selectedCategoryId() {
    const cat = categories.value[productForm.value.categoryIndex];
    return (cat == null ? void 0 : cat.categoryId) ?? null;
  }
  function promptAddProduct() {
    editingProductId.value = null;
    productForm.value = emptyProductForm();
    void loadCategories();
    productFormVisible.value = true;
  }
  function editProduct(item) {
    editingProductId.value = item.productId;
    productForm.value = {
      name: item.productName || "",
      points: item.price != null ? String(item.price) : "",
      stock: "",
      addStock: "",
      description: item.description || "",
      imagePath: item.imageUrl || "",
      imageUrl: item.imageUrl || "",
      categoryIndex: findCategoryIndex(item.categoryId)
    };
    void loadCategories();
    productFormVisible.value = true;
  }
  function openProductDetail(product, { onProductUpdated } = {}) {
    if (!(product == null ? void 0 : product.productId) || !shopId.value)
      return;
    utils_shopPageContext.navigateWithShop({
      url: `/pages/shop/product-detail?shopId=${shopId.value}&fromManage=1`,
      shop: shopInfo.value,
      success(res) {
        var _a, _b;
        (_a = res.eventChannel) == null ? void 0 : _a.emit("product", product);
        (_b = res.eventChannel) == null ? void 0 : _b.on("product-updated", (patch) => {
          if (!(patch == null ? void 0 : patch.productId))
            return;
          productCategories.value = productCategories.value.map((cat) => ({
            ...cat,
            products: (cat.products || []).map(
              (p) => p.productId === patch.productId ? { ...p, ...patch } : p
            )
          }));
          onProductUpdated == null ? void 0 : onProductUpdated(patch);
        });
      }
    });
  }
  function pickProductImage() {
    common_vendor.index.chooseImage({
      count: 1,
      sizeType: ["compressed"],
      sourceType: ["album", "camera"],
      success: (res) => {
        var _a;
        const path = (_a = res.tempFilePaths) == null ? void 0 : _a[0];
        if (!path)
          return;
        productForm.value.imagePath = path;
        productForm.value.imageUrl = "";
      }
    });
  }
  function clearProductImage() {
    productForm.value.imagePath = "";
    productForm.value.imageUrl = "";
  }
  function previewProductImage() {
    const src = productForm.value.imagePath;
    if (!src)
      return;
    common_vendor.index.previewImage({ urls: [src], current: src });
  }
  function openCategoryForm() {
    categoryFormVisible.value = true;
  }
  async function onCategoriesSaved() {
    await loadCategories();
    await loadProducts();
    if (productForm.value.categoryIndex >= categories.value.length) {
      productForm.value.categoryIndex = 0;
    }
  }
  function closeProductForm() {
    if (productSubmitting.value)
      return;
    productFormVisible.value = false;
    editingProductId.value = null;
  }
  async function submitProductForm() {
    var _a, _b, _c, _d, _e;
    const name = (_a = productForm.value.name) == null ? void 0 : _a.trim();
    if (!name) {
      common_vendor.index.showToast({ title: "请输入商品名称", icon: "none" });
      return;
    }
    const points = api_modules_shopManage.parseProductPoints(productForm.value.points);
    if (!points) {
      common_vendor.index.showToast({ title: "请输入有效积分", icon: "none" });
      return;
    }
    if (productSubmitting.value)
      return;
    productSubmitting.value = true;
    const descriptionRaw = (_b = productForm.value.description) == null ? void 0 : _b.trim();
    const description = editingProductId.value ? descriptionRaw ?? "" : descriptionRaw || null;
    const categoryId = selectedCategoryId();
    const imagePath = ((_c = productForm.value.imagePath) == null ? void 0 : _c.trim()) || "";
    const basePayload = {
      shopId: Number(shopId.value),
      productName: name,
      price: points,
      description,
      categoryId,
      imagePath: imagePath || void 0
    };
    if (editingProductId.value && !imagePath) {
      basePayload.imageUrl = "";
    }
    let res;
    if (editingProductId.value) {
      res = await api_modules_shopManage.updateManageProduct(editingProductId.value, basePayload);
      if (res.ok) {
        const addRaw = (_d = productForm.value.addStock) == null ? void 0 : _d.trim();
        if (addRaw !== "") {
          const addVal = parseInt(addRaw, 10);
          if (Number.isNaN(addVal) || addVal <= 0) {
            productSubmitting.value = false;
            common_vendor.index.showToast({ title: "追加数量须为正整数", icon: "none" });
            return;
          }
          const stockRes = await api_modules_shopManage.updateManageProductStock(editingProductId.value, addVal);
          if (!stockRes.ok) {
            productSubmitting.value = false;
            common_vendor.index.showToast({ title: stockRes.msg || "库存更新失败", icon: "none" });
            return;
          }
        }
      }
    } else {
      const stockRaw = (_e = productForm.value.stock) == null ? void 0 : _e.trim();
      let stock = -1;
      if (stockRaw !== "") {
        stock = parseInt(stockRaw, 10);
        if (Number.isNaN(stock) || stock < 0) {
          productSubmitting.value = false;
          common_vendor.index.showToast({ title: "库存须为非负整数，或留空表示无限", icon: "none" });
          return;
        }
      }
      res = await api_modules_shopManage.addManageProduct({ ...basePayload, stock });
    }
    productSubmitting.value = false;
    if (!res.ok) {
      common_vendor.index.showToast({ title: res.msg || "保存失败", icon: "none" });
      return;
    }
    common_vendor.index.showToast({ title: editingProductId.value ? "已保存" : "添加成功", icon: "success" });
    productFormVisible.value = false;
    editingProductId.value = null;
    loadProducts();
  }
  return {
    products,
    productCategories,
    categories,
    categoryLabels,
    currentProductList,
    productFormVisible,
    categoryFormVisible,
    editingProductId,
    productForm,
    productSubmitting,
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
  };
}
exports.useShopManageProducts = useShopManageProducts;
