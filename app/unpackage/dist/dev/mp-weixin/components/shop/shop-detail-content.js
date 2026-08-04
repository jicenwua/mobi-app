"use strict";
const common_vendor = require("../../common/vendor.js");
const composables_useExpiringQrcode = require("../../composables/use-expiring-qrcode.js");
const api_modules_points = require("../../api/modules/points.js");
const api_modules_shop = require("../../api/modules/shop.js");
const api_modules_qrcode = require("../../api/modules/qrcode.js");
const utils_datetimeFormat = require("../../utils/datetime-format.js");
const utils_pointsLogStatus = require("../../utils/points-log-status.js");
const utils_shopRole = require("../../utils/shop-role.js");
const utils_wxPerm = require("../../utils/wx-perm.js");
const utils_staffScan = require("../../utils/staff-scan.js");
const utils_shopPageContext = require("../../utils/shop-page-context.js");
const utils_shopDetailCache = require("../../utils/shop-detail-cache.js");
if (!Math) {
  (ShopImageSwiper + QrcodeCanvas)();
}
const ShopImageSwiper = () => "./shop-image-swiper.js";
const QrcodeCanvas = () => "../common/qrcode-canvas.js";
const pageSize = 10;
const _sfc_main = {
  __name: "shop-detail-content",
  props: {
    shopId: { type: [String, Number], default: "" },
    /** 列表项传入的店铺信息（从卡片跳转） */
    initialShop: { type: Object, default: null },
    isDark: { type: Boolean, default: false }
  },
  setup(__props, { expose: __expose }) {
    const props = __props;
    const shop = common_vendor.ref(null);
    const loading = common_vendor.ref(true);
    const errorMsg = common_vendor.ref("");
    const activeTab = common_vendor.ref("points");
    const sortType = common_vendor.ref(1);
    const keywordNickname = common_vendor.ref("");
    const keywordPhone = common_vendor.ref("");
    const listRows = common_vendor.ref([]);
    const listLoading = common_vendor.ref(false);
    const listLoadingMore = common_vendor.ref(false);
    const listFinished = common_vendor.ref(false);
    const pageNum = common_vendor.ref(1);
    const detailVisible = common_vendor.ref(false);
    const logDetail = common_vendor.ref(null);
    const qrcodeVisible = common_vendor.ref(false);
    const qrcodeLoading = common_vendor.ref(false);
    const shopQrcodeImage = common_vendor.ref("");
    const shopQrcodeFallback = common_vendor.ref("");
    const shopQrcodeExpireAt = common_vendor.ref(0);
    let shopLoadToken = 0;
    const pointsListLoaded = common_vendor.ref(false);
    const logsListLoaded = common_vendor.ref(false);
    const resolvedShopId = common_vendor.computed(() => {
      var _a;
      const fromProp = props.shopId != null && props.shopId !== "" ? String(props.shopId) : "";
      if (fromProp)
        return fromProp;
      const fromShop = (_a = shop.value) == null ? void 0 : _a.id;
      return fromShop != null && fromShop !== "" ? String(fromShop) : "";
    });
    const qrcodeExpireHint = common_vendor.computed(() => {
      if (!qrcodeCountdownLeft.value)
        return "";
      return `${qrcodeCountdownLeft.value} 秒后自动刷新`;
    });
    async function refreshShopQrcode() {
      var _a, _b, _c;
      const id = resolvedShopId.value;
      if (!id || !qrcodeVisible.value)
        return;
      qrcodeLoading.value = true;
      shopQrcodeImage.value = "";
      shopQrcodeFallback.value = "";
      stopQrcodeCountdown();
      qrcodeCountdownLeft.value = 0;
      const res = await api_modules_qrcode.fetchShopQrcode(id);
      qrcodeLoading.value = false;
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "生成失败", icon: "none" });
        return;
      }
      if ((_a = res.data) == null ? void 0 : _a.imageBase64) {
        shopQrcodeImage.value = res.data.imageBase64;
      }
      if ((_b = res.data) == null ? void 0 : _b.inviteContent) {
        shopQrcodeFallback.value = res.data.inviteContent;
      }
      shopQrcodeExpireAt.value = ((_c = res.data) == null ? void 0 : _c.expireAt) || Date.now() + 12e4;
      syncQrcodeCountdownFromExpireAt(shopQrcodeExpireAt.value);
    }
    const {
      countdownLeft: qrcodeCountdownLeft,
      stopCountdown: stopQrcodeCountdown,
      syncFromExpireAt: syncQrcodeCountdownFromExpireAt
    } = composables_useExpiringQrcode.useExpiringQrcode(() => {
      if (qrcodeVisible.value) {
        void refreshShopQrcode();
      }
    });
    const carouselImages = common_vendor.computed(() => api_modules_shop.getShopCarouselImages(shop.value));
    const showManageEntry = common_vendor.computed(
      () => utils_shopRole.isShopClerkCapable(shop.value) && utils_wxPerm.canManageShop()
    );
    const canShowQrcode = common_vendor.computed(() => utils_wxPerm.canPreviewShopByCode());
    const canCheckout = common_vendor.computed(() => utils_wxPerm.canStaffVerifyAtShop(shop.value));
    function goManage() {
      const id = resolvedShopId.value;
      if (!id)
        return;
      utils_shopPageContext.navigateWithShop({
        url: `/pages/shop/manage?id=${id}`,
        shop: shop.value
      });
    }
    function goScanCheckout() {
      const id = resolvedShopId.value;
      if (!id)
        return;
      utils_staffScan.scanStaffVerifyCode({ shopId: id });
    }
    async function openShopQrcode() {
      const id = resolvedShopId.value;
      if (!id)
        return;
      qrcodeVisible.value = true;
      await refreshShopQrcode();
    }
    function closeShopQrcode() {
      qrcodeVisible.value = false;
      stopQrcodeCountdown();
      qrcodeCountdownLeft.value = 0;
    }
    common_vendor.watch(
      () => {
        var _a;
        return [props.shopId, (_a = props.initialShop) == null ? void 0 : _a.id];
      },
      () => {
        syncShopFromProps();
      },
      { immediate: true }
    );
    common_vendor.watch(activeTab, (tab) => {
      if (!shop.value)
        return;
      if (tab === "points" && !pointsListLoaded.value && !listLoading.value) {
        reloadList();
      }
      if (tab === "log" && !logsListLoaded.value && !listLoading.value) {
        reloadList();
      }
    });
    function syncShopFromProps() {
      if (!utils_wxPerm.canShowShopTab()) {
        errorMsg.value = "无店铺访问权限";
        loading.value = false;
        shop.value = null;
        return;
      }
      const data = props.initialShop;
      if (data == null ? void 0 : data.id) {
        applyShop(data);
        return;
      }
      const id = props.shopId != null && props.shopId !== "" ? String(props.shopId) : "";
      if (!id) {
        loading.value = false;
        errorMsg.value = "店铺信息无效";
        shop.value = null;
        return;
      }
      const cached = utils_shopDetailCache.getCachedShopDetail(id);
      if (cached) {
        applyShop(cached);
        return;
      }
      loading.value = true;
      errorMsg.value = "";
      shop.value = null;
      loadShopFallback();
    }
    async function loadShopFallback() {
      const token = ++shopLoadToken;
      const id = props.shopId != null && props.shopId !== "" ? String(props.shopId) : "";
      if (!id)
        return;
      loading.value = true;
      errorMsg.value = "";
      const res = await api_modules_shop.fetchShopFromUserList(id);
      if (token !== shopLoadToken)
        return;
      if (!res.ok || !res.data) {
        loading.value = false;
        errorMsg.value = res.msg || "加载失败";
        shop.value = null;
        return;
      }
      applyShop(res.data);
    }
    function applyShop(data) {
      shopLoadToken += 1;
      const normalized = api_modules_shop.normalizeShopDetail(data);
      if (!(normalized == null ? void 0 : normalized.id)) {
        errorMsg.value = "店铺信息无效";
        loading.value = false;
        shop.value = null;
        return;
      }
      activeTab.value = "points";
      keywordNickname.value = "";
      keywordPhone.value = "";
      sortType.value = 1;
      shop.value = normalized;
      utils_shopPageContext.rememberShopDetail(normalized);
      loading.value = false;
      errorMsg.value = "";
      reloadList();
    }
    function setSort(type) {
      if (sortType.value === type)
        return;
      sortType.value = type;
      reloadList();
    }
    function reloadList() {
      pageNum.value = 1;
      listRows.value = [];
      listFinished.value = false;
      if (activeTab.value === "points") {
        pointsListLoaded.value = false;
      } else {
        logsListLoaded.value = false;
      }
      loadList(false);
    }
    async function loadList(more) {
      const id = resolvedShopId.value;
      if (!id || listLoading.value || listLoadingMore.value)
        return;
      if (more && listFinished.value)
        return;
      if (more)
        listLoadingMore.value = true;
      else
        listLoading.value = true;
      const params = {
        shopId: Number(id),
        pageNum: pageNum.value,
        pageSize,
        nickname: keywordNickname.value.trim() || void 0,
        phone: keywordPhone.value.trim() || void 0
      };
      if (activeTab.value === "points") {
        params.sortType = sortType.value;
      }
      const fetcher = activeTab.value === "points" ? api_modules_points.fetchShopPointsAccountList : api_modules_points.fetchShopConsumeLogList;
      const res = await fetcher(params);
      if (more)
        listLoadingMore.value = false;
      else
        listLoading.value = false;
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "加载失败", icon: "none" });
        return;
      }
      const rows = res.rows || [];
      if (more) {
        listRows.value = listRows.value.concat(rows);
      } else {
        listRows.value = rows;
        if (activeTab.value === "points") {
          pointsListLoaded.value = true;
        } else {
          logsListLoaded.value = true;
        }
      }
      listFinished.value = rows.length < pageSize;
    }
    function loadMore() {
      if (listFinished.value || listLoading.value || listLoadingMore.value)
        return;
      pageNum.value += 1;
      loadList(true);
    }
    function actionTypeLabel(type) {
      if (type === 1)
        return "增加";
      if (type === 2)
        return "消耗";
      return "—";
    }
    function statusLabel(status) {
      return utils_pointsLogStatus.pointsLogStatusLabel(status);
    }
    function formatDate(iso) {
      if (!iso)
        return "—";
      const s = String(iso).replace("T", " ");
      return s.length >= 10 ? s.slice(0, 10) : s;
    }
    async function openLogDetail(row) {
      const id = resolvedShopId.value;
      if (!(row == null ? void 0 : row.logId) || !id)
        return;
      detailVisible.value = true;
      logDetail.value = row;
      const res = await api_modules_points.fetchConsumeLogDetail(row.logId, Number(id));
      if (res.ok && res.data) {
        logDetail.value = res.data;
      } else if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "加载详情失败", icon: "none" });
      }
    }
    __expose({ applyShop });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: loading.value
      }, loading.value ? {} : shop.value ? common_vendor.e({
        c: common_vendor.p({
          images: carouselImages.value,
          ["is-dark"]: __props.isDark,
          height: "200px"
        }),
        d: common_vendor.t(shop.value.shopName || "未命名店铺"),
        e: canShowQrcode.value
      }, canShowQrcode.value ? {
        f: common_vendor.o(openShopQrcode, "51")
      } : {}, {
        g: canCheckout.value
      }, canCheckout.value ? {
        h: common_vendor.o(goScanCheckout, "c8")
      } : {}, {
        i: showManageEntry.value
      }, showManageEntry.value ? {
        j: common_vendor.o(goManage, "3d")
      } : {}, {
        k: activeTab.value === "points" ? 1 : "",
        l: common_vendor.o(($event) => activeTab.value = "points", "7f"),
        m: activeTab.value === "log" ? 1 : "",
        n: common_vendor.o(($event) => activeTab.value = "log", "ad"),
        o: common_vendor.n(activeTab.value === "log" ? "tab-slider--right" : ""),
        p: common_vendor.o(reloadList, "b2"),
        q: keywordNickname.value,
        r: common_vendor.o(($event) => keywordNickname.value = $event.detail.value, "4d"),
        s: common_vendor.o(reloadList, "16"),
        t: keywordPhone.value,
        v: common_vendor.o(($event) => keywordPhone.value = $event.detail.value, "c7"),
        w: common_vendor.o(reloadList, "69"),
        x: activeTab.value === "points"
      }, activeTab.value === "points" ? {
        y: sortType.value === 1 ? 1 : "",
        z: common_vendor.o(($event) => setSort(1), "50"),
        A: sortType.value === 2 ? 1 : "",
        B: common_vendor.o(($event) => setSort(2), "5f"),
        C: sortType.value === 3 ? 1 : "",
        D: common_vendor.o(($event) => setSort(3), "d5"),
        E: sortType.value === 4 ? 1 : "",
        F: common_vendor.o(($event) => setSort(4), "e2")
      } : {}, {
        G: listLoading.value && !listRows.value.length
      }, listLoading.value && !listRows.value.length ? {} : !listRows.value.length ? {} : common_vendor.e({
        I: activeTab.value === "points"
      }, activeTab.value === "points" ? {
        J: common_vendor.f(listRows.value, (row, k0, i0) => {
          return {
            a: common_vendor.t(row.nickname || "—"),
            b: common_vendor.t(row.remainingPoints ?? 0),
            c: common_vendor.t(row.basePoints ?? 0),
            d: common_vendor.t(row.bonusPoints ?? 0),
            e: common_vendor.t(row.totalUsedPoints ?? 0),
            f: row.userId
          };
        })
      } : {
        K: common_vendor.f(listRows.value, (row, k0, i0) => {
          return {
            a: common_vendor.t(formatDate(row.consumeTime)),
            b: common_vendor.t(row.consumePoints ?? 0),
            c: common_vendor.t(row.nickname || "—"),
            d: common_vendor.o(($event) => openLogDetail(row), row.logId),
            e: row.logId
          };
        })
      }, {
        L: listLoadingMore.value
      }, listLoadingMore.value ? {} : listFinished.value ? {} : {}, {
        M: listFinished.value
      }), {
        H: !listRows.value.length,
        N: common_vendor.o(loadMore, "2b")
      }) : {
        O: common_vendor.t(errorMsg.value || "店铺不存在")
      }, {
        b: shop.value,
        P: qrcodeVisible.value
      }, qrcodeVisible.value ? common_vendor.e({
        Q: qrcodeLoading.value
      }, qrcodeLoading.value ? {} : shopQrcodeImage.value || shopQrcodeFallback.value ? common_vendor.e({
        S: shopQrcodeImage.value
      }, shopQrcodeImage.value ? {
        T: shopQrcodeImage.value
      } : {
        U: common_vendor.p({
          text: shopQrcodeFallback.value,
          size: 200,
          ["canvas-id"]: "shop-qrcode-fallback"
        })
      }, {
        V: qrcodeExpireHint.value
      }, qrcodeExpireHint.value ? {
        W: common_vendor.t(qrcodeExpireHint.value)
      } : {}) : {}, {
        R: shopQrcodeImage.value || shopQrcodeFallback.value,
        X: common_vendor.o(closeShopQrcode, "63"),
        Y: common_vendor.n(__props.isDark ? "modal-panel--dark" : ""),
        Z: common_vendor.o(() => {
        }, "c1"),
        aa: common_vendor.o(closeShopQrcode, "91")
      }) : {}, {
        ab: detailVisible.value
      }, detailVisible.value ? common_vendor.e({
        ac: logDetail.value
      }, logDetail.value ? common_vendor.e({
        ad: common_vendor.t(common_vendor.unref(utils_datetimeFormat.formatDateTime)(logDetail.value.createTime)),
        ae: common_vendor.t(logDetail.value.nickname),
        af: common_vendor.t(logDetail.value.phone || ""),
        ag: common_vendor.t(actionTypeLabel(logDetail.value.actionType)),
        ah: common_vendor.t(statusLabel(logDetail.value.status)),
        ai: common_vendor.t(logDetail.value.consumePoints ?? 0),
        aj: common_vendor.t(logDetail.value.remainingPoints ?? 0),
        ak: logDetail.value.couponDiscountPoints
      }, logDetail.value.couponDiscountPoints ? {
        al: common_vendor.t(logDetail.value.couponDiscountPoints),
        am: common_vendor.t(logDetail.value.couponName || "优惠券")
      } : {}, {
        an: logDetail.value.items && logDetail.value.items.length
      }, logDetail.value.items && logDetail.value.items.length ? {
        ao: common_vendor.f(logDetail.value.items, (item, k0, i0) => {
          return {
            a: common_vendor.t(item.productName),
            b: common_vendor.t(item.count),
            c: common_vendor.t(item.linePoints ?? 0),
            d: item.receiptId
          };
        })
      } : {}) : {}, {
        ap: common_vendor.o(($event) => detailVisible.value = false, "fc"),
        aq: common_vendor.n(__props.isDark ? "modal-panel--dark" : ""),
        ar: common_vendor.o(() => {
        }, "7f"),
        as: common_vendor.o(($event) => detailVisible.value = false, "b1")
      }) : {}, {
        at: common_vendor.n(__props.isDark ? "detail-root--dark" : "detail-root--light")
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-7bd7fc71"]]);
wx.createComponent(Component);
