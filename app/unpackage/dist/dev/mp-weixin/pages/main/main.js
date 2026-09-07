"use strict";
const common_vendor = require("../../common/vendor.js");
require("../../utils/crypto-gateway.js");
const api_modules_authToken = require("../../api/modules/auth-token.js");
const services_authRelogin = require("../../services/auth-relogin.js");
const services_notifySocket = require("../../services/notify-socket.js");
const services_appSession = require("../../services/app-session.js");
const services_userProfile = require("../../services/user-profile.js");
const api_modules_coupon = require("../../api/modules/coupon.js");
const api_modules_ticket = require("../../api/modules/ticket.js");
const composables_useTicketNotify = require("../../composables/use-ticket-notify.js");
const services_userSecurity = require("../../services/user-security.js");
const services_profileCache = require("../../services/profile-cache.js");
const utils_permissions = require("../../utils/permissions.js");
const utils_wxPerm = require("../../utils/wx-perm.js");
const utils_qrcodeScan = require("../../utils/qrcode-scan.js");
const composables_useTheme = require("../../composables/use-theme.js");
const composables_useMainMemberShops = require("../../composables/use-main-member-shops.js");
if (!Array) {
  const _easycom_uni_icons2 = common_vendor.resolveComponent("uni-icons");
  _easycom_uni_icons2();
}
const _easycom_uni_icons = () => "../../uni_modules/uni-icons/components/uni-icons/uni-icons.js";
if (!Math) {
  (_easycom_uni_icons + MemberShopDetailContent + PageLoading + ShopImageSwiper + ShopCard + MemberOrdersContent + MainMinePanel)();
}
const ShopCard = () => "../../components/shop/shop-card.js";
const ShopImageSwiper = () => "../../components/shop/shop-image-swiper.js";
const MemberShopDetailContent = () => "../../components/member/member-shop-detail-content.js";
const MemberOrdersContent = () => "../../components/member/member-orders-content.js";
const MainMinePanel = () => "../../components/main/main-mine-panel.js";
const PageLoading = () => "../../components/common/page-loading.js";
const NAV_INNER_HEIGHT = 44;
const LOGIN_PAGE = "/pages/login/login";
const PROFILE_SETUP_PAGE = "/pages/login/profile-setup";
const _sfc_main = {
  __name: "main",
  setup(__props) {
    const { isDark, loadTheme, toggleTheme } = composables_useTheme.useTheme();
    const ALL_TABS = [
      { key: "member", label: "会员", visible: () => utils_wxPerm.canShowMemberTab() },
      { key: "orders", label: "订单", visible: () => utils_wxPerm.canShowMemberTab() },
      { key: "mine", label: "我的", visible: () => true }
    ];
    const permRevision = utils_permissions.getPermRevision();
    const visibleTabs = common_vendor.computed(() => {
      permRevision.value;
      return ALL_TABS.filter((t) => t.visible());
    });
    const canMemberSearch = common_vendor.computed(() => {
      permRevision.value;
      return utils_wxPerm.canPreviewShopByCode();
    });
    const showAddShopBtn = common_vendor.computed(() => {
      permRevision.value;
      return utils_wxPerm.canAddShop();
    });
    const canMemberJoin = common_vendor.computed(() => {
      permRevision.value;
      return utils_wxPerm.canJoinShop();
    });
    const activeTab = common_vendor.ref("member");
    const memberOrdersRef = common_vendor.ref(null);
    const memberDetailRef = common_vendor.ref(null);
    let launchShopId = "";
    const statusBarHeight = common_vendor.ref(0);
    const navPaddingRight = common_vendor.ref(16);
    const loginError = common_vendor.ref("");
    const sessionBooting = common_vendor.ref(false);
    const userProfile = common_vendor.ref({ avatar: "", nickname: "" });
    const couponCount = common_vendor.ref(0);
    const ticketUnreadCount = common_vendor.ref(0);
    const payPasswordSet = common_vendor.ref(false);
    const {
      memberList,
      memberLoading,
      memberLoadingMore,
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
    } = composables_useMainMemberShops.useMainMemberShops({ activeTab, canMemberSearch, showAddShopBtn, canMemberJoin });
    const searchIconColor = common_vendor.computed(() => isDark.value ? "#b8b8b8" : "#666666");
    const loadingColor = common_vendor.computed(() => isDark.value ? "#5ac8fa" : "#007aff");
    const displayNickname = common_vendor.computed(() => {
      const name = (userProfile.value.nickname || "").trim();
      return name || "加载中…";
    });
    const profileSubText = common_vendor.computed(() => {
      if (userProfile.value.isDefault)
        return "默认昵称，点击可修改";
      return "点击编辑资料";
    });
    const payPasswordLabel = common_vendor.computed(() => payPasswordSet.value ? "已设置" : "未设置");
    const navBarStyle = common_vendor.computed(() => ({
      paddingTop: `${statusBarHeight.value || 0}px`
    }));
    const navInnerStyle = common_vendor.computed(() => ({
      paddingRight: `${navPaddingRight.value}px`
    }));
    const bodyPadStyle = common_vendor.computed(() => ({
      paddingTop: `${(statusBarHeight.value || 0) + NAV_INNER_HEIGHT}px`
    }));
    const tabBarSafeStyle = common_vendor.computed(() => {
      var _a;
      const bottom = ((_a = common_vendor.index.getSystemInfoSync().safeAreaInsets) == null ? void 0 : _a.bottom) || 0;
      return { paddingBottom: `${bottom}px` };
    });
    let defaultTabApplied = false;
    function resolveDefaultTab(keys) {
      if (keys.includes("member"))
        return "member";
      return keys[0] || "mine";
    }
    function ensureActiveTabValid() {
      const keys = visibleTabs.value.map((t) => t.key);
      if (!keys.includes(activeTab.value)) {
        activeTab.value = resolveDefaultTab(keys);
      }
    }
    let launchTabFromQuery = false;
    function applyDefaultTabOnLaunch() {
      if (defaultTabApplied)
        return;
      defaultTabApplied = true;
      if (launchTabFromQuery)
        return;
      const keys = visibleTabs.value.map((t) => t.key);
      if (keys.includes("member")) {
        activeTab.value = "member";
      } else if (!keys.includes(activeTab.value)) {
        activeTab.value = resolveDefaultTab(keys);
      }
    }
    function onTabClick(key) {
      activeTab.value = key;
      if (key === "orders") {
        setTimeout(() => {
          var _a, _b;
          return (_b = (_a = memberOrdersRef.value) == null ? void 0 : _a.refreshIfStale) == null ? void 0 : _b.call(_a);
        }, 0);
      }
    }
    async function refreshMineData() {
      userProfile.value = services_userProfile.getUserProfile();
      payPasswordSet.value = services_userSecurity.hasPayPasswordSet();
      ensureActiveTabValid();
      const couponRes = await api_modules_coupon.fetchUnusedCouponCount();
      if (couponRes.ok)
        couponCount.value = couponRes.count;
      const ticketRes = await api_modules_ticket.fetchTicketUnreadCount();
      if (ticketRes.ok)
        ticketUnreadCount.value = ticketRes.count;
    }
    common_vendor.watch(visibleTabs, () => {
      ensureActiveTabValid();
    });
    common_vendor.watch(permRevision, () => {
      ensureActiveTabValid();
      invalidateMemberListCache();
      if (activeTab.value === "member" && utils_wxPerm.canShowMemberTab()) {
        reloadMemberList(true).then(() => {
          if (!memberDetailShopId.value)
            tryAutoEnterSingleShop();
        });
      }
    });
    common_vendor.watch(memberDetailShopId, async (id) => {
      var _a, _b;
      if (!id)
        return;
      await common_vendor.nextTick$1();
      const shop = memberDetailShop.value;
      if (shop) {
        (_b = (_a = memberDetailRef.value) == null ? void 0 : _a.applyShop) == null ? void 0 : _b.call(_a, shop);
      }
    });
    common_vendor.watch(activeTab, (tab) => {
      if (tab !== "member") {
        closeMemberSearch();
      }
    });
    function goLoginPage() {
      common_vendor.index.redirectTo({ url: LOGIN_PAGE });
    }
    function goProfileSetupPage() {
      common_vendor.index.redirectTo({ url: PROFILE_SETUP_PAGE });
    }
    async function initSession() {
      loginError.value = "";
      sessionBooting.value = true;
      try {
        const { loginResult, needLogin } = await services_appSession.bootstrapAppSession();
        if (needLogin) {
          goLoginPage();
          return;
        }
        if (!services_userProfile.hasCompletedProfileSetup()) {
          goProfileSetupPage();
          return;
        }
        if (!loginResult.ok) {
          loginError.value = loginResult.msg || "登录失败，请稍后重试";
        }
        refreshMineData();
        applyDefaultTabOnLaunch();
        if (utils_wxPerm.canShowMemberTab()) {
          await reloadMemberList();
          if (launchShopId) {
            openMemberDetailById(launchShopId);
            launchShopId = "";
          } else {
            tryAutoEnterSingleShop();
          }
        }
        if (utils_wxPerm.canAddShop()) {
          try {
            common_vendor.index.preloadPage({ url: "/pages/shop/add" });
          } catch {
          }
        }
        await handlePendingShopCode();
        ensureActiveTabValid();
      } finally {
        sessionBooting.value = false;
      }
    }
    async function refreshTicketUnreadOnly() {
      const ticketRes = await api_modules_ticket.fetchTicketUnreadCount();
      if (ticketRes.ok)
        ticketUnreadCount.value = ticketRes.count;
    }
    const { subscribe: subscribeTicketNotify } = composables_useTicketNotify.useTicketNotify((payload) => {
      if ((payload == null ? void 0 : payload.type) === "unread_changed") {
        void refreshTicketUnreadOnly();
      }
    });
    common_vendor.onLoad((options) => {
      const launchInvite = utils_qrcodeScan.parseLaunchShopInvite(options);
      if (launchInvite == null ? void 0 : launchInvite.token) {
        utils_qrcodeScan.savePendingShopInvite(launchInvite.token);
      } else if (launchInvite == null ? void 0 : launchInvite.shopCode) {
        utils_qrcodeScan.savePendingShopCode(launchInvite.shopCode);
      }
      if ((options == null ? void 0 : options.tab) === "orders" && utils_wxPerm.canShowMemberTab()) {
        activeTab.value = "orders";
        launchTabFromQuery = true;
      } else if ((options == null ? void 0 : options.tab) === "mine") {
        activeTab.value = "mine";
        launchTabFromQuery = true;
      }
      if (options == null ? void 0 : options.shopId) {
        launchShopId = String(options.shopId);
      }
      const sys = common_vendor.index.getSystemInfoSync();
      statusBarHeight.value = sys.statusBarHeight || 20;
      try {
        const menu = common_vendor.index.getMenuButtonBoundingClientRect();
        if (menu && menu.left > 0) {
          navPaddingRight.value = Math.max(16, sys.windowWidth - menu.left + 8);
        }
      } catch {
        navPaddingRight.value = 96;
      }
      loadTheme();
      initSession();
      subscribeTicketNotify();
    });
    common_vendor.onShow(async () => {
      var _a, _b, _c, _d;
      if (!api_modules_authToken.getToken()) {
        const auth = await services_authRelogin.ensureAuthenticated();
        if (!auth.ok)
          return;
      }
      services_notifySocket.connectNotifySocket();
      userProfile.value = services_userProfile.getUserProfile();
      payPasswordSet.value = services_userSecurity.hasPayPasswordSet();
      ensureActiveTabValid();
      if (activeTab.value === "mine") {
        const couponRes = await api_modules_coupon.fetchUnusedCouponCount();
        if (couponRes.ok)
          couponCount.value = couponRes.count;
      }
      if (activeTab.value === "orders") {
        (_b = (_a = memberOrdersRef.value) == null ? void 0 : _a.refreshIfStale) == null ? void 0 : _b.call(_a);
      }
      if (activeTab.value === "member" && memberDetailShopId.value) {
        (_d = (_c = memberDetailRef.value) == null ? void 0 : _c.syncCartFromCache) == null ? void 0 : _d.call(_c);
      }
    });
    function goProfile() {
      services_profileCache.setProfileEditCache(services_userProfile.getUserProfile());
      common_vendor.index.navigateTo({
        url: "/pages/mine/profile",
        animationType: "slide-in-right",
        animationDuration: 200
      });
    }
    function goSetPassword() {
      common_vendor.index.navigateTo({
        url: "/pages/mine/set-password",
        animationType: "slide-in-right",
        animationDuration: 200
      });
    }
    function goCoupons() {
      common_vendor.index.navigateTo({
        url: "/pages/mine/coupons",
        animationType: "slide-in-right",
        animationDuration: 200
      });
    }
    function goTickets() {
      common_vendor.index.navigateTo({
        url: "/pages/mine/tickets",
        animationType: "slide-in-right",
        animationDuration: 200
      });
    }
    function goPayQrcode() {
      if (!utils_wxPerm.canGeneratePayQrcode()) {
        common_vendor.index.showToast({ title: "无付款码权限", icon: "none" });
        return;
      }
      common_vendor.index.navigateTo({
        url: "/pages/member/pay-qrcode",
        animationType: "slide-in-right",
        animationDuration: 200
      });
    }
    return (_ctx, _cache) => {
      var _a, _b;
      return common_vendor.e({
        a: activeTab.value === "member" && common_vendor.unref(memberDetailShopId) && common_vendor.unref(canReturnToMemberList)
      }, activeTab.value === "member" && common_vendor.unref(memberDetailShopId) && common_vendor.unref(canReturnToMemberList) ? {
        b: common_vendor.o((...args) => common_vendor.unref(closeMemberDetail) && common_vendor.unref(closeMemberDetail)(...args), "33")
      } : {
        c: common_vendor.t(common_vendor.unref(isDark) ? "☽" : "☀"),
        d: common_vendor.n(common_vendor.unref(isDark) ? "theme-glyph--moon" : "theme-glyph--sun"),
        e: common_vendor.o((...args) => common_vendor.unref(toggleTheme) && common_vendor.unref(toggleTheme)(...args), "1b")
      }, {
        f: common_vendor.s(navInnerStyle.value),
        g: common_vendor.s(navBarStyle.value),
        h: activeTab.value === "member"
      }, activeTab.value === "member" ? common_vendor.e({
        i: !common_vendor.unref(memberDetailShopId) && (canMemberSearch.value || canMemberJoin.value || showAddShopBtn.value)
      }, !common_vendor.unref(memberDetailShopId) && (canMemberSearch.value || canMemberJoin.value || showAddShopBtn.value) ? common_vendor.e({
        j: showAddShopBtn.value && !common_vendor.unref(memberSearchOpen)
      }, showAddShopBtn.value && !common_vendor.unref(memberSearchOpen) ? {
        k: common_vendor.o((...args) => common_vendor.unref(goAddShop) && common_vendor.unref(goAddShop)(...args), "0d")
      } : {}, {
        l: canMemberSearch.value && common_vendor.unref(memberSearchOpen)
      }, canMemberSearch.value && common_vendor.unref(memberSearchOpen) ? {
        m: common_vendor.p({
          type: "search",
          size: 18,
          color: searchIconColor.value
        }),
        n: common_vendor.unref(memberSearchFocus),
        o: common_vendor.o((...args) => common_vendor.unref(onMemberSearchConfirm) && common_vendor.unref(onMemberSearchConfirm)(...args), "d5"),
        p: common_vendor.unref(memberSearchKeyword),
        q: common_vendor.o(($event) => common_vendor.isRef(memberSearchKeyword) ? memberSearchKeyword.value = $event.detail.value : null, "5e"),
        r: common_vendor.o((...args) => common_vendor.unref(closeMemberSearch) && common_vendor.unref(closeMemberSearch)(...args), "70")
      } : canMemberSearch.value ? {
        t: common_vendor.p({
          type: "search",
          size: 22,
          color: searchIconColor.value
        }),
        v: common_vendor.o((...args) => common_vendor.unref(openMemberSearch) && common_vendor.unref(openMemberSearch)(...args), "f8")
      } : {}, {
        s: canMemberSearch.value,
        w: canMemberJoin.value && !common_vendor.unref(memberSearchOpen)
      }, canMemberJoin.value && !common_vendor.unref(memberSearchOpen) ? {
        x: common_vendor.p({
          type: "scan",
          size: 22,
          color: searchIconColor.value
        }),
        y: common_vendor.o((...args) => common_vendor.unref(scanShopCode) && common_vendor.unref(scanShopCode)(...args), "75")
      } : {}) : {}, {
        z: common_vendor.unref(memberDetailShopId)
      }, common_vendor.unref(memberDetailShopId) ? {
        A: common_vendor.sr(memberDetailRef, "4f50ca8f-3", {
          "k": "memberDetailRef"
        }),
        B: common_vendor.p({
          ["shop-id"]: common_vendor.unref(memberDetailShopId),
          ["is-dark"]: common_vendor.unref(isDark),
          embedded: true
        })
      } : common_vendor.e({
        C: common_vendor.unref(memberSearchLoading)
      }, common_vendor.unref(memberSearchLoading) ? {
        D: common_vendor.p({
          text: "查询店铺中…",
          color: loadingColor.value
        })
      } : common_vendor.unref(memberEnterPreview) ? common_vendor.e({
        F: common_vendor.p({
          images: common_vendor.unref(memberPreviewImages),
          ["is-dark"]: common_vendor.unref(isDark)
        }),
        G: common_vendor.t(common_vendor.unref(memberEnterPreview).shopName || "未命名店铺"),
        H: common_vendor.t(common_vendor.unref(memberEnterPreview).phone || "未填写"),
        I: common_vendor.unref(memberEnterPreview).ratio != null
      }, common_vendor.unref(memberEnterPreview).ratio != null ? {
        J: common_vendor.t(common_vendor.unref(memberEnterPreview).ratio)
      } : {}, {
        K: common_vendor.t(common_vendor.unref(memberEnterPreview).address || "地址未填写"),
        L: (_a = common_vendor.unref(memberEnterPreview).products) == null ? void 0 : _a.length
      }, ((_b = common_vendor.unref(memberEnterPreview).products) == null ? void 0 : _b.length) ? {
        M: common_vendor.f(common_vendor.unref(memberEnterPreview).products, (p, k0, i0) => {
          return {
            a: common_vendor.t(p.productName),
            b: common_vendor.t(common_vendor.unref(formatPreviewProductPoints)(p)),
            c: p.productId
          };
        })
      } : {}, {
        N: canMemberJoin.value
      }, canMemberJoin.value ? {
        O: common_vendor.t(common_vendor.unref(memberJoinBtnText)),
        P: common_vendor.unref(memberAlreadyJoined) || common_vendor.unref(memberJoining) ? 1 : "",
        Q: common_vendor.unref(memberAlreadyJoined) || common_vendor.unref(memberJoining) ? "none" : "tap-hover-opacity-mid",
        R: common_vendor.o((...args) => common_vendor.unref(onJoinShop) && common_vendor.unref(onJoinShop)(...args), "53")
      } : {}) : common_vendor.e({
        S: common_vendor.unref(memberLoading) && !common_vendor.unref(memberList).length
      }, common_vendor.unref(memberLoading) && !common_vendor.unref(memberList).length ? {
        T: common_vendor.p({
          text: "加载店铺中…",
          color: loadingColor.value
        })
      } : !common_vendor.unref(memberList).length ? {
        V: common_vendor.t(common_vendor.unref(memberEmptyText))
      } : common_vendor.e({
        W: common_vendor.f(common_vendor.unref(memberList), (item, k0, i0) => {
          return {
            a: item.id,
            b: common_vendor.o(common_vendor.unref(openMemberDetail), item.id),
            c: "4f50ca8f-7-" + i0,
            d: common_vendor.p({
              shop: item,
              ["is-dark"]: common_vendor.unref(isDark),
              ["show-points"]: true,
              ["embed-detail"]: true
            })
          };
        }),
        X: common_vendor.unref(memberLoadingMore)
      }, common_vendor.unref(memberLoadingMore) ? {
        Y: common_vendor.p({
          text: "加载更多…",
          compact: true,
          color: loadingColor.value
        })
      } : {}), {
        U: !common_vendor.unref(memberList).length
      }), {
        E: common_vendor.unref(memberEnterPreview),
        Z: common_vendor.n(canMemberSearch.value || canMemberJoin.value || showAddShopBtn.value ? "shop-scroll--with-toolbar" : ""),
        aa: common_vendor.unref(memberRefreshing),
        ab: common_vendor.o((...args) => common_vendor.unref(onMemberRefresh) && common_vendor.unref(onMemberRefresh)(...args), "e4"),
        ac: common_vendor.o((...args) => common_vendor.unref(loadMoreMemberShops) && common_vendor.unref(loadMoreMemberShops)(...args), "c8")
      })) : {}, {
        ad: activeTab.value === "orders"
      }, activeTab.value === "orders" ? {
        ae: common_vendor.sr(memberOrdersRef, "4f50ca8f-9", {
          "k": "memberOrdersRef"
        }),
        af: common_vendor.p({
          ["is-dark"]: common_vendor.unref(isDark)
        })
      } : {}, {
        ag: activeTab.value === "mine"
      }, activeTab.value === "mine" ? {
        ah: common_vendor.o(goProfile, "73"),
        ai: common_vendor.o(goCoupons, "99"),
        aj: common_vendor.o(goPayQrcode, "2c"),
        ak: common_vendor.o(goSetPassword, "98"),
        al: common_vendor.o(goTickets, "3a"),
        am: common_vendor.p({
          ["user-profile"]: userProfile.value,
          ["display-nickname"]: displayNickname.value,
          ["profile-sub-text"]: profileSubText.value,
          ["coupon-count"]: couponCount.value,
          ["pay-password-label"]: payPasswordLabel.value,
          ["ticket-unread-count"]: ticketUnreadCount.value,
          ["login-error"]: loginError.value,
          ["is-dark"]: common_vendor.unref(isDark)
        })
      } : {}, {
        an: activeTab.value === "member" && common_vendor.unref(memberDetailShopId) ? 1 : "",
        ao: activeTab.value === "orders" ? 1 : "",
        ap: common_vendor.s(bodyPadStyle.value),
        aq: common_vendor.f(visibleTabs.value, (item, k0, i0) => {
          return {
            a: common_vendor.t(item.label),
            b: item.key,
            c: activeTab.value === item.key ? 1 : "",
            d: common_vendor.o(($event) => onTabClick(item.key), item.key)
          };
        }),
        ar: common_vendor.s(tabBarSafeStyle.value),
        as: sessionBooting.value
      }, sessionBooting.value ? {
        at: common_vendor.p({
          text: "正在进入…",
          overlay: true,
          color: loadingColor.value,
          ["overlay-bg"]: common_vendor.unref(isDark) ? "rgba(18, 18, 18, 0.78)" : "rgba(255, 255, 255, 0.72)"
        })
      } : {}, {
        av: common_vendor.n(common_vendor.unref(isDark) ? "theme-dark" : "theme-light")
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-4f50ca8f"]]);
wx.createPage(MiniProgramPage);
