"use strict";
const common_vendor = require("../common/vendor.js");
const api_modules_shop = require("../api/modules/shop.js");
const utils_shopRole = require("../utils/shop-role.js");
const utils_activityPopupSession = require("../utils/activity-popup-session.js");
const utils_couponBannerSession = require("../utils/coupon-banner-session.js");
function useMemberShopPromo(shopId, detail) {
  const activityModalVisible = common_vendor.ref(false);
  const activityIndex = common_vendor.ref(0);
  const couponModalVisible = common_vendor.ref(false);
  const couponClaiming = common_vendor.ref(false);
  const claimedCouponIds = common_vendor.ref([]);
  const activeActivities = common_vendor.computed(() => {
    var _a;
    const list = (_a = detail.value) == null ? void 0 : _a.activities;
    return Array.isArray(list) ? list.filter((item) => item && item.kind !== 2) : [];
  });
  const announcement = common_vendor.computed(() => {
    var _a;
    const ann = (_a = detail.value) == null ? void 0 : _a.announcement;
    return ann && typeof ann === "object" ? ann : null;
  });
  const promoItems = common_vendor.computed(() => {
    const items = activeActivities.value.map((data) => ({ type: "activity", data }));
    if (announcement.value) {
      items.push({ type: "announcement", data: announcement.value });
    }
    return items;
  });
  const currentPromoItem = common_vendor.computed(() => promoItems.value[activityIndex.value] || null);
  const currentPromoTitle = common_vendor.computed(() => {
    var _a;
    const item = currentPromoItem.value;
    if (!item)
      return "";
    if (item.type === "announcement") {
      return item.data.activityName || ((_a = item.data.description) == null ? void 0 : _a.slice(0, 20)) || "店铺公告";
    }
    return item.data.activityName || "未命名活动";
  });
  const currentPromoDescription = common_vendor.computed(() => {
    var _a, _b;
    const desc = (_b = (_a = currentPromoItem.value) == null ? void 0 : _a.data) == null ? void 0 : _b.description;
    return desc ? String(desc).trim() : "";
  });
  const currentPromoRules = common_vendor.computed(() => {
    var _a, _b;
    const rules = ((_a = currentPromoItem.value) == null ? void 0 : _a.type) === "activity" ? (_b = currentPromoItem.value.data) == null ? void 0 : _b.rules : [];
    return Array.isArray(rules) ? rules : [];
  });
  const availableCoupons = common_vendor.computed(() => {
    var _a;
    const list = (_a = detail.value) == null ? void 0 : _a.coupons;
    if (!Array.isArray(list))
      return [];
    const claimed = new Set(claimedCouponIds.value);
    return list.filter((item) => {
      if (!(item == null ? void 0 : item.templateId) || claimed.has(item.templateId))
        return false;
      if (item.remainingQuantity != null && item.remainingQuantity !== "") {
        return Number(item.remainingQuantity) > 0;
      }
      const total = item.totalQuantity;
      if (total == null || total === "")
        return true;
      const issued = item.issuedQuantity ?? 0;
      return total > 0 && issued < total;
    });
  });
  function resetPromo() {
    activityModalVisible.value = false;
    activityIndex.value = 0;
    couponModalVisible.value = false;
    couponClaiming.value = false;
    claimedCouponIds.value = [];
  }
  function showActivityModalIfNeeded() {
    const list = promoItems.value;
    if (!list.length) {
      activityModalVisible.value = false;
      return;
    }
    const shopKey = shopId.value;
    if (utils_activityPopupSession.hasShownActivityPopup(shopKey)) {
      activityModalVisible.value = false;
      return;
    }
    activityIndex.value = 0;
    activityModalVisible.value = true;
  }
  function showCouponModalIfNeeded() {
    if (activityModalVisible.value)
      return;
    const list = availableCoupons.value;
    if (!list.length) {
      couponModalVisible.value = false;
      return;
    }
    if (utils_couponBannerSession.hasDismissedCouponBanner(shopId.value)) {
      couponModalVisible.value = false;
      return;
    }
    couponModalVisible.value = true;
  }
  function closeActivityModal() {
    activityModalVisible.value = false;
    utils_activityPopupSession.markActivityPopupShown(shopId.value);
    showCouponModalIfNeeded();
  }
  function closeCouponModal() {
    couponModalVisible.value = false;
    utils_couponBannerSession.markCouponBannerDismissed(shopId.value);
  }
  function prevPromoItem() {
    const len = promoItems.value.length;
    if (len <= 1)
      return;
    activityIndex.value = (activityIndex.value - 1 + len) % len;
  }
  function nextPromoItem() {
    const len = promoItems.value.length;
    if (len <= 1)
      return;
    activityIndex.value = (activityIndex.value + 1) % len;
  }
  function isCouponAlreadyClaimedMsg(msg) {
    const text = String(msg || "");
    return text.includes("已领取") || text.includes("您已领取");
  }
  function handleCouponClaimFailure(coupon, msg) {
    if ((coupon == null ? void 0 : coupon.templateId) && isCouponAlreadyClaimedMsg(msg)) {
      claimedCouponIds.value = claimedCouponIds.value.concat(coupon.templateId);
    }
    if (!availableCoupons.value.length) {
      utils_couponBannerSession.markCouponBannerDismissed(shopId.value);
      couponModalVisible.value = false;
    }
    common_vendor.index.showToast({ title: msg || "领取失败", icon: "none" });
  }
  async function claimCoupon(coupon) {
    if (!(coupon == null ? void 0 : coupon.templateId) || couponClaiming.value)
      return;
    couponClaiming.value = true;
    const res = await api_modules_shop.claimShopCoupon(coupon.templateId, shopId.value);
    couponClaiming.value = false;
    if (!res.ok) {
      handleCouponClaimFailure(coupon, res.msg);
      return;
    }
    claimedCouponIds.value = claimedCouponIds.value.concat(coupon.templateId);
    common_vendor.index.showToast({ title: res.msg || "领取成功", icon: "success" });
    if (!availableCoupons.value.length) {
      closeCouponModal();
    }
  }
  async function claimAllCoupons() {
    const list = availableCoupons.value;
    if (!list.length || couponClaiming.value)
      return;
    couponClaiming.value = true;
    let successCount = 0;
    let lastMsg = "";
    for (const coupon of list) {
      const res = await api_modules_shop.claimShopCoupon(coupon.templateId, shopId.value);
      if (res.ok) {
        successCount += 1;
        claimedCouponIds.value = claimedCouponIds.value.concat(coupon.templateId);
        lastMsg = res.msg || "领取成功";
      } else {
        lastMsg = res.msg || "领取失败";
        if (isCouponAlreadyClaimedMsg(lastMsg)) {
          claimedCouponIds.value = claimedCouponIds.value.concat(coupon.templateId);
        }
      }
    }
    couponClaiming.value = false;
    if (successCount > 0) {
      common_vendor.index.showToast({
        title: successCount === list.length ? lastMsg || "领取成功" : `已领取 ${successCount} 张`,
        icon: "success"
      });
    } else {
      common_vendor.index.showToast({ title: lastMsg || "领取失败", icon: "none" });
    }
    if (!availableCoupons.value.length) {
      if (successCount === 0 && isCouponAlreadyClaimedMsg(lastMsg)) {
        utils_couponBannerSession.markCouponBannerDismissed(shopId.value);
        couponModalVisible.value = false;
      } else {
        closeCouponModal();
      }
    }
  }
  function shouldIncludeCouponsInExtras() {
    return !utils_shopRole.isShopManager(detail.value);
  }
  return {
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
    resetPromo,
    showActivityModalIfNeeded,
    showCouponModalIfNeeded,
    closeActivityModal,
    closeCouponModal,
    prevPromoItem,
    nextPromoItem,
    claimCoupon,
    claimAllCoupons,
    shouldIncludeCouponsInExtras
  };
}
exports.useMemberShopPromo = useMemberShopPromo;
