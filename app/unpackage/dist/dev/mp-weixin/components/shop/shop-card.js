"use strict";
const common_vendor = require("../../common/vendor.js");
const composables_useExpiringQrcode = require("../../composables/use-expiring-qrcode.js");
const api_modules_shop = require("../../api/modules/shop.js");
const utils_pointsFormat = require("../../utils/points-format.js");
const api_modules_qrcode = require("../../api/modules/qrcode.js");
const utils_wxPerm = require("../../utils/wx-perm.js");
const utils_staffScan = require("../../utils/staff-scan.js");
const utils_shopPageContext = require("../../utils/shop-page-context.js");
const utils_shopRole = require("../../utils/shop-role.js");
if (!Array) {
  const _easycom_uni_icons2 = common_vendor.resolveComponent("uni-icons");
  _easycom_uni_icons2();
}
const _easycom_uni_icons = () => "../../uni_modules/uni-icons/components/uni-icons/uni-icons.js";
if (!Math) {
  (_easycom_uni_icons + ShopImageSwiper + QrcodeCanvas)();
}
const ShopImageSwiper = () => "./shop-image-swiper.js";
const QrcodeCanvas = () => "../common/qrcode-canvas.js";
const SHOP_NAME_MAX_LEN = 10;
const _sfc_main = {
  __name: "shop-card",
  props: {
    shop: { type: Object, required: true },
    isDark: { type: Boolean, default: false },
    /** 会员页展示剩余积分 */
    showPoints: { type: Boolean, default: false },
    /** 主屏内嵌详情，不跳转子页面 */
    embedDetail: { type: Boolean, default: false }
  },
  emits: ["open-detail"],
  setup(__props, { emit: __emit }) {
    const props = __props;
    const emit = __emit;
    const qrcodeVisible = common_vendor.ref(false);
    const qrcodeLoading = common_vendor.ref(false);
    const shopQrcodeImage = common_vendor.ref("");
    const shopQrcodeFallback = common_vendor.ref("");
    const shopQrcodeExpireAt = common_vendor.ref(0);
    async function refreshShopQrcode() {
      var _a, _b, _c, _d;
      const id = (_a = props.shop) == null ? void 0 : _a.id;
      if (!id || !qrcodeVisible.value)
        return;
      qrcodeLoading.value = true;
      shopQrcodeImage.value = "";
      shopQrcodeFallback.value = "";
      stopQrcodeCountdown();
      qrcodeCountdownLeft.value = 0;
      const res = await api_modules_qrcode.fetchShopQrcode(id);
      qrcodeLoading.value = false;
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "生成失败", icon: "none" });
        return;
      }
      if ((_b = res.data) == null ? void 0 : _b.imageBase64) {
        shopQrcodeImage.value = res.data.imageBase64;
      }
      if ((_c = res.data) == null ? void 0 : _c.inviteContent) {
        shopQrcodeFallback.value = res.data.inviteContent;
      }
      shopQrcodeExpireAt.value = ((_d = res.data) == null ? void 0 : _d.expireAt) || Date.now() + 12e4;
      syncQrcodeCountdownFromExpireAt(shopQrcodeExpireAt.value);
    }
    const {
      countdownLeft: qrcodeCountdownLeft,
      stopCountdown: stopQrcodeCountdown,
      syncFromExpireAt: syncQrcodeCountdownFromExpireAt
    } = composables_useExpiringQrcode.useExpiringQrcode(() => {
      if (qrcodeVisible.value) {
        void refreshShopQrcode();
      }
    });
    const displayShopName = common_vendor.computed(() => {
      var _a;
      const raw = (((_a = props.shop) == null ? void 0 : _a.shopName) || "").trim() || "未命名店铺";
      if (raw.length <= SHOP_NAME_MAX_LEN)
        return raw;
      return `${raw.slice(0, SHOP_NAME_MAX_LEN)}…`;
    });
    const roleLabel = common_vendor.computed(() => utils_shopRole.getShopRoleLabel(props.shop));
    const roleTagClass = common_vendor.computed(() => {
      const type = utils_shopRole.getShopRoleTagType(props.shop);
      return type ? `shop-role-tag--${type}` : "";
    });
    const actionIconColor = common_vendor.computed(() => props.isDark ? "#5ac8fa" : "#007aff");
    const remainingPointsText = common_vendor.computed(() => {
      var _a;
      const n = (_a = props.shop) == null ? void 0 : _a.remainingPoints;
      if (n == null || n === "")
        return "0.00";
      return utils_pointsFormat.formatPointsAmount(n);
    });
    const images = common_vendor.computed(() => api_modules_shop.getShopCarouselImages(props.shop));
    const showDetailLink = common_vendor.computed(() => utils_wxPerm.canShowMemberTab());
    const showManageLink = common_vendor.computed(
      () => utils_shopRole.isShopClerkCapable(props.shop) && utils_wxPerm.canManageShop()
    );
    const showScanVerifyBtn = common_vendor.computed(() => utils_wxPerm.canStaffVerifyAtShop(props.shop));
    const showInviteQrcodeBtn = common_vendor.computed(() => utils_wxPerm.canPreviewShopByCode());
    const qrcodeCanvasId = common_vendor.computed(() => {
      var _a;
      return `shop-qrcode-card-${((_a = props.shop) == null ? void 0 : _a.id) ?? "x"}`;
    });
    const qrcodeExpireHint = common_vendor.computed(() => {
      if (!qrcodeCountdownLeft.value)
        return "";
      return `${qrcodeCountdownLeft.value} 秒后自动刷新`;
    });
    const addressText = common_vendor.computed(() => api_modules_shop.formatShopAddress(props.shop) || "地址未填写");
    const codeMaskText = common_vendor.computed(() => {
      var _a;
      const code = (((_a = props.shop) == null ? void 0 : _a.shopCode) || "").trim();
      if (!code)
        return "••••••••";
      if (code.length <= 4)
        return "••••";
      return `${code.slice(0, 2)}${"•".repeat(Math.min(6, code.length - 4))}${code.slice(-2)}`;
    });
    const auditLabel = common_vendor.computed(() => {
      var _a;
      const s = (_a = props.shop) == null ? void 0 : _a.auditStatus;
      if (s === 0)
        return "待审核";
      if (s === 1)
        return "已通过";
      if (s === 2)
        return "已驳回";
      return "";
    });
    const auditClass = common_vendor.computed(() => {
      var _a;
      const s = (_a = props.shop) == null ? void 0 : _a.auditStatus;
      if (s === 0)
        return "shop-audit-tag--pending";
      if (s === 1)
        return "shop-audit-tag--ok";
      if (s === 2)
        return "shop-audit-tag--reject";
      return "";
    });
    async function openShopQrcode() {
      var _a;
      const id = (_a = props.shop) == null ? void 0 : _a.id;
      if (!id) {
        common_vendor.index.showToast({ title: "店铺信息无效", icon: "none" });
        return;
      }
      qrcodeVisible.value = true;
      await refreshShopQrcode();
    }
    function scanForVerify() {
      var _a;
      const id = (_a = props.shop) == null ? void 0 : _a.id;
      if (!id || !showScanVerifyBtn.value)
        return;
      utils_staffScan.scanStaffVerifyCode({ shopId: id });
    }
    function closeShopQrcode() {
      qrcodeVisible.value = false;
      stopQrcodeCountdown();
      qrcodeCountdownLeft.value = 0;
    }
    function goDetail() {
      if (!showDetailLink.value) {
        common_vendor.index.showToast({ title: "无查看详情权限", icon: "none" });
        return;
      }
      const item = props.shop;
      const id = item == null ? void 0 : item.id;
      if (!id) {
        common_vendor.index.showToast({ title: "店铺信息无效", icon: "none" });
        return;
      }
      api_modules_shop.reportShopEnterTimeAsync(id);
      if (props.embedDetail) {
        emit("open-detail", item);
        return;
      }
      utils_shopPageContext.navigateWithShop({
        url: `/pages/member/shop-detail?id=${id}`,
        shop: item
      });
    }
    function goManage() {
      if (!showManageLink.value) {
        common_vendor.index.showToast({ title: "无店铺管理权限", icon: "none" });
        return;
      }
      const item = props.shop;
      const id = item == null ? void 0 : item.id;
      if (!id) {
        common_vendor.index.showToast({ title: "店铺信息无效", icon: "none" });
        return;
      }
      utils_shopPageContext.navigateWithShop({
        url: `/pages/shop/manage?id=${id}`,
        shop: item
      });
    }
    function onCopyCode() {
      var _a;
      const code = (((_a = props.shop) == null ? void 0 : _a.shopCode) || "").trim();
      if (!code) {
        common_vendor.index.showToast({ title: "暂无店铺代码", icon: "none" });
        return;
      }
      common_vendor.index.setClipboardData({
        data: code,
        success: () => {
          common_vendor.index.showToast({ title: "店铺代码已复制", icon: "success" });
        },
        fail: () => {
          common_vendor.index.showToast({ title: "复制失败", icon: "none" });
        }
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.t(displayShopName.value),
        b: roleLabel.value
      }, roleLabel.value ? {
        c: common_vendor.t(roleLabel.value),
        d: common_vendor.n(roleTagClass.value)
      } : {}, {
        e: showScanVerifyBtn.value
      }, showScanVerifyBtn.value ? {
        f: common_vendor.p({
          type: "scan",
          size: 17,
          color: actionIconColor.value
        }),
        g: common_vendor.o(scanForVerify, "21")
      } : {}, {
        h: showInviteQrcodeBtn.value
      }, showInviteQrcodeBtn.value ? {
        i: common_vendor.o(openShopQrcode, "40")
      } : {}, {
        j: showManageLink.value
      }, showManageLink.value ? {
        k: common_vendor.o(goManage, "9a")
      } : {}, {
        l: showDetailLink.value
      }, showDetailLink.value ? {
        m: common_vendor.t("详情 >>"),
        n: common_vendor.o(goDetail, "83")
      } : {}, {
        o: common_vendor.o(() => {
        }, "c0"),
        p: common_vendor.p({
          images: images.value,
          ["is-dark"]: __props.isDark,
          height: "100%",
          compact: true,
          autoplay: true
        }),
        q: common_vendor.o(() => {
        }, "b8"),
        r: common_vendor.t(codeMaskText.value),
        s: common_vendor.o(onCopyCode, "90"),
        t: common_vendor.t(addressText.value),
        v: __props.showPoints
      }, __props.showPoints ? {
        w: common_vendor.t(remainingPointsText.value)
      } : {}, {
        x: auditLabel.value
      }, auditLabel.value ? {
        y: common_vendor.t(auditLabel.value),
        z: common_vendor.n(auditClass.value)
      } : {}, {
        A: qrcodeVisible.value
      }, qrcodeVisible.value ? common_vendor.e({
        B: qrcodeLoading.value
      }, qrcodeLoading.value ? {} : shopQrcodeImage.value || shopQrcodeFallback.value ? common_vendor.e({
        D: shopQrcodeImage.value
      }, shopQrcodeImage.value ? {
        E: shopQrcodeImage.value
      } : {
        F: common_vendor.p({
          text: shopQrcodeFallback.value,
          size: 200,
          ["canvas-id"]: qrcodeCanvasId.value
        })
      }, {
        G: qrcodeExpireHint.value
      }, qrcodeExpireHint.value ? {
        H: common_vendor.t(qrcodeExpireHint.value)
      } : {}) : {}, {
        C: shopQrcodeImage.value || shopQrcodeFallback.value,
        I: common_vendor.n(__props.isDark ? "modal-close--dark" : ""),
        J: common_vendor.o(closeShopQrcode, "10"),
        K: common_vendor.n(__props.isDark ? "modal-panel--dark" : ""),
        L: common_vendor.o(() => {
        }, "81"),
        M: common_vendor.o(closeShopQrcode, "f6")
      }) : {}, {
        N: common_vendor.n(__props.isDark ? "shop-card--dark" : "shop-card--light"),
        O: showDetailLink.value ? "tap-hover-opacity" : "",
        P: common_vendor.o(goDetail, "f9")
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-02d94146"]]);
wx.createComponent(Component);
