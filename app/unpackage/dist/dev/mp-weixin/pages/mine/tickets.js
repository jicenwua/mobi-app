"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_ticket = require("../../api/modules/ticket.js");
const services_notifySocket = require("../../services/notify-socket.js");
const composables_usePaginatedList = require("../../composables/use-paginated-list.js");
const composables_useTicketNotify = require("../../composables/use-ticket-notify.js");
const composables_useDebounceFn = require("../../composables/use-debounce-fn.js");
const utils_datetimeFormat = require("../../utils/datetime-format.js");
const utils_ticketStatus = require("../../utils/ticket-status.js");
const PAGE_SIZE = 20;
const _sfc_main = {
  __name: "tickets",
  setup(__props) {
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
      pageSize: PAGE_SIZE,
      fetchPage: (pageNum, pageSize) => api_modules_ticket.fetchMyTickets({ pageNum, pageSize })
    });
    const { run: debouncedReloadList } = composables_useDebounceFn.useDebounceFn(() => {
      void reloadList();
    }, 300);
    let pageReady = false;
    let needRefreshOnShow = false;
    const { subscribe: subscribeTicketNotify } = composables_useTicketNotify.useTicketNotify((payload) => {
      if ((payload == null ? void 0 : payload.type) === "unread_changed" || (payload == null ? void 0 : payload.type) === "ticket_message" || (payload == null ? void 0 : payload.type) === "ticket_status_changed") {
        debouncedReloadList();
      }
    });
    function formatUnreadCount(count) {
      const value = Number(count) || 0;
      if (value > 99)
        return "99+";
      return String(value);
    }
    function statusClass(status) {
      return utils_ticketStatus.ticketStatusClass(status);
    }
    common_vendor.onLoad(() => {
      subscribeTicketNotify();
      void reloadList().finally(() => {
        pageReady = true;
      });
    });
    common_vendor.onShow(() => {
      services_notifySocket.connectNotifySocket();
      if (!pageReady)
        return;
      if (needRefreshOnShow) {
        needRefreshOnShow = false;
        void reloadList();
      }
    });
    function goCreate() {
      needRefreshOnShow = true;
      common_vendor.index.navigateTo({
        url: "/pages/mine/ticket-create",
        animationType: "slide-in-right",
        animationDuration: 200
      });
    }
    function goDetail(ticketId) {
      needRefreshOnShow = true;
      common_vendor.index.navigateTo({
        url: `/pages/mine/ticket-detail?id=${ticketId}`,
        animationType: "slide-in-right",
        animationDuration: 200
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.unref(listLoading) && !common_vendor.unref(rows).length
      }, common_vendor.unref(listLoading) && !common_vendor.unref(rows).length ? {} : !common_vendor.unref(rows).length ? {} : {}, {
        b: !common_vendor.unref(rows).length,
        c: common_vendor.f(common_vendor.unref(rows), (item, k0, i0) => {
          return common_vendor.e({
            a: item.unreadCount > 0
          }, item.unreadCount > 0 ? {
            b: common_vendor.t(formatUnreadCount(item.unreadCount))
          } : {}, {
            c: common_vendor.t(item.title),
            d: common_vendor.t(item.statusLabel),
            e: common_vendor.n(statusClass(item.status)),
            f: common_vendor.t(item.description),
            g: common_vendor.t(common_vendor.unref(utils_datetimeFormat.formatDateTime)(item.createTime)),
            h: item.ticketId,
            i: common_vendor.o(($event) => goDetail(item.ticketId), item.ticketId)
          });
        }),
        d: common_vendor.unref(loadingMore)
      }, common_vendor.unref(loadingMore) ? {} : common_vendor.unref(finished) && common_vendor.unref(rows).length ? {} : {}, {
        e: common_vendor.unref(finished) && common_vendor.unref(rows).length,
        f: common_vendor.unref(refreshing),
        g: common_vendor.o((...args) => common_vendor.unref(onRefresh) && common_vendor.unref(onRefresh)(...args), "f1"),
        h: common_vendor.o((...args) => common_vendor.unref(loadMore) && common_vendor.unref(loadMore)(...args), "16"),
        i: common_vendor.o(goCreate, "7d")
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-f8d5c9d9"]]);
wx.createPage(MiniProgramPage);
