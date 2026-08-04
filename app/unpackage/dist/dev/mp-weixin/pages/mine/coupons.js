"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_coupon = require("../../api/modules/coupon.js");
const utils_datetimeFormat = require("../../utils/datetime-format.js");
const utils_couponDisplay = require("../../utils/coupon-display.js");
const composables_usePaginatedList = require("../../composables/use-paginated-list.js");
const _sfc_main = {
  __name: "coupons",
  setup(__props) {
    const tabs = [
      { key: "unused", label: "待使用", status: 0 },
      { key: "expired", label: "已过期", status: 2 },
      { key: "used", label: "已使用", status: 1 }
    ];
    const activeTab = common_vendor.ref("unused");
    const activeTabMeta = common_vendor.computed(() => tabs.find((t) => t.key === activeTab.value) || tabs[0]);
    const activeTabLabel = common_vendor.computed(() => activeTabMeta.value.label);
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
      pageSize: 20,
      fetchPage: (pageNum, pageSize) => api_modules_coupon.fetchMyCoupons({
        status: activeTabMeta.value.status,
        pageNum,
        pageSize
      })
    });
    const tabSliderStyle = common_vendor.computed(() => {
      const idx = tabs.findIndex((t) => t.key === activeTab.value);
      const width = 100 / tabs.length;
      return {
        width: `calc(${width}% - 3px)`,
        transform: `translateX(${idx * 100}%)`
      };
    });
    common_vendor.onLoad(() => {
      void reloadList();
    });
    function switchTab(key) {
      if (activeTab.value === key)
        return;
      activeTab.value = key;
      void reloadList();
    }
    function cardClass(item) {
      if (activeTab.value === "unused")
        return "";
      if (activeTab.value === "expired")
        return "coupon-card--muted";
      return "coupon-card--muted";
    }
    function formatCouponTime(item) {
      if (activeTab.value === "used") {
        return item.usedTime ? `使用时间 ${utils_datetimeFormat.formatDateTime(item.usedTime, { empty: "", maxLen: 16 })}` : "";
      }
      if (item.expireTime) {
        return activeTab.value === "expired" ? `已于 ${utils_datetimeFormat.formatDateTime(item.expireTime, { empty: "", maxLen: 16 })} 过期` : `有效期至 ${utils_datetimeFormat.formatDateTime(item.expireTime, { empty: "", maxLen: 16 })}`;
      }
      return item.receiveTime ? `领取于 ${utils_datetimeFormat.formatDateTime(item.receiveTime, { empty: "", maxLen: 16 })}` : "";
    }
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
        b: common_vendor.s(tabSliderStyle.value),
        c: common_vendor.unref(listLoading) && !common_vendor.unref(rows).length
      }, common_vendor.unref(listLoading) && !common_vendor.unref(rows).length ? {} : !common_vendor.unref(rows).length ? {
        e: common_vendor.t(activeTabLabel.value)
      } : {}, {
        d: !common_vendor.unref(rows).length,
        f: common_vendor.f(common_vendor.unref(rows), (item, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.unref(utils_couponDisplay.couponIconSrc)(item.type),
            b: common_vendor.t(item.couponName || "店铺优惠券"),
            c: common_vendor.t(common_vendor.unref(utils_couponDisplay.couponTypeLabel)(item.type)),
            d: common_vendor.t(common_vendor.unref(utils_couponDisplay.formatCouponDesc)(item, {
              emptyValueFallback: true
            })),
            e: item.shopName
          }, item.shopName ? {
            f: common_vendor.t(item.shopName)
          } : {}, {
            g: common_vendor.t(formatCouponTime(item)),
            h: item.userCouponId,
            i: common_vendor.n(cardClass())
          });
        }),
        g: common_vendor.unref(loadingMore)
      }, common_vendor.unref(loadingMore) ? {} : common_vendor.unref(finished) && common_vendor.unref(rows).length ? {} : {}, {
        h: common_vendor.unref(finished) && common_vendor.unref(rows).length,
        i: common_vendor.unref(refreshing),
        j: common_vendor.o((...args) => common_vendor.unref(onRefresh) && common_vendor.unref(onRefresh)(...args), "a2"),
        k: common_vendor.o((...args) => common_vendor.unref(loadMore) && common_vendor.unref(loadMore)(...args), "ec")
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-aa0a0a80"]]);
wx.createPage(MiniProgramPage);
