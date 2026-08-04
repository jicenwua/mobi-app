"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_couponManageDisplay = require("../../utils/coupon-manage-display.js");
const _sfc_main = {
  __name: "shop-manage-coupon-list",
  props: {
    coupons: { type: Array, default: () => [] },
    listLoading: { type: Boolean, default: false },
    isManager: { type: Boolean, default: false }
  },
  emits: ["edit", "resume", "add-stock", "delete"],
  setup(__props) {
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.listLoading
      }, __props.listLoading ? {} : !__props.coupons.length ? {} : {
        c: common_vendor.f(__props.coupons, (item, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(item.couponName),
            b: common_vendor.t(common_vendor.unref(utils_couponManageDisplay.couponTypeLabel)(item.type)),
            c: common_vendor.t(common_vendor.unref(utils_couponManageDisplay.formatCouponRule)(item)),
            d: common_vendor.t(common_vendor.unref(utils_couponManageDisplay.formatCouponIssued)(item)),
            e: common_vendor.t(common_vendor.unref(utils_couponManageDisplay.formatValidDays)(item)),
            f: common_vendor.t(common_vendor.unref(utils_couponManageDisplay.couponStatusLabel)(item)),
            g: common_vendor.n(common_vendor.unref(utils_couponManageDisplay.couponPhaseTagClass)(common_vendor.unref(utils_couponManageDisplay.couponStatusCode)(item))),
            h: common_vendor.t(common_vendor.unref(utils_couponManageDisplay.formatCouponDistributionPeriod)(item.distributionStartTime, item.distributionEndTime))
          }, __props.isManager ? common_vendor.e({
            i: common_vendor.unref(utils_couponManageDisplay.canEditCoupon)(item)
          }, common_vendor.unref(utils_couponManageDisplay.canEditCoupon)(item) ? {
            j: common_vendor.o(($event) => _ctx.$emit("edit", item), item.templateId)
          } : {}, {
            k: common_vendor.unref(utils_couponManageDisplay.canResumeCouponDistribution)(item)
          }, common_vendor.unref(utils_couponManageDisplay.canResumeCouponDistribution)(item) ? {
            l: common_vendor.o(($event) => _ctx.$emit("resume", item), item.templateId)
          } : {}, {
            m: common_vendor.o(($event) => _ctx.$emit("add-stock", item), item.templateId),
            n: common_vendor.o(($event) => _ctx.$emit("delete", item), item.templateId)
          }) : {}, {
            o: item.templateId
          });
        }),
        d: __props.isManager
      }, {
        b: !__props.coupons.length
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-89e92006"]]);
wx.createComponent(Component);
