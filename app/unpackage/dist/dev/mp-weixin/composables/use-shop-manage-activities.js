"use strict";
const common_vendor = require("../common/vendor.js");
const api_modules_shopManage = require("../api/modules/shop-manage.js");
const utils_withClickLock = require("../utils/with-click-lock.js");
const utils_shopPageContext = require("../utils/shop-page-context.js");
function showModalAsync(options) {
  return new Promise((resolve) => {
    common_vendor.index.showModal({
      ...options,
      success: resolve
    });
  });
}
function useShopManageActivities(shopId, { isManager, listLoading, shopInfo }) {
  const activities = common_vendor.ref([]);
  const needReloadActivities = common_vendor.ref(false);
  async function loadActivities() {
    if (listLoading.value)
      return;
    listLoading.value = true;
    const res = await api_modules_shopManage.fetchManageActivities(Number(shopId.value), 1, 50);
    listLoading.value = false;
    if (!res.ok) {
      common_vendor.index.showToast({ title: res.msg || "加载失败", icon: "none" });
      return;
    }
    activities.value = res.rows;
  }
  function goActivityEdit(activityId) {
    if (!isManager.value) {
      common_vendor.index.showToast({ title: "仅店长可编辑活动", icon: "none" });
      return;
    }
    const q = activityId ? `&activityId=${activityId}` : "";
    needReloadActivities.value = true;
    utils_shopPageContext.navigateWithShop({
      url: `/pages/shop/activity-edit?shopId=${shopId.value}${q}`,
      shop: shopInfo == null ? void 0 : shopInfo.value
    });
  }
  function editActivity(item) {
    goActivityEdit(item.activityId);
  }
  const deleteActivity = utils_withClickLock.withClickLock(async (item) => {
    if (!isManager.value) {
      common_vendor.index.showToast({ title: "仅店长可删除活动", icon: "none" });
      return;
    }
    const r = await showModalAsync({
      title: "删除活动",
      content: `确定删除「${item.activityName}」吗？`
    });
    if (!r.confirm)
      return;
    const res = await api_modules_shopManage.deleteManageActivity(item.activityId);
    if (!res.ok) {
      common_vendor.index.showToast({ title: res.msg || "删除失败", icon: "none" });
      return;
    }
    common_vendor.index.showToast({ title: "已删除", icon: "success" });
    loadActivities();
  });
  const stopActivity = utils_withClickLock.withClickLock(async (item) => {
    if (!isManager.value) {
      common_vendor.index.showToast({ title: "仅店长可停止活动", icon: "none" });
      return;
    }
    const r = await showModalAsync({
      title: "停止活动",
      content: `确定手动停止「${item.activityName}」吗？`
    });
    if (!r.confirm)
      return;
    const res = await api_modules_shopManage.stopManageActivity(item.activityId, shopId.value);
    if (!res.ok) {
      common_vendor.index.showToast({ title: res.msg || "操作失败", icon: "none" });
      return;
    }
    common_vendor.index.showToast({ title: "已停止", icon: "success" });
    loadActivities();
  });
  const enableActivity = utils_withClickLock.withClickLock(async (item) => {
    if (!isManager.value) {
      common_vendor.index.showToast({ title: "仅店长可启用活动", icon: "none" });
      return;
    }
    const r = await showModalAsync({
      title: "启用活动",
      content: "启用后将按当前活动时间重新生效，确定启用吗？"
    });
    if (!r.confirm)
      return;
    const res = await api_modules_shopManage.enableManageActivity(item.activityId, shopId.value);
    if (!res.ok) {
      common_vendor.index.showToast({ title: res.msg || "操作失败", icon: "none" });
      return;
    }
    common_vendor.index.showToast({ title: "已启用", icon: "success" });
    loadActivities();
  });
  return {
    activities,
    needReloadActivities,
    loadActivities,
    goActivityEdit,
    editActivity,
    deleteActivity,
    stopActivity,
    enableActivity
  };
}
exports.useShopManageActivities = useShopManageActivities;
