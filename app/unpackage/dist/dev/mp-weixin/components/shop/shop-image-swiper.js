"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "shop-image-swiper",
  props: {
    images: { type: Array, default: () => [] },
    height: { type: String, default: "180px" },
    isDark: { type: Boolean, default: false },
    /** 卡片内紧凑展示：隐藏左右切换按钮 */
    compact: { type: Boolean, default: false },
    /** 多图时是否自动轮播 */
    autoplay: { type: Boolean, default: true },
    /** 自动轮播间隔（毫秒） */
    interval: { type: Number, default: 3500 },
    /** 切换动画时长（毫秒） */
    duration: { type: Number, default: 400 }
  },
  setup(__props) {
    const props = __props;
    const shouldAutoplay = common_vendor.computed(() => props.autoplay && props.images.length > 1);
    const current = common_vendor.ref(0);
    common_vendor.watch(
      () => {
        var _a;
        return ((_a = props.images) == null ? void 0 : _a.length) ?? 0;
      },
      () => {
        current.value = 0;
      }
    );
    function onSwiperChange(e) {
      var _a;
      current.value = ((_a = e.detail) == null ? void 0 : _a.current) ?? 0;
    }
    function goPrev() {
      const n = props.images.length;
      if (n <= 1)
        return;
      current.value = current.value <= 0 ? n - 1 : current.value - 1;
    }
    function goNext() {
      const n = props.images.length;
      if (n <= 1)
        return;
      current.value = current.value >= n - 1 ? 0 : current.value + 1;
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.images.length
      }, __props.images.length ? {
        b: common_vendor.f(__props.images, (src, idx, i0) => {
          return {
            a: src,
            b: idx
          };
        }),
        c: current.value,
        d: __props.images.length > 1,
        e: __props.images.length > 1,
        f: shouldAutoplay.value,
        g: __props.interval,
        h: __props.duration,
        i: common_vendor.o(onSwiperChange, "83")
      } : {}, {
        j: __props.images.length > 1 && !__props.compact
      }, __props.images.length > 1 && !__props.compact ? {
        k: common_vendor.o(goPrev, "e6"),
        l: common_vendor.o(goNext, "28")
      } : {}, {
        m: common_vendor.n(__props.isDark ? "shop-image-swiper--dark" : "shop-image-swiper--light"),
        n: common_vendor.n(__props.compact ? "shop-image-swiper--compact" : ""),
        o: __props.height
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1dddcaeb"]]);
wx.createComponent(Component);
