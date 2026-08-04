"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_shop = require("../../api/modules/shop.js");
const utils_wxPerm = require("../../utils/wx-perm.js");
const utils_navigation = require("../../utils/navigation.js");
if (!Array) {
  const _easycom_uni_datetime_picker2 = common_vendor.resolveComponent("uni-datetime-picker");
  _easycom_uni_datetime_picker2();
}
const _easycom_uni_datetime_picker = () => "../../uni_modules/uni-datetime-picker/components/uni-datetime-picker/uni-datetime-picker.js";
if (!Math) {
  _easycom_uni_datetime_picker();
}
const _sfc_main = {
  __name: "statistics",
  setup(__props) {
    const shopId = common_vendor.ref("");
    const loading = common_vendor.ref(true);
    const refreshing = common_vendor.ref(false);
    const statistics = common_vendor.ref(null);
    const selectedScope = common_vendor.ref("all");
    const periodPreset = common_vendor.ref("today");
    const customDateRange = common_vendor.ref([]);
    const canvasWidth = common_vendor.ref(320);
    const canvasHeight = common_vendor.ref(220);
    const periodOptions = [
      { label: "今天", value: "today" },
      { label: "近3天", value: "3d" },
      { label: "近7天", value: "7d" },
      { label: "近30天", value: "30d" },
      { label: "自定义", value: "custom" }
    ];
    const stats = common_vendor.computed(() => {
      var _a, _b, _c, _d;
      return {
        newUsers: ((_a = statistics.value) == null ? void 0 : _a.todayNewUsers) ?? 0,
        orders: ((_b = statistics.value) == null ? void 0 : _b.todayOrders) ?? 0,
        pointsUsed: ((_c = statistics.value) == null ? void 0 : _c.totalPointsUsed) ?? 0,
        rechargeAmount: ((_d = statistics.value) == null ? void 0 : _d.todayRechargeAmount) ?? 0
      };
    });
    const todayStr = common_vendor.computed(() => formatDate(/* @__PURE__ */ new Date()));
    const isTodayPeriod = common_vendor.computed(() => {
      const { startDate, endDate } = resolveDateRange();
      return startDate === endDate && startDate === todayStr.value;
    });
    const statTitles = common_vendor.computed(() => {
      if (isTodayPeriod.value) {
        return {
          newUsers: "今日新用户",
          orders: "今日订单",
          pointsUsed: "今日使用积分",
          rechargeAmount: "今日充值金额"
        };
      }
      return {
        newUsers: "新用户",
        orders: "订单",
        pointsUsed: "使用积分",
        rechargeAmount: "充值金额"
      };
    });
    const chartTitle = common_vendor.computed(() => {
      const { startDate, endDate } = resolveDateRange();
      if (startDate === endDate) {
        return `${startDate} 走势`;
      }
      return `${startDate} ~ ${endDate} 走势`;
    });
    const scopeOptions = common_vendor.computed(() => {
      var _a;
      if (!((_a = statistics.value) == null ? void 0 : _a.headShop)) {
        return [];
      }
      const options = [
        { label: "全部汇总", value: "all" },
        { label: "总店本店", value: `self:${shopId.value}` }
      ];
      for (const branch of statistics.value.branches || []) {
        options.push({
          label: branch.shopName || `分店#${branch.id}`,
          value: `shop:${branch.id}`
        });
      }
      return options;
    });
    common_vendor.onLoad((options) => {
      if (!utils_wxPerm.canManageShop()) {
        common_vendor.index.showToast({ title: "无店铺管理权限", icon: "none" });
        utils_navigation.navigateBackDelayed(800);
        return;
      }
      shopId.value = (options == null ? void 0 : options.shopId) ? String(options.shopId) : (options == null ? void 0 : options.id) ? String(options.id) : "";
      if (!shopId.value) {
        loading.value = false;
        common_vendor.index.showToast({ title: "缺少店铺 ID", icon: "none" });
        return;
      }
      void loadStatistics();
    });
    common_vendor.onReady(() => {
      const sys = common_vendor.index.getSystemInfoSync();
      canvasWidth.value = Math.max(280, (sys.windowWidth || 375) - 48);
    });
    function formatDate(date) {
      const y = date.getFullYear();
      const m = String(date.getMonth() + 1).padStart(2, "0");
      const d = String(date.getDate()).padStart(2, "0");
      return `${y}-${m}-${d}`;
    }
    function resolveDateRange() {
      const today = /* @__PURE__ */ new Date();
      today.setHours(0, 0, 0, 0);
      if (periodPreset.value === "custom") {
        if (Array.isArray(customDateRange.value) && customDateRange.value.length === 2) {
          return {
            startDate: customDateRange.value[0],
            endDate: customDateRange.value[1]
          };
        }
        return { startDate: todayStr.value, endDate: todayStr.value };
      }
      const dayMap = {
        today: 1,
        "3d": 3,
        "7d": 7,
        "30d": 30
      };
      const days = dayMap[periodPreset.value] || 1;
      const start = new Date(today);
      start.setDate(start.getDate() - (days - 1));
      return {
        startDate: formatDate(start),
        endDate: formatDate(today)
      };
    }
    function buildFilterShopId() {
      if (selectedScope.value.startsWith("self:")) {
        return shopId.value;
      }
      if (selectedScope.value.startsWith("shop:")) {
        return selectedScope.value.split(":")[1];
      }
      return null;
    }
    async function loadStatistics() {
      const { startDate, endDate } = resolveDateRange();
      const res = await api_modules_shop.fetchShopStatistics({
        shopId: shopId.value,
        filterShopId: buildFilterShopId(),
        startDate,
        endDate
      });
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "加载失败", icon: "none" });
        statistics.value = null;
        loading.value = false;
        refreshing.value = false;
        return;
      }
      statistics.value = res.data;
      loading.value = false;
      refreshing.value = false;
      await common_vendor.nextTick$1();
      drawTrendChart();
    }
    function switchScope(value) {
      if (selectedScope.value === value)
        return;
      selectedScope.value = value;
      loading.value = true;
      void loadStatistics();
    }
    function switchPeriod(value) {
      if (periodPreset.value === value)
        return;
      periodPreset.value = value;
      if (value !== "custom") {
        customDateRange.value = [];
        loading.value = true;
        void loadStatistics();
      }
    }
    function onCustomDateChange(value) {
      if (Array.isArray(value) && value.length === 2) {
        loading.value = true;
        void loadStatistics();
      }
    }
    function onRefresh() {
      refreshing.value = true;
      void loadStatistics();
    }
    function formatNumber(value) {
      const num = Number(value ?? 0);
      if (Number.isNaN(num))
        return "0";
      return Number.isInteger(num) ? String(num) : num.toFixed(0);
    }
    function formatMoney(value) {
      const num = Number(value ?? 0);
      if (Number.isNaN(num))
        return "0.00";
      return num.toFixed(2);
    }
    function drawTrendChart() {
      var _a;
      const trend = ((_a = statistics.value) == null ? void 0 : _a.trend) || [];
      if (!trend.length)
        return;
      const instance = common_vendor.getCurrentInstance();
      const ctx = common_vendor.index.createCanvasContext("statsTrendCanvas", instance == null ? void 0 : instance.proxy);
      const width = canvasWidth.value;
      const height = canvasHeight.value;
      const padding = { top: 16, right: 12, bottom: 28, left: 36 };
      const plotW = width - padding.left - padding.right;
      const plotH = height - padding.top - padding.bottom;
      const users = trend.map((item) => Number(item.newUsers ?? 0));
      const orders = trend.map((item) => Number(item.orders ?? 0));
      const points = trend.map((item) => Number(item.pointsUsed ?? 0));
      const maxVal = Math.max(1, ...users, ...orders, ...points);
      ctx.clearRect(0, 0, width, height);
      ctx.setStrokeStyle("#e8edf3");
      ctx.setLineWidth(1);
      for (let i = 0; i <= 4; i++) {
        const y = padding.top + plotH / 4 * i;
        ctx.beginPath();
        ctx.moveTo(padding.left, y);
        ctx.lineTo(width - padding.right, y);
        ctx.stroke();
      }
      const xStep = trend.length > 1 ? plotW / (trend.length - 1) : 0;
      const toY = (val) => padding.top + plotH - val / maxVal * plotH;
      function drawLine(data, color) {
        ctx.setStrokeStyle(color);
        ctx.setLineWidth(2);
        ctx.beginPath();
        data.forEach((val, idx) => {
          const x = padding.left + xStep * idx;
          const y = toY(val);
          if (idx === 0)
            ctx.moveTo(x, y);
          else
            ctx.lineTo(x, y);
        });
        ctx.stroke();
      }
      drawLine(users, "#007aff");
      drawLine(orders, "#34c759");
      drawLine(points, "#ff9500");
      ctx.setFillStyle("#8a94a6");
      ctx.setFontSize(10);
      trend.forEach((item, idx) => {
        const label = (item.date || "").slice(5);
        const x = padding.left + xStep * idx;
        ctx.fillText(label, x - 12, height - 8);
      });
      ctx.draw();
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: loading.value
      }, loading.value ? {} : common_vendor.e({
        b: common_vendor.f(periodOptions, (opt, k0, i0) => {
          return {
            a: common_vendor.t(opt.label),
            b: opt.value,
            c: periodPreset.value === opt.value ? 1 : "",
            d: common_vendor.o(($event) => switchPeriod(opt.value), opt.value)
          };
        }),
        c: periodPreset.value === "custom"
      }, periodPreset.value === "custom" ? {
        d: common_vendor.o(onCustomDateChange, "32"),
        e: common_vendor.o(($event) => customDateRange.value = $event, "e4"),
        f: common_vendor.p({
          type: "daterange",
          end: todayStr.value,
          modelValue: customDateRange.value
        })
      } : {}, {
        g: scopeOptions.value.length > 1
      }, scopeOptions.value.length > 1 ? {
        h: common_vendor.f(scopeOptions.value, (opt, k0, i0) => {
          return {
            a: common_vendor.t(opt.label),
            b: opt.value,
            c: selectedScope.value === opt.value ? 1 : "",
            d: common_vendor.o(($event) => switchScope(opt.value), opt.value)
          };
        })
      } : {}, {
        i: common_vendor.t(statTitles.value.newUsers),
        j: common_vendor.t(formatNumber(stats.value.newUsers)),
        k: common_vendor.t(statTitles.value.orders),
        l: common_vendor.t(formatNumber(stats.value.orders)),
        m: common_vendor.t(statTitles.value.pointsUsed),
        n: common_vendor.t(formatNumber(stats.value.pointsUsed)),
        o: common_vendor.t(statTitles.value.rechargeAmount),
        p: common_vendor.t(formatMoney(stats.value.rechargeAmount)),
        q: common_vendor.t(chartTitle.value),
        r: canvasWidth.value + "px",
        s: canvasHeight.value + "px"
      }), {
        t: refreshing.value,
        v: common_vendor.o(onRefresh, "54")
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-63602879"]]);
wx.createPage(MiniProgramPage);
