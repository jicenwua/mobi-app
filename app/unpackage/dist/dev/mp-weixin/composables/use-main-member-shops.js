"use strict";
const common_vendor = require("../common/vendor.js");
const api_modules_shop = require("../api/modules/shop.js");
const api_modules_qrcode = require("../api/modules/qrcode.js");
const utils_qrcodeScan = require("../utils/qrcode-scan.js");
const utils_wxPerm = require("../utils/wx-perm.js");
function useMainMemberShops({ activeTab, canMemberSearch, showAddShopBtn, canMemberJoin }) {
  const memberList = common_vendor.ref([]);
  const memberPageNum = common_vendor.ref(1);
  const memberLoading = common_vendor.ref(false);
  const memberLoadingMore = common_vendor.ref(false);
  const memberFinished = common_vendor.ref(false);
  const memberLoadError = common_vendor.ref("");
  const memberSearchOpen = common_vendor.ref(false);
  const memberSearchKeyword = common_vendor.ref("");
  const memberSearchFocus = common_vendor.ref(false);
  const memberSearchLoading = common_vendor.ref(false);
  const memberEnterPreview = common_vendor.ref(null);
  const memberJoining = common_vendor.ref(false);
  const memberRefreshing = common_vendor.ref(false);
  let memberListLoaded = false;
  const memberDetailShopId = common_vendor.ref("");
  const memberDetailShop = common_vendor.ref(null);
  const memberEmptyText = common_vendor.computed(() => {
    if (memberLoadError.value)
      return memberLoadError.value;
    if (canMemberSearch.value)
      return "暂无已加入的店铺，可通过搜索店铺代码加入";
    if (canMemberJoin.value)
      return "暂无已加入的店铺，可通过扫码加入";
    return "暂无已加入的店铺";
  });
  const canReturnToMemberList = common_vendor.computed(() => {
    if (!memberDetailShopId.value)
      return false;
    if (memberList.value.length > 1)
      return true;
    return canMemberJoin.value;
  });
  const memberPreviewImages = common_vendor.computed(() => api_modules_shop.getShopCarouselImages(memberEnterPreview.value));
  const memberAlreadyJoined = common_vendor.computed(() => {
    var _a;
    const id = (_a = memberEnterPreview.value) == null ? void 0 : _a.shopId;
    if (!id)
      return false;
    return memberList.value.some((s) => s.id === id || s.shopId === id);
  });
  const memberJoinBtnText = common_vendor.computed(() => {
    if (memberJoining.value)
      return "加入中…";
    if (memberAlreadyJoined.value)
      return "已加入";
    return "加入店铺";
  });
  function invalidateMemberListCache() {
    memberListLoaded = false;
  }
  async function reloadMemberList(force = false) {
    if (!force && memberListLoaded)
      return;
    memberPageNum.value = 1;
    memberList.value = [];
    memberFinished.value = false;
    memberLoadError.value = "";
    await loadMemberPage(true);
    memberListLoaded = true;
  }
  async function onMemberRefresh() {
    memberRefreshing.value = true;
    invalidateMemberListCache();
    await reloadMemberList(true);
    memberRefreshing.value = false;
  }
  async function loadMemberPage(reset = false) {
    if (!utils_wxPerm.canShowMemberTab())
      return;
    if (memberLoading.value || memberLoadingMore.value)
      return;
    if (!reset && memberFinished.value)
      return;
    if (reset)
      memberLoading.value = true;
    else
      memberLoadingMore.value = true;
    const pageNum = reset ? 1 : memberPageNum.value;
    const result = await api_modules_shop.fetchMemberShops({ pageNum });
    if (reset)
      memberLoading.value = false;
    else
      memberLoadingMore.value = false;
    if (!result.ok) {
      if (reset)
        memberLoadError.value = result.msg || "加载失败";
      else
        common_vendor.index.showToast({ title: result.msg || "加载失败", icon: "none" });
      return;
    }
    memberLoadError.value = "";
    memberList.value = reset ? result.rows : [...memberList.value, ...result.rows];
    memberFinished.value = !result.hasMore;
    memberPageNum.value = pageNum + 1;
  }
  function loadMoreMemberShops() {
    if (activeTab.value !== "member" || memberEnterPreview.value || memberDetailShopId.value)
      return;
    loadMemberPage(false);
  }
  function openMemberDetail(shop) {
    const id = (shop == null ? void 0 : shop.id) ?? (shop == null ? void 0 : shop.shopId);
    if (!id) {
      common_vendor.index.showToast({ title: "店铺信息无效", icon: "none" });
      return;
    }
    api_modules_shop.reportShopEnterTimeAsync(id);
    memberDetailShopId.value = String(id);
    memberDetailShop.value = shop;
    closeMemberSearch();
  }
  function closeMemberDetail() {
    if (!canReturnToMemberList.value) {
      common_vendor.index.showToast({ title: "无返回列表权限", icon: "none" });
      return;
    }
    memberDetailShopId.value = "";
    memberDetailShop.value = null;
  }
  function tryAutoEnterSingleShop() {
    if (memberEnterPreview.value || memberDetailShopId.value)
      return;
    if (memberList.value.length !== 1)
      return;
    openMemberDetail(memberList.value[0]);
  }
  function openMemberDetailById(shopId) {
    const id = shopId ? String(shopId) : "";
    if (!id)
      return;
    const shop = memberList.value.find((s) => String(s.id) === id || String(s.shopId) === id);
    openMemberDetail(shop || { id });
  }
  function goAddShop() {
    if (!showAddShopBtn.value) {
      common_vendor.index.showToast({ title: "无添加店铺权限", icon: "none" });
      return;
    }
    try {
      common_vendor.index.preloadPage({ url: "/pages/shop/add" });
    } catch {
    }
    common_vendor.index.navigateTo({
      url: "/pages/shop/add",
      animationType: "slide-in-right",
      animationDuration: 200
    });
  }
  function openMemberSearch() {
    if (!canMemberSearch.value) {
      common_vendor.index.showToast({ title: "无搜索店铺权限", icon: "none" });
      return;
    }
    memberSearchOpen.value = true;
    memberSearchFocus.value = false;
    setTimeout(() => {
      memberSearchFocus.value = true;
    }, 50);
  }
  function closeMemberSearch() {
    memberSearchOpen.value = false;
    memberSearchKeyword.value = "";
    memberSearchFocus.value = false;
    memberEnterPreview.value = null;
    memberSearchLoading.value = false;
  }
  async function onMemberSearchConfirm() {
    if (!canMemberSearch.value) {
      common_vendor.index.showToast({ title: "无搜索店铺权限", icon: "none" });
      return;
    }
    const code = memberSearchKeyword.value.trim();
    if (!code) {
      common_vendor.index.showToast({ title: "请输入店铺代码", icon: "none" });
      return;
    }
    await previewShopByCode(code);
  }
  async function previewShopByCode(code) {
    const trimmed = (code || "").trim();
    if (!trimmed)
      return;
    common_vendor.index.hideKeyboard();
    memberSearchLoading.value = true;
    memberEnterPreview.value = null;
    const result = await api_modules_shop.fetchEnterShopByCode(trimmed);
    memberSearchLoading.value = false;
    if (!result.ok || !result.data) {
      common_vendor.index.showToast({ title: result.msg || "未找到店铺", icon: "none" });
      return;
    }
    activeTab.value = "member";
    memberSearchOpen.value = false;
    memberEnterPreview.value = result.data;
  }
  async function previewShopByInviteToken(token) {
    const trimmed = (token || "").trim();
    if (!trimmed)
      return;
    common_vendor.index.hideKeyboard();
    memberSearchLoading.value = true;
    memberEnterPreview.value = null;
    const result = await api_modules_qrcode.resolveShopInvite(trimmed);
    memberSearchLoading.value = false;
    if (!result.ok || !result.data) {
      common_vendor.index.showToast({ title: result.msg || "邀请码已过期或无效", icon: "none" });
      return;
    }
    activeTab.value = "member";
    memberSearchOpen.value = false;
    memberEnterPreview.value = result.data;
  }
  function scanShopCode() {
    if (!canMemberJoin.value) {
      common_vendor.index.showToast({ title: "无扫码加入权限", icon: "none" });
      return;
    }
    common_vendor.index.scanCode({
      onlyFromCamera: false,
      success: (res) => {
        const inviteToken = utils_qrcodeScan.parseShopInviteFromScan(res.result);
        if (inviteToken) {
          previewShopByInviteToken(inviteToken);
          return;
        }
        const code = utils_qrcodeScan.parseShopCodeFromScan(res.result);
        if (!code) {
          common_vendor.index.showToast({ title: "无效的店铺码", icon: "none" });
          return;
        }
        previewShopByCode(code);
      },
      fail: () => {
        common_vendor.index.showToast({ title: "扫码取消", icon: "none" });
      }
    });
  }
  async function handlePendingShopCode() {
    const inviteToken = utils_qrcodeScan.takePendingShopInvite();
    if (inviteToken) {
      if (canMemberSearch.value || canMemberJoin.value) {
        await previewShopByInviteToken(inviteToken);
      }
      return;
    }
    const code = utils_qrcodeScan.takePendingShopCode();
    if (code && canMemberSearch.value)
      await previewShopByCode(code);
  }
  function formatPreviewProductPoints(p) {
    const price = p == null ? void 0 : p.price;
    if (price == null || price === "")
      return "—";
    const points = Number(price);
    if (Number.isNaN(points))
      return "—";
    return Number.isInteger(points) ? String(points) : String(Math.round(points * 100) / 100);
  }
  async function onJoinShop() {
    var _a;
    if (!canMemberJoin.value) {
      common_vendor.index.showToast({ title: "无加入店铺权限", icon: "none" });
      return;
    }
    if (memberJoining.value || memberAlreadyJoined.value)
      return;
    const shopId = (_a = memberEnterPreview.value) == null ? void 0 : _a.shopId;
    if (!shopId) {
      common_vendor.index.showToast({ title: "店铺信息无效", icon: "none" });
      return;
    }
    memberJoining.value = true;
    const result = await api_modules_shop.joinShop(shopId);
    memberJoining.value = false;
    if (!result.ok) {
      common_vendor.index.showToast({ title: result.msg || "加入失败", icon: "none" });
      return;
    }
    common_vendor.index.showToast({ title: "加入成功", icon: "success" });
    closeMemberSearch();
    invalidateMemberListCache();
    await reloadMemberList(true);
    tryAutoEnterSingleShop();
  }
  common_vendor.watch(activeTab, (tab) => {
    if (tab !== "member")
      closeMemberSearch();
  });
  return {
    memberList,
    memberLoading,
    memberLoadingMore,
    memberFinished,
    memberLoadError,
    memberSearchOpen,
    memberSearchKeyword,
    memberSearchFocus,
    memberSearchLoading,
    memberEnterPreview,
    memberJoining,
    memberRefreshing,
    memberDetailShopId,
    memberDetailShop,
    memberEmptyText,
    memberPreviewImages,
    memberAlreadyJoined,
    memberJoinBtnText,
    canReturnToMemberList,
    invalidateMemberListCache,
    reloadMemberList,
    onMemberRefresh,
    loadMoreMemberShops,
    goAddShop,
    openMemberSearch,
    closeMemberSearch,
    onMemberSearchConfirm,
    scanShopCode,
    handlePendingShopCode,
    formatPreviewProductPoints,
    onJoinShop,
    openMemberDetail,
    closeMemberDetail,
    tryAutoEnterSingleShop,
    openMemberDetailById
  };
}
exports.useMainMemberShops = useMainMemberShops;
