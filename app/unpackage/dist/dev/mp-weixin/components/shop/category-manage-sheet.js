"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_shopManage = require("../../api/modules/shop-manage.js");
const _sfc_main = {
  __name: "category-manage-sheet",
  props: {
    visible: { type: Boolean, default: false },
    shopId: { type: String, default: "" },
    categories: { type: Array, default: () => [] }
  },
  emits: ["update:visible", "saved"],
  setup(__props, { emit: __emit }) {
    const props = __props;
    const emit = __emit;
    const draftItems = common_vendor.ref([]);
    const deletedIds = common_vendor.ref([]);
    const newName = common_vendor.ref("");
    const saving = common_vendor.ref(false);
    let newKeyCounter = 0;
    common_vendor.watch(
      () => props.visible,
      (visible) => {
        if (visible)
          initDraft(props.categories);
      }
    );
    function noop() {
    }
    function initDraft(categories) {
      newKeyCounter = 0;
      deletedIds.value = [];
      newName.value = "";
      draftItems.value = (categories || []).map((c, i) => ({
        key: `id-${c.categoryId}`,
        categoryId: c.categoryId,
        categoryName: c.categoryName || "",
        originalName: c.categoryName || "",
        originalIndex: i
      }));
    }
    function isDirty() {
      if (deletedIds.value.length)
        return true;
      if (newName.value.trim())
        return true;
      return draftItems.value.some((item, index) => {
        if (!item.categoryId)
          return true;
        return item.categoryName.trim() !== item.originalName || index !== item.originalIndex;
      });
    }
    function moveItem(from, to) {
      if (from === to || from < 0 || to < 0 || to >= draftItems.value.length)
        return;
      const list = [...draftItems.value];
      const [item] = list.splice(from, 1);
      list.splice(to, 0, item);
      draftItems.value = list;
    }
    function moveItemUp(index) {
      if (index <= 0)
        return;
      moveItem(index, index - 1);
    }
    function moveItemDown(index) {
      if (index >= draftItems.value.length - 1)
        return;
      moveItem(index, index + 1);
    }
    function addToDraft() {
      var _a;
      const name = (_a = newName.value) == null ? void 0 : _a.trim();
      if (!name) {
        common_vendor.index.showToast({ title: "请输入分类名称", icon: "none" });
        return;
      }
      if (hasDuplicateName(name)) {
        common_vendor.index.showToast({ title: "分类名称已存在", icon: "none" });
        return;
      }
      newKeyCounter += 1;
      draftItems.value.push({
        key: `new-${newKeyCounter}`,
        categoryId: null,
        categoryName: name,
        originalName: null,
        originalIndex: null
      });
      newName.value = "";
    }
    function hasDuplicateName(name, ignoreIndex = -1) {
      const normalized = name.trim();
      return draftItems.value.some((item, index) => index !== ignoreIndex && item.categoryName.trim() === normalized);
    }
    function removeItem(index) {
      const item = draftItems.value[index];
      const doRemove = () => {
        if (item.categoryId)
          deletedIds.value.push(item.categoryId);
        draftItems.value.splice(index, 1);
      };
      if (!item.categoryId) {
        doRemove();
        return;
      }
      common_vendor.index.showModal({
        title: "删除分类",
        content: `确定删除「${item.categoryName}」吗？分类下仍有商品时无法删除。`,
        success: (r) => {
          if (r.confirm)
            doRemove();
        }
      });
    }
    function onMaskClick() {
      onCancel();
    }
    function onCancel() {
      if (saving.value)
        return;
      if (!isDirty()) {
        emit("update:visible", false);
        return;
      }
      common_vendor.index.showModal({
        title: "放弃修改",
        content: "有未保存的修改，确定放弃吗？",
        success: (r) => {
          if (r.confirm)
            emit("update:visible", false);
        }
      });
    }
    async function onComplete() {
      var _a, _b;
      if (saving.value)
        return;
      const pendingName = (_a = newName.value) == null ? void 0 : _a.trim();
      if (pendingName) {
        if (hasDuplicateName(pendingName)) {
          common_vendor.index.showToast({ title: "分类名称已存在", icon: "none" });
          return;
        }
        addToDraft();
      }
      const items = draftItems.value;
      if (!items.length && !deletedIds.value.length) {
        emit("update:visible", false);
        return;
      }
      for (let i = 0; i < items.length; i++) {
        const name = (_b = items[i].categoryName) == null ? void 0 : _b.trim();
        if (!name) {
          common_vendor.index.showToast({ title: `第 ${i + 1} 项分类名称不能为空`, icon: "none" });
          return;
        }
        if (hasDuplicateName(name, i)) {
          common_vendor.index.showToast({ title: "分类名称不能重复", icon: "none" });
          return;
        }
      }
      if (!props.shopId) {
        common_vendor.index.showToast({ title: "缺少店铺 ID", icon: "none" });
        return;
      }
      if (!isDirty()) {
        emit("update:visible", false);
        return;
      }
      saving.value = true;
      const shopIdNum = Number(props.shopId);
      for (const categoryId of deletedIds.value) {
        const res = await api_modules_shopManage.deleteManageProductCategory(categoryId);
        if (!res.ok) {
          saving.value = false;
          common_vendor.index.showToast({ title: res.msg || "删除失败", icon: "none" });
          return;
        }
      }
      const newEntries = items.map((item, index) => ({ item, index })).filter(({ item }) => !item.categoryId);
      if (newEntries.length) {
        const res = await api_modules_shopManage.addManageProductCategories(
          newEntries.map(({ item, index }) => ({
            shopId: shopIdNum,
            categoryName: item.categoryName.trim(),
            sortOrder: index
          }))
        );
        if (!res.ok) {
          saving.value = false;
          common_vendor.index.showToast({ title: res.msg || "添加失败", icon: "none" });
          return;
        }
      }
      const updates = [];
      for (let i = 0; i < items.length; i++) {
        const item = items[i];
        if (!item.categoryId)
          continue;
        const name = item.categoryName.trim();
        const nameChanged = name !== item.originalName;
        const sortChanged = i !== item.originalIndex;
        if (!nameChanged && !sortChanged)
          continue;
        const entry = { categoryId: item.categoryId };
        if (nameChanged)
          entry.categoryName = name;
        if (sortChanged)
          entry.sortOrder = i;
        updates.push(entry);
      }
      if (updates.length) {
        const res = await api_modules_shopManage.updateManageProductCategories(updates);
        if (!res.ok) {
          saving.value = false;
          common_vendor.index.showToast({ title: res.msg || "保存失败", icon: "none" });
          return;
        }
      }
      saving.value = false;
      common_vendor.index.showToast({ title: "已保存", icon: "success" });
      emit("saved");
      emit("update:visible", false);
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.visible
      }, __props.visible ? common_vendor.e({
        b: !draftItems.value.length
      }, !draftItems.value.length ? {} : {}, {
        c: common_vendor.f(draftItems.value, (item, index, i0) => {
          return {
            a: index === 0 ? 1 : "",
            b: common_vendor.o(($event) => moveItemUp(index), item.key),
            c: index === draftItems.value.length - 1 ? 1 : "",
            d: common_vendor.o(($event) => moveItemDown(index), item.key),
            e: item.categoryName,
            f: common_vendor.o(($event) => item.categoryName = $event.detail.value, item.key),
            g: common_vendor.o(($event) => removeItem(index), item.key),
            h: item.key
          };
        }),
        d: common_vendor.o(addToDraft, "96"),
        e: newName.value,
        f: common_vendor.o(($event) => newName.value = $event.detail.value, "86"),
        g: common_vendor.o(addToDraft, "f5"),
        h: common_vendor.o(onCancel, "8b"),
        i: common_vendor.t(saving.value ? "保存中…" : "完成"),
        j: common_vendor.o(onComplete, "54"),
        k: common_vendor.o(() => {
        }, "df"),
        l: common_vendor.o(onMaskClick, "77"),
        m: common_vendor.o(noop, "49")
      }) : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-04384a97"]]);
wx.createComponent(Component);
