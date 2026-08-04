"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_points = require("../../api/modules/points.js");
const api_modules_shop = require("../../api/modules/shop.js");
const utils_shopRole = require("../../utils/shop-role.js");
const utils_wxPerm = require("../../utils/wx-perm.js");
const utils_pointsLogStatus = require("../../utils/points-log-status.js");
const utils_pointsFormat = require("../../utils/points-format.js");
const utils_datetimeFormat = require("../../utils/datetime-format.js");
const utils_navigation = require("../../utils/navigation.js");
const utils_shopPageContext = require("../../utils/shop-page-context.js");
const composables_useDebounceFn = require("../../composables/use-debounce-fn.js");
const composables_usePaginatedList = require("../../composables/use-paginated-list.js");
const utils_rechargeGift = require("../../utils/recharge-gift.js");
const utils_requestId = require("../../utils/request-id.js");
if (!Array) {
  const _easycom_uni_icons2 = common_vendor.resolveComponent("uni-icons");
  _easycom_uni_icons2();
}
const _easycom_uni_icons = () => "../../uni_modules/uni-icons/components/uni-icons/uni-icons.js";
if (!Math) {
  (SegmentedTabs + _easycom_uni_icons)();
}
const SegmentedTabs = () => "../../components/common/segmented-tabs.js";
const pageSize = 20;
const _sfc_main = {
  __name: "records",
  setup(__props) {
    const shopId = common_vendor.ref("");
    const shopInfo = common_vendor.ref(null);
    const shopRatio = common_vendor.computed(() => {
      var _a;
      return ((_a = shopInfo.value) == null ? void 0 : _a.ratio) ?? null;
    });
    const activeTab = common_vendor.ref("account");
    const searchKeyword = common_vendor.ref("");
    const accountSortType = common_vendor.ref(1);
    const {
      rows,
      listLoading,
      loadingMore,
      refreshing,
      finished,
      reloadList,
      onRefresh,
      loadMore
    } = composables_usePaginatedList.usePaginatedList({
      pageSize,
      fetchPage: (pageNum, size) => {
        const params = {
          shopId: Number(shopId.value),
          pageNum,
          pageSize: size
        };
        const keyword = searchKeyword.value.trim();
        if (keyword)
          params.keyword = keyword;
        if (activeTab.value === "account")
          params.sortType = accountSortType.value;
        return activeTab.value === "log" ? api_modules_points.fetchShopConsumeLogList(params) : api_modules_points.fetchShopPointsAccountList(params);
      }
    });
    const { run: debouncedReloadList } = composables_useDebounceFn.useDebounceFn(() => {
      void reloadList();
    }, 350);
    const tabs = [
      { key: "account", label: "会员积分" },
      { key: "log", label: "消费记录" }
    ];
    const ongoingActivities = common_vendor.ref([]);
    const rechargeVisible = common_vendor.ref(false);
    const rechargeTarget = common_vendor.ref(null);
    const rechargeAmount = common_vendor.ref("");
    const rechargeSubmitting = common_vendor.ref(false);
    const rechargePreview = common_vendor.reactive({
      basePoints: 0,
      giftPoints: 0,
      totalPoints: 0
    });
    let rechargeRequestId = "";
    const accountSortOptions = [
      { value: 1, label: "剩余↓" },
      { value: 2, label: "剩余↑" },
      { value: 3, label: "已用↓" },
      { value: 4, label: "已用↑" }
    ];
    const accountList = common_vendor.computed(
      () => rows.value.map((row) => ({
        id: `${row.userId}-${row.shopId}`,
        nickname: row.nickname || "会员",
        phone: row.phone ? String(row.phone) : "",
        remainingText: utils_pointsFormat.formatPointsAmount(row.remainingPoints),
        usedText: utils_pointsFormat.formatPointsAmount(row.totalUsedPoints),
        baseText: utils_pointsFormat.formatPointsAmount(row.basePoints ?? 0),
        bonusText: utils_pointsFormat.formatPointsAmount(row.bonusPoints ?? 0),
        showBreakdown: row.basePoints != null || row.bonusPoints != null,
        source: row
      }))
    );
    const searchPlaceholder = common_vendor.computed(
      () => activeTab.value === "log" ? "搜索会员昵称或手机号" : "搜索会员昵称或手机号"
    );
    const emptyText = common_vendor.computed(() => {
      const trimmed = searchKeyword.value.trim();
      if (trimmed)
        return "未找到匹配的会员";
      return "暂无数据";
    });
    common_vendor.onLoad((options) => {
      if (!utils_wxPerm.canManageShop()) {
        common_vendor.index.showToast({ title: "无店铺管理权限", icon: "none" });
        utils_navigation.navigateBackDelayed(800);
        return;
      }
      shopId.value = (options == null ? void 0 : options.shopId) ? String(options.shopId) : "";
      if (!shopId.value) {
        common_vendor.index.showToast({ title: "缺少店铺 ID", icon: "none" });
        return;
      }
      utils_shopPageContext.bindOpenerShop((data) => {
        if (!utils_shopRole.isShopClerkCapable(data)) {
          common_vendor.index.showToast({ title: "无店铺管理权限", icon: "none" });
          utils_navigation.navigateBackDelayed(800);
          return;
        }
        shopInfo.value = data;
      });
      void loadShopInfo();
      void loadOngoingActivities();
      void reloadList();
    });
    async function loadShopInfo() {
      if (shopInfo.value || !shopId.value)
        return;
      const res = await api_modules_shop.fetchShopFromUserList(shopId.value);
      if (res.ok && res.data) {
        shopInfo.value = utils_shopPageContext.rememberShopDetail(res.data) || res.data;
      }
    }
    async function loadOngoingActivities() {
      if (!shopId.value)
        return;
      const res = await api_modules_shop.fetchShopOngoingActivities(Number(shopId.value));
      if (res.ok) {
        ongoingActivities.value = res.rows || [];
      }
    }
    function updateRechargePreview() {
      const preview = utils_rechargeGift.previewRechargePoints(rechargeAmount.value, shopRatio.value, ongoingActivities.value);
      rechargePreview.basePoints = preview.basePoints;
      rechargePreview.giftPoints = preview.giftPoints;
      rechargePreview.totalPoints = preview.totalPoints;
    }
    function onRechargeAmountInput() {
      updateRechargePreview();
    }
    function openRecharge(item) {
      rechargeTarget.value = item;
      rechargeAmount.value = "";
      rechargeRequestId = "";
      updateRechargePreview();
      rechargeVisible.value = true;
    }
    function closeRecharge() {
      if (rechargeSubmitting.value)
        return;
      rechargeVisible.value = false;
      rechargeTarget.value = null;
      rechargeAmount.value = "";
      rechargeRequestId = "";
    }
    async function submitRecharge() {
      var _a, _b;
      if (rechargeSubmitting.value || !((_a = rechargeTarget.value) == null ? void 0 : _a.userId))
        return;
      updateRechargePreview();
      if (rechargePreview.basePoints <= 0) {
        common_vendor.index.showToast({ title: "请输入有效充值金额", icon: "none" });
        return;
      }
      const amountYuan = Number(rechargeAmount.value);
      if (Number.isNaN(amountYuan) || amountYuan <= 0) {
        common_vendor.index.showToast({ title: "请输入有效充值金额", icon: "none" });
        return;
      }
      if (!rechargeRequestId) {
        rechargeRequestId = utils_requestId.createRequestId();
      }
      rechargeSubmitting.value = true;
      const res = await api_modules_points.rechargeMemberPoints({
        shopId: Number(shopId.value),
        userId: rechargeTarget.value.userId,
        amountYuan,
        requestId: rechargeRequestId
      });
      rechargeSubmitting.value = false;
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "充值失败", icon: "none" });
        return;
      }
      rechargeRequestId = "";
      const total = ((_b = res.data) == null ? void 0 : _b.totalPoints) ?? rechargePreview.totalPoints;
      common_vendor.index.showToast({
        title: `充值成功，+${utils_pointsFormat.formatPointsAmount(total)} 积分`,
        icon: "success"
      });
      closeRecharge();
      void reloadList();
    }
    function switchTab(key) {
      if (activeTab.value === key)
        return;
      activeTab.value = key;
      void reloadList();
    }
    function switchAccountSort(value) {
      if (accountSortType.value === value)
        return;
      accountSortType.value = value;
      void reloadList();
    }
    function onSearchConfirm() {
      void reloadList();
    }
    function onSearchInput() {
      debouncedReloadList();
    }
    function clearSearch() {
      searchKeyword.value = "";
      void reloadList();
    }
    function logActionLabel(item) {
      if (item.status === utils_pointsLogStatus.POINTS_LOG_STATUS.RECHARGE)
        return "积分充值";
      if (item.actionType === 1)
        return "积分增加";
      if (item.actionType === 2)
        return "积分消耗";
      return "—";
    }
    function logStatusOrActionLabel(item) {
      const label = utils_pointsLogStatus.pointsLogStatusLabel(item.status);
      return label !== "—" ? label : logActionLabel(item);
    }
    function formatLogPoints(item) {
      const pts = item.consumePoints;
      if (pts == null)
        return "—";
      const prefix = utils_pointsLogStatus.isPointsLogGain(item) ? "+" : "-";
      return `${prefix}${utils_pointsFormat.formatPointsAmount(pts)} 积分`;
    }
    function logPointsClass(item) {
      if (utils_pointsLogStatus.isPointsLogGain(item))
        return "card-points--gain";
      return "";
    }
    function openLogDetail(item) {
      if (!(item == null ? void 0 : item.logId))
        return;
      common_vendor.index.navigateTo({
        url: `/pages/member/order-detail?shopId=${shopId.value}&logId=${item.logId}&from=shop`,
        animationType: "slide-in-right",
        animationDuration: 200,
        success(res) {
          var _a;
          (_a = res.eventChannel) == null ? void 0 : _a.emit("order", item);
        }
      });
    }
    return (_ctx, _cache) => {
      var _a;
      return common_vendor.e({
        a: common_vendor.o(switchTab, "a1"),
        b: common_vendor.p({
          ["model-value"]: activeTab.value,
          tabs
        }),
        c: common_vendor.p({
          type: "search",
          size: 22,
          color: "#999"
        }),
        d: searchPlaceholder.value,
        e: common_vendor.o(onSearchConfirm, "27"),
        f: common_vendor.o([($event) => searchKeyword.value = $event.detail.value, onSearchInput], "58"),
        g: searchKeyword.value,
        h: searchKeyword.value
      }, searchKeyword.value ? {
        i: common_vendor.o(clearSearch, "60")
      } : {}, {
        j: activeTab.value === "account"
      }, activeTab.value === "account" ? {
        k: common_vendor.f(accountSortOptions, (opt, k0, i0) => {
          return {
            a: common_vendor.t(opt.label),
            b: opt.value,
            c: accountSortType.value === opt.value ? 1 : "",
            d: common_vendor.o(($event) => switchAccountSort(opt.value), opt.value)
          };
        })
      } : {}, {
        l: common_vendor.unref(listLoading) && !common_vendor.unref(rows).length
      }, common_vendor.unref(listLoading) && !common_vendor.unref(rows).length ? {} : !common_vendor.unref(rows).length ? {
        n: common_vendor.t(emptyText.value)
      } : activeTab.value === "log" ? {
        p: common_vendor.f(common_vendor.unref(rows), (item, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(item.nickname || "会员"),
            b: common_vendor.t(logStatusOrActionLabel(item)),
            c: common_vendor.t(common_vendor.unref(utils_datetimeFormat.formatDateTime)(item.consumeTime || item.createTime)),
            d: common_vendor.t(formatLogPoints(item)),
            e: common_vendor.n(logPointsClass(item)),
            f: item.phone
          }, item.phone ? {
            g: common_vendor.t(item.phone),
            h: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(item.remainingPoints))
          } : {
            i: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(item.remainingPoints))
          }, {
            j: item.logId,
            k: common_vendor.o(($event) => openLogDetail(item), item.logId)
          });
        })
      } : {
        q: common_vendor.f(accountList.value, (item, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(item.nickname),
            b: item.phone
          }, item.phone ? {
            c: common_vendor.t(item.phone)
          } : {}, {
            d: common_vendor.o(($event) => openRecharge(item.source), item.id),
            e: common_vendor.t(item.remainingText),
            f: common_vendor.t(item.usedText),
            g: item.showBreakdown
          }, item.showBreakdown ? {
            h: common_vendor.t(item.baseText),
            i: common_vendor.t(item.bonusText)
          } : {}, {
            j: item.id
          });
        })
      }, {
        m: !common_vendor.unref(rows).length,
        o: activeTab.value === "log",
        r: common_vendor.unref(loadingMore)
      }, common_vendor.unref(loadingMore) ? {} : common_vendor.unref(finished) && common_vendor.unref(rows).length ? {} : {}, {
        s: common_vendor.unref(finished) && common_vendor.unref(rows).length,
        t: activeTab.value === "account" ? 1 : "",
        v: common_vendor.unref(refreshing),
        w: common_vendor.o((...args) => common_vendor.unref(onRefresh) && common_vendor.unref(onRefresh)(...args), "6a"),
        x: common_vendor.o((...args) => common_vendor.unref(loadMore) && common_vendor.unref(loadMore)(...args), "7f"),
        y: rechargeVisible.value
      }, rechargeVisible.value ? common_vendor.e({
        z: common_vendor.t(((_a = rechargeTarget.value) == null ? void 0 : _a.nickname) || "会员"),
        A: shopRatio.value
      }, shopRatio.value ? {
        B: common_vendor.t(shopRatio.value)
      } : {}, {
        C: common_vendor.o([($event) => rechargeAmount.value = $event.detail.value, onRechargeAmountInput], "07"),
        D: rechargeAmount.value,
        E: rechargePreview.basePoints > 0
      }, rechargePreview.basePoints > 0 ? common_vendor.e({
        F: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(rechargePreview.basePoints)),
        G: rechargePreview.giftPoints > 0
      }, rechargePreview.giftPoints > 0 ? {
        H: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(rechargePreview.giftPoints))
      } : {}, {
        I: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(rechargePreview.totalPoints))
      }) : {}, {
        J: common_vendor.o(closeRecharge, "30"),
        K: common_vendor.t(rechargeSubmitting.value ? "提交中…" : "确认充值"),
        L: rechargeSubmitting.value ? 1 : "",
        M: common_vendor.o(submitRecharge, "6c"),
        N: common_vendor.o(() => {
        }, "a6"),
        O: common_vendor.o(closeRecharge, "09")
      }) : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-80b293e6"]]);
wx.createPage(MiniProgramPage);
