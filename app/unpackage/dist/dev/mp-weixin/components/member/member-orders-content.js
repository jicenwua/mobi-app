"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_points = require("../../api/modules/points.js");
const utils_pointsLogStatus = require("../../utils/points-log-status.js");
const utils_datetimeFormat = require("../../utils/datetime-format.js");
const utils_pointsFormat = require("../../utils/points-format.js");
const composables_usePaginatedList = require("../../composables/use-paginated-list.js");
const utils_memberOrdersCache = require("../../utils/member-orders-cache.js");
const pageSize = 10;
const REFRESH_PULL_THRESHOLD = 72;
const _sfc_main = {
  __name: "member-orders-content",
  props: {
    isDark: { type: Boolean, default: false }
  },
  setup(__props, { expose: __expose }) {
    const tabs = [
      { key: "unused", label: "待使用", status: utils_pointsLogStatus.POINTS_LOG_STATUS.PENDING_USE },
      { key: "verified", label: "已核销", excludeStatus: utils_pointsLogStatus.POINTS_LOG_STATUS.PENDING_USE }
    ];
    const activeTab = common_vendor.ref("unused");
    const pullDistance = common_vendor.ref(0);
    const refreshReady = common_vendor.ref(false);
    let syncedEpoch = utils_memberOrdersCache.getMemberOrdersCacheEpoch();
    const pullRefreshStyle = common_vendor.computed(() => refreshing.value ? "black" : "none");
    const activeTabMeta = common_vendor.computed(() => tabs.find((t) => t.key === activeTab.value) || tabs[0]);
    const activeTabLabel = common_vendor.computed(() => activeTabMeta.value.label);
    const pullStatusText = common_vendor.computed(() => {
      if (refreshing.value)
        return "刷新中…";
      if (refreshReady.value)
        return "松手刷新";
      return "继续下拉刷新";
    });
    const {
      rows,
      listLoading,
      loadingMore,
      refreshing,
      finished,
      pageNum,
      reloadList: fetchList,
      loadMore
    } = composables_usePaginatedList.usePaginatedList({
      pageSize,
      fetchPage: (num, size) => {
        const tabMeta = activeTabMeta.value;
        const query = {
          pageNum: num,
          pageSize: size,
          actionType: 2
        };
        if (tabMeta.status != null)
          query.status = tabMeta.status;
        if (tabMeta.excludeStatus != null)
          query.excludeStatus = tabMeta.excludeStatus;
        return api_modules_points.fetchMyOrderList(query);
      }
    });
    function restoreFromCache(tabKey) {
      const cached = utils_memberOrdersCache.getMemberOrdersCache(tabKey);
      if (!cached)
        return false;
      rows.value = cached.rows;
      finished.value = cached.finished;
      pageNum.value = cached.pageNum;
      return true;
    }
    function saveToCache(tabKey) {
      utils_memberOrdersCache.setMemberOrdersCache(tabKey, {
        rows: rows.value,
        finished: finished.value,
        pageNum: pageNum.value
      });
    }
    function statusLabel(status) {
      return utils_pointsLogStatus.pointsLogStatusLabel(status);
    }
    function statusTagClass(status) {
      return utils_pointsLogStatus.pointsLogStatusTagClass(status);
    }
    function formatOrderItemsSummary(row) {
      const items = row == null ? void 0 : row.items;
      if (!(items == null ? void 0 : items.length))
        return "";
      return items.map((item) => {
        var _a;
        const name = ((_a = item.productName) == null ? void 0 : _a.trim()) || "商品";
        const count = item.count != null && item.count > 0 ? item.count : 1;
        return `${name}×${count}`;
      }).join("、");
    }
    async function reloadList(force = false) {
      const tabKey = activeTab.value;
      if (!force && utils_memberOrdersCache.isMemberOrdersCacheReady(tabKey) && restoreFromCache(tabKey)) {
        syncedEpoch = utils_memberOrdersCache.getMemberOrdersCacheEpoch();
        return;
      }
      await fetchList();
      saveToCache(tabKey);
      syncedEpoch = utils_memberOrdersCache.getMemberOrdersCacheEpoch();
    }
    async function refreshIfStale() {
      if (syncedEpoch === utils_memberOrdersCache.getMemberOrdersCacheEpoch())
        return;
      await reloadList(true);
    }
    function resetPullRefreshState() {
      pullDistance.value = 0;
      refreshReady.value = false;
    }
    function onRefresherPulling(e) {
      var _a;
      const dy = Number(((_a = e == null ? void 0 : e.detail) == null ? void 0 : _a.dy) ?? 0);
      pullDistance.value = Math.min(dy, REFRESH_PULL_THRESHOLD);
      refreshReady.value = dy >= REFRESH_PULL_THRESHOLD;
    }
    function onRefresherAbort() {
      resetPullRefreshState();
      refreshing.value = false;
    }
    function onRefresherRestore() {
      resetPullRefreshState();
    }
    async function onRefresh() {
      if (!refreshReady.value) {
        refreshing.value = false;
        resetPullRefreshState();
        return;
      }
      refreshing.value = true;
      utils_memberOrdersCache.invalidateMemberOrdersCache(activeTab.value);
      try {
        await reloadList(true);
      } finally {
        refreshing.value = false;
        resetPullRefreshState();
      }
    }
    function switchTab(key) {
      if (activeTab.value === key)
        return;
      activeTab.value = key;
      if (utils_memberOrdersCache.getMemberOrdersCacheEpoch() !== syncedEpoch) {
        void reloadList(true);
        return;
      }
      if (utils_memberOrdersCache.isMemberOrdersCacheReady(key) && restoreFromCache(key)) {
        return;
      }
      void reloadList(false);
    }
    function openDetail(row) {
      if (!(row == null ? void 0 : row.logId) || !(row == null ? void 0 : row.shopId))
        return;
      common_vendor.index.navigateTo({
        url: `/pages/member/order-detail?shopId=${row.shopId}&logId=${row.logId}`,
        animationType: "slide-in-right",
        animationDuration: 200,
        success(res) {
          var _a;
          (_a = res.eventChannel) == null ? void 0 : _a.emit("order", row);
        }
      });
    }
    __expose({ reload: reloadList, refreshIfStale });
    common_vendor.onMounted(() => {
      void reloadList();
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.f(tabs, (tab, k0, i0) => {
          return {
            a: common_vendor.t(tab.label),
            b: tab.key,
            c: activeTab.value === tab.key ? 1 : "",
            d: common_vendor.o(($event) => switchTab(tab.key), tab.key)
          };
        }),
        b: activeTab.value === "verified" ? 1 : "",
        c: pullDistance.value > 0 || common_vendor.unref(refreshing)
      }, pullDistance.value > 0 || common_vendor.unref(refreshing) ? {
        d: common_vendor.t(pullStatusText.value)
      } : {}, {
        e: common_vendor.unref(listLoading) && !common_vendor.unref(rows).length
      }, common_vendor.unref(listLoading) && !common_vendor.unref(rows).length ? {} : !common_vendor.unref(rows).length ? {
        g: common_vendor.t(activeTabLabel.value)
      } : common_vendor.e({
        h: common_vendor.f(common_vendor.unref(rows), (row, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(row.shopName || "店铺"),
            b: common_vendor.t(statusLabel(row.status)),
            c: common_vendor.n(`order-status-tag--${statusTagClass(row.status)}`),
            d: formatOrderItemsSummary(row)
          }, formatOrderItemsSummary(row) ? {
            e: common_vendor.t(formatOrderItemsSummary(row))
          } : {}, {
            f: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(row.consumePoints)),
            g: common_vendor.t(common_vendor.unref(utils_datetimeFormat.formatDateTime)(row.createTime)),
            h: row.logId,
            i: common_vendor.o(($event) => openDetail(row), row.logId)
          });
        }),
        i: common_vendor.unref(loadingMore)
      }, common_vendor.unref(loadingMore) ? {} : common_vendor.unref(finished) ? {} : {}, {
        j: common_vendor.unref(finished)
      }), {
        f: !common_vendor.unref(rows).length,
        k: REFRESH_PULL_THRESHOLD,
        l: pullRefreshStyle.value,
        m: common_vendor.unref(refreshing),
        n: common_vendor.o(onRefresherPulling, "a9"),
        o: common_vendor.o(onRefresh, "fe"),
        p: common_vendor.o(onRefresherAbort, "0b"),
        q: common_vendor.o(onRefresherRestore, "79"),
        r: common_vendor.o((...args) => common_vendor.unref(loadMore) && common_vendor.unref(loadMore)(...args), "d4"),
        s: common_vendor.n(__props.isDark ? "orders-root--dark" : "orders-root--light")
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-07d1b2dc"]]);
wx.createComponent(Component);
