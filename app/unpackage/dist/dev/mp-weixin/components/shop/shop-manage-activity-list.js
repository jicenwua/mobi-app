"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_datetimeFormat = require("../../utils/datetime-format.js");
const _sfc_main = {
  __name: "shop-manage-activity-list",
  props: {
    activities: { type: Array, default: () => [] },
    listLoading: { type: Boolean, default: false },
    isManager: { type: Boolean, default: false },
    isDark: { type: Boolean, default: false }
  },
  emits: ["edit", "delete", "add", "stop", "enable"],
  setup(__props, { emit: __emit }) {
    const emit = __emit;
    function activityTypeLabel(item) {
      if ((item == null ? void 0 : item.kind) === 2)
        return "公告";
      if ((item == null ? void 0 : item.activityType) === 1)
        return "充值满赠";
      if ((item == null ? void 0 : item.activityType) === 2)
        return "消费满赠";
      return "—";
    }
    function formatActivityPeriod(startTime, endTime) {
      const start = startTime ? utils_datetimeFormat.formatDateTime(startTime, { maxLen: 10 }) : "立即开始";
      const end = endTime ? utils_datetimeFormat.formatDateTime(endTime, { maxLen: 10 }) : "永久";
      return `${start} ~ ${end}`;
    }
    function activityStatusLabel(status) {
      if (status === 0)
        return "未开始";
      if (status === 1)
        return "进行中";
      if (status === 2)
        return "已结束";
      if (status === 3)
        return "手动停止";
      return "—";
    }
    function activityStatusCode(item) {
      const status = item == null ? void 0 : item.status;
      if (status === 0 || status === 1 || status === 2 || status === 3)
        return status;
      return null;
    }
    function statusTagClass(code) {
      if (code === 0)
        return "status-tag--pending";
      if (code === 1)
        return "status-tag--active";
      if (code === 2)
        return "status-tag--ended";
      if (code === 3)
        return "status-tag--stopped";
      return "status-tag--unknown";
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.listLoading
      }, __props.listLoading ? {} : !__props.activities.length ? {} : {
        c: common_vendor.f(__props.activities, (item, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(item.activityName),
            b: common_vendor.t(activityTypeLabel(item)),
            c: common_vendor.t(formatActivityPeriod(item.startTime, item.endTime)),
            d: common_vendor.t(activityStatusLabel(item.status)),
            e: common_vendor.n(statusTagClass(activityStatusCode(item)))
          }, __props.isManager ? common_vendor.e({
            f: common_vendor.o(($event) => emit("edit", item), item.activityId),
            g: item.status === 0 || item.status === 1
          }, item.status === 0 || item.status === 1 ? {
            h: common_vendor.o(($event) => emit("stop", item), item.activityId)
          } : {}, {
            i: item.status === 3
          }, item.status === 3 ? {
            j: common_vendor.o(($event) => emit("enable", item), item.activityId)
          } : {}, {
            k: common_vendor.o(($event) => emit("delete", item), item.activityId)
          }) : {}, {
            l: item.activityId
          });
        }),
        d: __props.isManager,
        e: __props.isDark ? 1 : ""
      }, {
        b: !__props.activities.length
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-f33728f9"]]);
wx.createComponent(Component);
