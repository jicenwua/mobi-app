"use strict";
const common_vendor = require("../../common/vendor.js");
const __default__ = {
  options: {
    virtualHost: true
  }
};
const _sfc_main = /* @__PURE__ */ Object.assign(__default__, {
  __name: "main-mine-panel",
  props: {
    userProfile: {
      type: Object,
      required: true
    },
    displayNickname: {
      type: String,
      required: true
    },
    profileSubText: {
      type: String,
      required: true
    },
    couponCount: {
      type: Number,
      required: true
    },
    payPasswordLabel: {
      type: String,
      required: true
    },
    ticketUnreadCount: {
      type: Number,
      default: 0
    },
    loginError: {
      type: String,
      default: ""
    },
    isDark: {
      type: Boolean,
      required: true
    }
  },
  emits: ["profile", "coupons", "pay-qrcode", "set-password", "tickets"],
  setup(__props, { emit: __emit }) {
    const props = __props;
    const emit = __emit;
    const unreadBadgeText = common_vendor.computed(() => {
      const count = props.ticketUnreadCount;
      if (count > 99)
        return "99+";
      return String(count);
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.userProfile.avatar,
        b: common_vendor.t(__props.displayNickname),
        c: common_vendor.t(__props.profileSubText),
        d: common_vendor.o(($event) => emit("profile"), "c2"),
        e: common_vendor.t(__props.couponCount),
        f: common_vendor.o(($event) => emit("coupons"), "67"),
        g: common_vendor.o(($event) => emit("pay-qrcode"), "38"),
        h: common_vendor.t(__props.payPasswordLabel),
        i: common_vendor.o(($event) => emit("set-password"), "00"),
        j: __props.ticketUnreadCount > 0
      }, __props.ticketUnreadCount > 0 ? {
        k: common_vendor.t(unreadBadgeText.value)
      } : {}, {
        l: common_vendor.o(($event) => emit("tickets"), "c3"),
        m: __props.loginError
      }, __props.loginError ? {
        n: common_vendor.t(__props.loginError)
      } : {}, {
        o: common_vendor.n(__props.isDark ? "theme-dark" : "theme-light")
      });
    };
  }
});
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-140fca43"]]);
wx.createComponent(Component);
