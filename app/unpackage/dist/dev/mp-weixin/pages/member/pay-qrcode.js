"use strict";
const common_vendor = require("../../common/vendor.js");
const composables_useExpiringQrcode = require("../../composables/use-expiring-qrcode.js");
const api_modules_qrcode = require("../../api/modules/qrcode.js");
const api_modules_user = require("../../api/modules/user.js");
const services_payment = require("../../services/payment.js");
const utils_wxPerm = require("../../utils/wx-perm.js");
const services_userSecurity = require("../../services/user-security.js");
const utils_navigation = require("../../utils/navigation.js");
const composables_useTheme = require("../../composables/use-theme.js");
if (!Math) {
  (PayPinPad + QrcodeCanvas)();
}
const QrcodeCanvas = () => "../../components/common/qrcode-canvas.js";
const PayPinPad = () => "../../components/pay/pay-pin-pad.js";
const MAX_PIN_ERRORS = 3;
const _sfc_main = {
  __name: "pay-qrcode",
  setup(__props) {
    const { isDark, loadTheme } = composables_useTheme.useTheme();
    const qrContent = common_vendor.ref("");
    const loading = common_vendor.ref(false);
    const errorMsg = common_vendor.ref("");
    const showPinPad = common_vendor.ref(false);
    const pinError = common_vendor.ref("");
    const pinResetKey = common_vendor.ref(0);
    const pinVerifying = common_vendor.ref(false);
    const passwordVerified = common_vendor.ref(false);
    let passwordGateDone = false;
    let skipPinOnce = false;
    let pinErrorCount = 0;
    const {
      countdownLeft,
      expireAt,
      stopCountdown,
      syncFromExpireAt: syncCountdownFromExpireAt
    } = composables_useExpiringQrcode.useExpiringQrcode(() => {
      void refresh();
    });
    const expireHint = common_vendor.computed(() => {
      if (!qrContent.value || countdownLeft.value <= 0)
        return "";
      return `${countdownLeft.value} 秒后自动刷新`;
    });
    common_vendor.onLoad(() => {
      if (!utils_wxPerm.canShowMemberTab()) {
        common_vendor.index.showToast({ title: "无付款码权限", icon: "none" });
        utils_navigation.navigateBackDelayed(800);
        return;
      }
      loadTheme();
      startPayQrcode();
    });
    common_vendor.onShow(() => {
      if (!passwordGateDone)
        return;
      if (skipPinOnce && services_userSecurity.hasPayPasswordSet()) {
        skipPinOnce = false;
        passwordVerified.value = true;
        showPinPad.value = false;
        beginQrcodeRefresh();
        return;
      }
      if (passwordVerified.value && qrContent.value) {
        syncCountdownFromExpireAt(expireAt.value);
      }
    });
    common_vendor.onHide(() => {
      stopCountdown();
    });
    common_vendor.onUnload(() => {
      stopCountdown();
    });
    function openPinPad() {
      pinError.value = "";
      pinErrorCount = 0;
      showPinPad.value = true;
    }
    async function startPayQrcode() {
      const guard = await services_payment.guardPayPassword();
      passwordGateDone = true;
      if (!guard.ok) {
        if (guard.navigatedToSet) {
          skipPinOnce = true;
        } else {
          common_vendor.index.navigateBack();
        }
        return;
      }
      openPinPad();
    }
    async function onPinComplete(digits) {
      if (!/^\d{6}$/.test(digits)) {
        recordPinError("请输入 6 位数字");
        return;
      }
      pinVerifying.value = true;
      const res = await api_modules_user.verifyPayPassword(digits);
      pinVerifying.value = false;
      if (!res.ok) {
        if (res.type === "forbidden") {
          showPinPad.value = false;
          common_vendor.index.showModal({
            title: "无法校验密码",
            content: res.msg || "当前账号无权限校验支付密码，请联系管理员",
            showCancel: false,
            success: () => common_vendor.index.navigateBack(),
            fail: () => common_vendor.index.navigateBack()
          });
          return;
        }
        if (res.type === "network") {
          showPinPad.value = false;
          common_vendor.index.showToast({ title: res.msg || "网络错误", icon: "none" });
          return;
        }
        recordPinError(res.msg || "支付密码错误");
        return;
      }
      passwordVerified.value = true;
      showPinPad.value = false;
      await beginQrcodeRefresh();
    }
    function recordPinError(msg) {
      pinErrorCount += 1;
      if (pinErrorCount >= MAX_PIN_ERRORS) {
        showPinPad.value = false;
        common_vendor.index.showModal({
          title: "提示",
          content: "错误次数过多，请稍后再试",
          showCancel: false,
          success: () => common_vendor.index.navigateBack(),
          fail: () => common_vendor.index.navigateBack()
        });
        return;
      }
      const left = MAX_PIN_ERRORS - pinErrorCount;
      pinResetKey.value += 1;
      pinError.value = `${msg}（还可尝试 ${left} 次）`;
    }
    function onPinCancel() {
      common_vendor.index.navigateBack();
    }
    async function beginQrcodeRefresh() {
      stopCountdown();
      await refresh();
    }
    async function refresh() {
      var _a;
      if (loading.value || !passwordVerified.value)
        return;
      loading.value = true;
      errorMsg.value = "";
      const res = await api_modules_qrcode.generatePayQrcode();
      loading.value = false;
      if (!res.ok || !((_a = res.data) == null ? void 0 : _a.qrContent)) {
        errorMsg.value = res.msg || "生成失败";
        qrContent.value = "";
        countdownLeft.value = 0;
        stopCountdown();
        return;
      }
      qrContent.value = res.data.qrContent;
      expireAt.value = res.data.expireAt || Date.now() + 12e4;
      syncCountdownFromExpireAt(expireAt.value);
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: showPinPad.value
      }, showPinPad.value ? {
        b: common_vendor.o(onPinComplete, "a3"),
        c: common_vendor.o(onPinCancel, "fd"),
        d: common_vendor.p({
          title: "请输入支付密码",
          ["error-hint"]: pinError.value,
          ["reset-key"]: pinResetKey.value,
          busy: pinVerifying.value
        })
      } : common_vendor.e({
        e: loading.value
      }, loading.value ? {} : qrContent.value ? {
        g: common_vendor.p({
          text: qrContent.value,
          size: 220,
          ["canvas-id"]: "pay-qrcode"
        }),
        h: common_vendor.t(expireHint.value)
      } : {
        i: common_vendor.t(errorMsg.value || "无法生成付款码")
      }, {
        f: qrContent.value,
        j: loading.value ? 1 : "",
        k: common_vendor.o(refresh, "31")
      }), {
        l: common_vendor.n(common_vendor.unref(isDark) ? "page--dark" : "page--light")
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-a1939bdf"]]);
wx.createPage(MiniProgramPage);
