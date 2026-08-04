"use strict";
const common_vendor = require("../common/vendor.js");
const api_modules_shopManage = require("../api/modules/shop-manage.js");
const utils_couponManageDisplay = require("../utils/coupon-manage-display.js");
function emptyCouponForm() {
  return {
    couponName: "",
    type: 1,
    thresholdAmount: "",
    discountValue: "",
    validDays: "",
    neverExpire: false,
    totalQuantity: "",
    distributionStartDate: "",
    distributionEndDate: ""
  };
}
function useShopManageCoupons(shopId, { isManager, listLoading }) {
  const coupons = common_vendor.ref([]);
  const couponStockVisible = common_vendor.ref(false);
  const couponStockTarget = common_vendor.ref(null);
  const couponStockForm = common_vendor.ref({ quantity: "" });
  const couponStockSubmitting = common_vendor.ref(false);
  const couponFormVisible = common_vendor.ref(false);
  const editingCouponId = common_vendor.ref(null);
  const editingCouponItem = common_vendor.ref(null);
  const couponTypeIndex = common_vendor.ref(0);
  const couponForm = common_vendor.ref(emptyCouponForm());
  const couponSubmitting = common_vendor.ref(false);
  const couponTypeLabels = utils_couponManageDisplay.COUPON_TYPE_PICKER_LABELS;
  async function loadCoupons() {
    if (listLoading.value)
      return;
    listLoading.value = true;
    const res = await api_modules_shopManage.fetchManageCoupons(Number(shopId.value), 1, 50);
    listLoading.value = false;
    if (!res.ok) {
      common_vendor.index.showToast({ title: res.msg || "加载失败", icon: "none" });
      return;
    }
    coupons.value = res.rows;
  }
  function onCouponTypeChange(e) {
    couponTypeIndex.value = Number(e.detail.value);
    couponForm.value.type = couponTypeIndex.value === 0 ? 1 : 2;
  }
  function promptAddCoupon() {
    editingCouponId.value = null;
    editingCouponItem.value = null;
    couponTypeIndex.value = 0;
    couponForm.value = emptyCouponForm();
    couponFormVisible.value = true;
  }
  function editCoupon(item) {
    if (!utils_couponManageDisplay.canEditCoupon(item))
      return;
    editingCouponId.value = item.templateId;
    editingCouponItem.value = item;
    couponTypeIndex.value = item.type === 2 ? 1 : 0;
    couponForm.value = {
      couponName: item.couponName || "",
      type: item.type === 2 ? 2 : 1,
      thresholdAmount: item.thresholdAmount != null ? String(item.thresholdAmount) : "",
      discountValue: item.discountValue != null ? String(item.discountValue) : "",
      validDays: item.validDays != null ? String(item.validDays) : "",
      neverExpire: item.validDays == null || item.validDays === "",
      totalQuantity: item.totalQuantity != null ? String(item.totalQuantity) : "",
      distributionStartDate: item.distributionStartTime ? utils_couponManageDisplay.formatManageCouponDate(item.distributionStartTime) : "",
      distributionEndDate: item.distributionEndTime ? utils_couponManageDisplay.formatManageCouponDate(item.distributionEndTime) : ""
    };
    couponFormVisible.value = true;
  }
  function formatCouponRuleFromForm() {
    return utils_couponManageDisplay.formatCouponRule({
      type: couponForm.value.type,
      thresholdAmount: couponForm.value.thresholdAmount,
      discountValue: couponForm.value.discountValue
    });
  }
  function formatValidDaysFromForm() {
    if (couponForm.value.neverExpire)
      return "领取后永久";
    const days = Number(couponForm.value.validDays);
    if (!days || Number.isNaN(days))
      return "领取后永久";
    return `领取后 ${days} 天`;
  }
  function onNeverExpireChange(e) {
    couponForm.value.neverExpire = !!e.detail.value;
    if (couponForm.value.neverExpire) {
      couponForm.value.validDays = "";
    }
  }
  function closeCouponForm() {
    if (couponSubmitting.value)
      return;
    couponFormVisible.value = false;
    editingCouponId.value = null;
    editingCouponItem.value = null;
  }
  async function doResumeOrStopCoupon(templateId, action, fromForm = false) {
    couponSubmitting.value = true;
    const sid = Number(shopId.value);
    const res = action === "resume" ? await api_modules_shopManage.resumeManageCouponDistribution(templateId, sid) : await api_modules_shopManage.stopManageCouponDistribution(templateId, sid);
    couponSubmitting.value = false;
    if (!res.ok) {
      common_vendor.index.showToast({ title: res.msg || "操作失败", icon: "none" });
      return;
    }
    common_vendor.index.showToast({ title: res.msg || (action === "resume" ? "已恢复发放" : "已停止发放"), icon: "success" });
    if (fromForm) {
      couponFormVisible.value = false;
      editingCouponId.value = null;
      editingCouponItem.value = null;
    }
    loadCoupons();
  }
  function stopCouponDistribution() {
    if (!editingCouponId.value || couponSubmitting.value)
      return;
    common_vendor.index.showModal({
      title: "停止发放",
      content: "停止后用户将无法继续领取该优惠券，确定停止吗？",
      success: async (r) => {
        if (!r.confirm)
          return;
        await doResumeOrStopCoupon(editingCouponId.value, "stop", true);
      }
    });
  }
  function resumeCouponDistributionFromForm() {
    if (!editingCouponId.value || couponSubmitting.value)
      return;
    common_vendor.index.showModal({
      title: "恢复发放",
      content: "恢复后将取消手动停止，按当前发放时间与库存状态继续，确定恢复吗？",
      success: async (r) => {
        if (!r.confirm)
          return;
        await doResumeOrStopCoupon(editingCouponId.value, "resume", true);
      }
    });
  }
  function resumeCouponDistribution(item) {
    if (!(item == null ? void 0 : item.templateId) || couponSubmitting.value)
      return;
    common_vendor.index.showModal({
      title: "恢复发放",
      content: `确定恢复「${item.couponName}」的发放吗？`,
      success: async (r) => {
        if (!r.confirm)
          return;
        await doResumeOrStopCoupon(item.templateId, "resume");
      }
    });
  }
  async function submitCouponForm() {
    var _a, _b;
    if (editingCouponId.value) {
      await submitCouponDistributionEdit();
      return;
    }
    const name = (_a = couponForm.value.couponName) == null ? void 0 : _a.trim();
    if (!name) {
      common_vendor.index.showToast({ title: "请输入折扣券名称", icon: "none" });
      return;
    }
    const thresholdAmount = api_modules_shopManage.parseOptionalAmount(couponForm.value.thresholdAmount);
    if (thresholdAmount == null) {
      common_vendor.index.showToast({ title: "使用门槛须为非负数", icon: "none" });
      return;
    }
    const discountValue = api_modules_shopManage.parsePositiveAmount(couponForm.value.discountValue);
    if (!discountValue) {
      common_vendor.index.showToast({ title: couponForm.value.type === 1 ? "请输入有效折扣力度" : "请输入有效减免积分", icon: "none" });
      return;
    }
    if (couponForm.value.type === 1 && discountValue > 100) {
      common_vendor.index.showToast({ title: "折扣力度不能超过 100", icon: "none" });
      return;
    }
    const totalQuantityRaw = (_b = couponForm.value.totalQuantity) == null ? void 0 : _b.trim();
    const totalQuantity = api_modules_shopManage.parseOptionalPositiveInt(couponForm.value.totalQuantity);
    if (totalQuantityRaw && totalQuantity == null) {
      common_vendor.index.showToast({ title: "发放数量须为正整数，或留空表示无限", icon: "none" });
      return;
    }
    if (!couponForm.value.neverExpire) {
      const validDays2 = api_modules_shopManage.parsePositiveInt(couponForm.value.validDays);
      if (!validDays2) {
        common_vendor.index.showToast({ title: "请输入领取后有效天数", icon: "none" });
        return;
      }
      if (validDays2 > 365) {
        common_vendor.index.showToast({ title: "有效天数不能超过 365 天", icon: "none" });
        return;
      }
    }
    if (couponForm.value.distributionStartDate && couponForm.value.distributionEndDate && couponForm.value.distributionStartDate > couponForm.value.distributionEndDate) {
      common_vendor.index.showToast({ title: "结束发放时间须晚于开始时间", icon: "none" });
      return;
    }
    if (couponSubmitting.value)
      return;
    couponSubmitting.value = true;
    const validDays = couponForm.value.neverExpire ? null : api_modules_shopManage.parsePositiveInt(couponForm.value.validDays);
    const res = await api_modules_shopManage.addManageCoupon({
      shopId: Number(shopId.value),
      couponName: name,
      type: couponForm.value.type,
      thresholdAmount,
      discountValue,
      validDays,
      effectiveTime: null,
      expireTime: null,
      totalQuantity,
      distributionStartTime: api_modules_shopManage.dateToDateTime(couponForm.value.distributionStartDate),
      distributionEndTime: api_modules_shopManage.dateToDateTimeEnd(couponForm.value.distributionEndDate)
    });
    couponSubmitting.value = false;
    if (!res.ok) {
      common_vendor.index.showToast({ title: res.msg || "添加失败", icon: "none" });
      return;
    }
    common_vendor.index.showToast({ title: "添加成功", icon: "success" });
    couponFormVisible.value = false;
    editingCouponId.value = null;
    editingCouponItem.value = null;
    loadCoupons();
  }
  async function submitCouponDistributionEdit() {
    if (couponForm.value.distributionStartDate && couponForm.value.distributionEndDate && couponForm.value.distributionStartDate > couponForm.value.distributionEndDate) {
      common_vendor.index.showToast({ title: "结束发放时间须晚于开始时间", icon: "none" });
      return;
    }
    if (couponSubmitting.value)
      return;
    couponSubmitting.value = true;
    const res = await api_modules_shopManage.adjustManageCouponDistribution(editingCouponId.value, {
      shopId: Number(shopId.value),
      distributionStartTime: api_modules_shopManage.dateToDateTime(couponForm.value.distributionStartDate),
      distributionEndTime: api_modules_shopManage.dateToDateTimeEnd(couponForm.value.distributionEndDate)
    });
    couponSubmitting.value = false;
    if (!res.ok) {
      common_vendor.index.showToast({ title: res.msg || "保存失败", icon: "none" });
      return;
    }
    common_vendor.index.showToast({ title: "保存成功", icon: "success" });
    couponFormVisible.value = false;
    editingCouponId.value = null;
    editingCouponItem.value = null;
    loadCoupons();
  }
  function deleteCoupon(item) {
    if (!isManager.value)
      return;
    common_vendor.index.showModal({
      title: "删除折扣券",
      content: `确定删除「${item.couponName}」吗？`,
      success: async (r) => {
        if (!r.confirm)
          return;
        const res = await api_modules_shopManage.deleteManageCoupon(item.templateId);
        if (!res.ok) {
          common_vendor.index.showToast({ title: res.msg || "删除失败", icon: "none" });
          return;
        }
        common_vendor.index.showToast({ title: "已删除", icon: "success" });
        loadCoupons();
      }
    });
  }
  function promptAddCouponStock(item) {
    couponStockTarget.value = item;
    couponStockForm.value = { quantity: "" };
    couponStockVisible.value = true;
  }
  function closeCouponStockForm() {
    if (couponStockSubmitting.value)
      return;
    couponStockVisible.value = false;
    couponStockTarget.value = null;
  }
  async function submitCouponStockForm() {
    const addQuantity = api_modules_shopManage.parsePositiveInt(couponStockForm.value.quantity);
    if (!addQuantity) {
      common_vendor.index.showToast({ title: "请输入有效数量", icon: "none" });
      return;
    }
    if (couponStockSubmitting.value || !couponStockTarget.value)
      return;
    couponStockSubmitting.value = true;
    const res = await api_modules_shopManage.addManageCouponStock(couponStockTarget.value.templateId, {
      shopId: Number(shopId.value),
      addQuantity
    });
    couponStockSubmitting.value = false;
    if (!res.ok) {
      common_vendor.index.showToast({ title: res.msg || "操作失败", icon: "none" });
      return;
    }
    common_vendor.index.showToast({ title: "库存已追加", icon: "success" });
    couponStockVisible.value = false;
    couponStockTarget.value = null;
    loadCoupons();
  }
  return {
    coupons,
    couponStockVisible,
    couponStockTarget,
    couponStockForm,
    couponStockSubmitting,
    couponFormVisible,
    editingCouponId,
    editingCouponItem,
    couponTypeIndex,
    couponTypeLabels,
    couponForm,
    couponSubmitting,
    loadCoupons,
    onCouponTypeChange,
    promptAddCoupon,
    editCoupon,
    formatCouponRuleFromForm,
    formatValidDaysFromForm,
    onNeverExpireChange,
    closeCouponForm,
    stopCouponDistribution,
    resumeCouponDistributionFromForm,
    resumeCouponDistribution,
    submitCouponForm,
    deleteCoupon,
    promptAddCouponStock,
    closeCouponStockForm,
    submitCouponStockForm,
    canEditCoupon: utils_couponManageDisplay.canEditCoupon,
    canStopCouponDistribution: utils_couponManageDisplay.canStopCouponDistribution,
    canResumeCouponDistribution: utils_couponManageDisplay.canResumeCouponDistribution,
    formatValidDays: utils_couponManageDisplay.formatValidDays
  };
}
exports.useShopManageCoupons = useShopManageCoupons;
