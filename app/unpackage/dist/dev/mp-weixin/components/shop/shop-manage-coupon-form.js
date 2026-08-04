"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_couponManageDisplay = require("../../utils/coupon-manage-display.js");
const _sfc_main = {
  __name: "shop-manage-coupon-form",
  props: {
    visible: { type: Boolean, default: false },
    editingCouponId: { type: [String, Number], default: null },
    editingCouponItem: { type: Object, default: null },
    form: { type: Object, required: true },
    couponTypeLabels: { type: Array, default: () => [] },
    couponTypeIndex: { type: Number, default: 0 }
  },
  emits: [
    "close",
    "submit",
    "coupon-type-change",
    "never-expire-change",
    "stop-distribution",
    "resume-distribution"
  ],
  setup(__props) {
    const props = __props;
    const formatCouponRuleFromForm = common_vendor.computed(
      () => utils_couponManageDisplay.formatCouponRule({
        type: props.form.type,
        thresholdAmount: props.form.thresholdAmount,
        discountValue: props.form.discountValue
      })
    );
    const formatValidDaysFromForm = common_vendor.computed(() => {
      if (props.form.neverExpire)
        return "领取后永久";
      return utils_couponManageDisplay.formatValidDays(props.form);
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.visible
      }, __props.visible ? common_vendor.e({
        b: common_vendor.t(__props.editingCouponId ? "修改折扣券" : "添加折扣券"),
        c: common_vendor.t(__props.editingCouponId ? "创建后不可修改优惠规则，仅可调整发放时间" : "设置优惠规则与发放计划"),
        d: __props.editingCouponId
      }, __props.editingCouponId ? {
        e: common_vendor.t(__props.form.couponName),
        f: common_vendor.t(__props.couponTypeLabels[__props.couponTypeIndex]),
        g: common_vendor.t(formatCouponRuleFromForm.value),
        h: common_vendor.t(formatValidDaysFromForm.value)
      } : {
        i: __props.form.couponName,
        j: common_vendor.o(($event) => __props.form.couponName = $event.detail.value, "bf"),
        k: common_vendor.t(__props.couponTypeLabels[__props.couponTypeIndex]),
        l: __props.couponTypeLabels,
        m: __props.couponTypeIndex,
        n: common_vendor.o(($event) => _ctx.$emit("coupon-type-change", $event), "f1")
      }, {
        o: !__props.editingCouponId
      }, !__props.editingCouponId ? {
        p: __props.form.thresholdAmount,
        q: common_vendor.o(($event) => __props.form.thresholdAmount = $event.detail.value, "26"),
        r: common_vendor.t(__props.form.type === 1 ? "折扣力度" : "减免积分"),
        s: common_vendor.t(__props.form.type === 1 ? "如 85 表示 8.5 折" : "固定减免积分数"),
        t: __props.form.type === 1 ? "85" : "20",
        v: __props.form.discountValue,
        w: common_vendor.o(($event) => __props.form.discountValue = $event.detail.value, "d1"),
        x: common_vendor.t(__props.form.type === 1 ? "折" : "积分")
      } : {}, {
        y: common_vendor.t(__props.editingCouponId ? "发放设置" : "时间与发放"),
        z: !__props.editingCouponId
      }, !__props.editingCouponId ? common_vendor.e({
        A: __props.form.neverExpire,
        B: common_vendor.o(($event) => _ctx.$emit("never-expire-change", $event), "ad"),
        C: __props.form.neverExpire ? 1 : "",
        D: !__props.form.neverExpire
      }, !__props.form.neverExpire ? {
        E: __props.form.validDays,
        F: common_vendor.o(($event) => __props.form.validDays = $event.detail.value, "56")
      } : {}) : {}, {
        G: !__props.editingCouponId
      }, !__props.editingCouponId ? {
        H: __props.form.totalQuantity,
        I: common_vendor.o(($event) => __props.form.totalQuantity = $event.detail.value, "85")
      } : {}, {
        J: common_vendor.t(__props.form.distributionStartDate || "立即开始"),
        K: !__props.form.distributionStartDate ? 1 : "",
        L: __props.form.distributionStartDate,
        M: common_vendor.o(($event) => __props.form.distributionStartDate = $event.detail.value, "f7"),
        N: common_vendor.t(__props.form.distributionEndDate || "永久有效"),
        O: !__props.form.distributionEndDate ? 1 : "",
        P: __props.form.distributionEndDate,
        Q: common_vendor.o(($event) => __props.form.distributionEndDate = $event.detail.value, "2a"),
        R: __props.form.distributionStartDate || __props.form.distributionEndDate
      }, __props.form.distributionStartDate || __props.form.distributionEndDate ? common_vendor.e({
        S: __props.form.distributionStartDate
      }, __props.form.distributionStartDate ? {
        T: common_vendor.o(($event) => __props.form.distributionStartDate = "", "01")
      } : {}, {
        U: __props.form.distributionEndDate
      }, __props.form.distributionEndDate ? {
        V: common_vendor.o(($event) => __props.form.distributionEndDate = "", "35")
      } : {}) : {}, {
        W: common_vendor.o(($event) => _ctx.$emit("close"), "c2"),
        X: __props.editingCouponId && common_vendor.unref(utils_couponManageDisplay.canStopCouponDistribution)(__props.editingCouponItem)
      }, __props.editingCouponId && common_vendor.unref(utils_couponManageDisplay.canStopCouponDistribution)(__props.editingCouponItem) ? {
        Y: common_vendor.o(($event) => _ctx.$emit("stop-distribution"), "4f")
      } : {}, {
        Z: __props.editingCouponId && common_vendor.unref(utils_couponManageDisplay.canResumeCouponDistribution)(__props.editingCouponItem)
      }, __props.editingCouponId && common_vendor.unref(utils_couponManageDisplay.canResumeCouponDistribution)(__props.editingCouponItem) ? {
        aa: common_vendor.o(($event) => _ctx.$emit("resume-distribution"), "14")
      } : {}, {
        ab: common_vendor.t(__props.editingCouponId ? "保存修改" : "创建折扣券"),
        ac: common_vendor.o(($event) => _ctx.$emit("submit"), "61"),
        ad: common_vendor.o(() => {
        }, "25"),
        ae: common_vendor.o(() => {
        }, "34"),
        af: common_vendor.o(($event) => _ctx.$emit("close"), "32")
      }) : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-d11eac74"]]);
wx.createComponent(Component);
