"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_points = require("../../api/modules/points.js");
const api_modules_qrcode = require("../../api/modules/qrcode.js");
const utils_pointsLogStatus = require("../../utils/points-log-status.js");
const utils_pointsFormat = require("../../utils/points-format.js");
const utils_datetimeFormat = require("../../utils/datetime-format.js");
const utils_memberOrdersCache = require("../../utils/member-orders-cache.js");
const composables_useTheme = require("../../composables/use-theme.js");
const composables_usePolling = require("../../composables/use-polling.js");
if (!Math) {
  (PageLoading + QrcodeCanvas)();
}
const QrcodeCanvas = () => "../../components/common/qrcode-canvas.js";
const PageLoading = () => "../../components/common/page-loading.js";
const ORDER_STATUS_POLL_MS = 3e3;
const _sfc_main = {
  __name: "order-detail",
  setup(__props) {
    const { isDark, loadTheme } = composables_useTheme.useTheme();
    const shopId = common_vendor.ref("");
    const logId = common_vendor.ref("");
    const fromShop = common_vendor.ref(false);
    const detail = common_vendor.ref(null);
    const errorMsg = common_vendor.ref("");
    const itemsLoading = common_vendor.ref(false);
    const qrContent = common_vendor.ref("");
    const qrLoading = common_vendor.ref(false);
    const qrError = common_vendor.ref("");
    const refunding = common_vendor.ref(false);
    let verifyNotified = false;
    const showOrderQrcode = common_vendor.computed(
      () => {
        var _a;
        return !fromShop.value && ((_a = detail.value) == null ? void 0 : _a.status) === utils_pointsLogStatus.POINTS_LOG_STATUS.PENDING_USE;
      }
    );
    const showRefundButton = common_vendor.computed(
      () => {
        var _a;
        return !fromShop.value && ((_a = detail.value) == null ? void 0 : _a.status) === utils_pointsLogStatus.POINTS_LOG_STATUS.PENDING_USE;
      }
    );
    const showConsumeTime = common_vendor.computed(() => {
      const d = detail.value;
      if (!d || d.status === utils_pointsLogStatus.POINTS_LOG_STATUS.PENDING_USE)
        return false;
      return !!d.consumeTime;
    });
    const isPointsGain = common_vendor.computed(() => utils_pointsLogStatus.isPointsLogGain(detail.value));
    const consumePointsLabel = common_vendor.computed(() => {
      const d = detail.value;
      if (!d)
        return "积分变动";
      if (d.status === utils_pointsLogStatus.POINTS_LOG_STATUS.RECHARGE)
        return "充值积分";
      if (d.status === utils_pointsLogStatus.POINTS_LOG_STATUS.ADMIN_ADD)
        return "添加积分";
      if (d.status === utils_pointsLogStatus.POINTS_LOG_STATUS.ADMIN_DEDUCT)
        return "扣除积分";
      if (d.actionType === 1)
        return "增加积分";
      return "消费积分";
    });
    const consumePointsDisplay = common_vendor.computed(() => {
      var _a;
      const pts = (_a = detail.value) == null ? void 0 : _a.consumePoints;
      if (pts == null)
        return "—";
      const prefix = isPointsGain.value ? "+" : "-";
      return `${prefix}${utils_pointsFormat.formatPointsAmount(pts)} 积分`;
    });
    const consumePointsClass = common_vendor.computed(
      () => isPointsGain.value ? "summary-value--gain" : "summary-value--points"
    );
    const orderQrCanvasId = common_vendor.computed(() => `order-detail-qr-${logId.value || "x"}`);
    function statusLabel(status) {
      return utils_pointsLogStatus.pointsLogStatusLabel(status);
    }
    function applyOrderSnapshot(row) {
      var _a;
      if (!row)
        return;
      detail.value = { ...row };
      if (row.status === utils_pointsLogStatus.POINTS_LOG_STATUS.PENDING_USE) {
        void loadOrderQrcode();
        orderPolling.start();
      } else {
        orderPolling.stop();
      }
      if (!((_a = row.items) == null ? void 0 : _a.length)) {
        void refreshDetailItems();
      }
    }
    function shouldPollOrderStatus() {
      var _a;
      return !fromShop.value && ((_a = detail.value) == null ? void 0 : _a.status) === utils_pointsLogStatus.POINTS_LOG_STATUS.PENDING_USE;
    }
    const orderPolling = composables_usePolling.usePolling(
      () => shouldPollOrderStatus(),
      () => pollOrderStatus(),
      ORDER_STATUS_POLL_MS
    );
    function handleOrderStatusSettled(nextStatus, data) {
      orderPolling.stop();
      detail.value = { ...detail.value, ...data, status: nextStatus };
      qrContent.value = "";
      utils_memberOrdersCache.invalidateMemberOrdersCache();
      if (nextStatus === utils_pointsLogStatus.POINTS_LOG_STATUS.VERIFIED && !verifyNotified) {
        verifyNotified = true;
        common_vendor.index.showModal({
          title: "核销成功",
          content: "订单已核销，感谢您的光临",
          showCancel: false
        });
      }
    }
    async function pollOrderStatus() {
      if (!logId.value || !shopId.value || !shouldPollOrderStatus()) {
        orderPolling.stop();
        return;
      }
      const res = await api_modules_points.fetchConsumeLogDetail(Number(logId.value), Number(shopId.value));
      if (!res.ok || !res.data)
        return;
      const nextStatus = res.data.status;
      if (nextStatus === utils_pointsLogStatus.POINTS_LOG_STATUS.PENDING_USE)
        return;
      handleOrderStatusSettled(nextStatus, res.data);
    }
    async function loadOrderQrcode() {
      var _a;
      if (!logId.value || !shopId.value)
        return;
      qrLoading.value = true;
      qrError.value = "";
      qrContent.value = "";
      const res = await api_modules_qrcode.generateOrderQrcode({
        logId: Number(logId.value),
        shopId: Number(shopId.value)
      });
      qrLoading.value = false;
      if (!res.ok || !((_a = res.data) == null ? void 0 : _a.qrContent)) {
        qrError.value = res.msg || "生成失败";
        return;
      }
      qrContent.value = res.data.qrContent;
    }
    function onRefund() {
      if (refunding.value || !detail.value)
        return;
      common_vendor.index.showModal({
        title: "确认退款",
        content: "退款后积分将原路退回，是否继续？",
        confirmText: "确认退款",
        success(res) {
          if (!res.confirm)
            return;
          void doRefund();
        }
      });
    }
    async function doRefund() {
      if (!logId.value || !shopId.value)
        return;
      refunding.value = true;
      const res = await api_modules_points.refundOrder({
        logId: Number(logId.value),
        shopId: Number(shopId.value)
      });
      refunding.value = false;
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "退款失败", icon: "none" });
        return;
      }
      detail.value = {
        ...detail.value,
        status: utils_pointsLogStatus.POINTS_LOG_STATUS.CANCELLED,
        remainingPoints: res.data ?? detail.value.remainingPoints
      };
      qrContent.value = "";
      orderPolling.stop();
      utils_memberOrdersCache.invalidateMemberOrdersCache();
      common_vendor.index.showToast({ title: "退款成功", icon: "success" });
      void refreshDetailItems();
    }
    async function refreshDetailItems() {
      if (!logId.value || !shopId.value)
        return;
      itemsLoading.value = true;
      const res = await api_modules_points.fetchConsumeLogDetail(Number(logId.value), Number(shopId.value));
      itemsLoading.value = false;
      if (!res.ok || !res.data) {
        if (!detail.value) {
          errorMsg.value = res.msg || "加载订单失败";
        }
        return;
      }
      detail.value = { ...detail.value, ...res.data };
    }
    common_vendor.onLoad((options) => {
      var _a, _b;
      loadTheme();
      shopId.value = (options == null ? void 0 : options.shopId) ? String(options.shopId) : "";
      logId.value = (options == null ? void 0 : options.logId) ? String(options.logId) : "";
      fromShop.value = (options == null ? void 0 : options.from) === "shop";
      if (!shopId.value || !logId.value) {
        errorMsg.value = "订单信息无效";
        return;
      }
      const pages = getCurrentPages();
      const page = pages[pages.length - 1];
      let snapshotApplied = false;
      const channel = (_a = page == null ? void 0 : page.getOpenerEventChannel) == null ? void 0 : _a.call(page);
      (_b = channel == null ? void 0 : channel.on) == null ? void 0 : _b.call(channel, "order", (data) => {
        snapshotApplied = true;
        applyOrderSnapshot(data);
      });
      setTimeout(() => {
        if (snapshotApplied || detail.value)
          return;
        void (async () => {
          itemsLoading.value = true;
          const res = await api_modules_points.fetchConsumeLogDetail(Number(logId.value), Number(shopId.value));
          itemsLoading.value = false;
          if (!res.ok || !res.data) {
            errorMsg.value = res.msg || "加载订单失败";
            return;
          }
          applyOrderSnapshot(res.data);
        })();
      }, 80);
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: !detail.value
      }, !detail.value ? {
        b: common_vendor.t(errorMsg.value || "订单信息无效")
      } : common_vendor.e({
        c: common_vendor.t(statusLabel(detail.value.status)),
        d: detail.value.status !== common_vendor.unref(utils_pointsLogStatus.POINTS_LOG_STATUS).PENDING_USE ? 1 : "",
        e: showOrderQrcode.value
      }, showOrderQrcode.value ? {} : {}, {
        f: fromShop.value
      }, fromShop.value ? {
        g: common_vendor.t(detail.value.nickname || "—")
      } : {}, {
        h: fromShop.value && detail.value.phone
      }, fromShop.value && detail.value.phone ? {
        i: common_vendor.t(detail.value.phone)
      } : {}, {
        j: !fromShop.value
      }, !fromShop.value ? {
        k: common_vendor.t(detail.value.shopName || "—")
      } : {}, {
        l: common_vendor.t(common_vendor.unref(utils_datetimeFormat.formatDateTime)(detail.value.createTime)),
        m: showConsumeTime.value
      }, showConsumeTime.value ? {
        n: common_vendor.t(common_vendor.unref(utils_datetimeFormat.formatDateTime)(detail.value.consumeTime))
      } : {}, {
        o: common_vendor.t(consumePointsLabel.value),
        p: common_vendor.t(consumePointsDisplay.value),
        q: common_vendor.n(consumePointsClass.value),
        r: detail.value.remark
      }, detail.value.remark ? {
        s: common_vendor.t(detail.value.remark)
      } : {}, {
        t: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(detail.value.remainingPoints)),
        v: detail.value.couponDiscountPoints
      }, detail.value.couponDiscountPoints ? {
        w: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(detail.value.couponDiscountPoints))
      } : {}, {
        x: itemsLoading.value
      }, itemsLoading.value ? {
        y: common_vendor.p({
          text: "加载商品明细…",
          compact: true,
          color: common_vendor.unref(isDark) ? "#5ac8fa" : "#007aff"
        })
      } : detail.value.items && detail.value.items.length ? {
        A: common_vendor.f(detail.value.items, (item, k0, i0) => {
          return {
            a: common_vendor.t(item.productName),
            b: common_vendor.t(item.count),
            c: common_vendor.t(common_vendor.unref(utils_pointsFormat.formatPointsAmount)(item.linePoints)),
            d: item.receiptId || item.productId
          };
        })
      } : {}, {
        z: detail.value.items && detail.value.items.length,
        B: showOrderQrcode.value
      }, showOrderQrcode.value ? common_vendor.e({
        C: qrLoading.value
      }, qrLoading.value ? {
        D: common_vendor.p({
          text: "生成核销码…",
          color: common_vendor.unref(isDark) ? "#ffb800" : "#ff9800"
        })
      } : qrContent.value ? {
        F: common_vendor.p({
          text: qrContent.value,
          size: 220,
          ["canvas-id"]: orderQrCanvasId.value
        })
      } : {
        G: common_vendor.t(qrError.value || "无法生成订单码")
      }, {
        E: qrContent.value
      }) : {}, {
        H: showRefundButton.value
      }, showRefundButton.value ? {
        I: common_vendor.t(refunding.value ? "退款中…" : "申请退款"),
        J: refunding.value ? 1 : "",
        K: refunding.value,
        L: common_vendor.o(onRefund, "e2")
      } : {}), {
        M: common_vendor.n(common_vendor.unref(isDark) ? "page--dark" : "page--light"),
        N: common_vendor.n({
          "page--with-refund": showRefundButton.value
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-6ca6b674"]]);
wx.createPage(MiniProgramPage);
