"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_user = require("../../api/modules/user.js");
const services_userSecurity = require("../../services/user-security.js");
const utils_navigation = require("../../utils/navigation.js");
if (!Math) {
  PayPinPad();
}
const PayPinPad = () => "../../components/pay/pay-pin-pad.js";
const MAX_PIN_ERRORS = 3;
const _sfc_main = {
  __name: "set-password",
  setup(__props) {
    const showPad = common_vendor.ref(false);
    const submitting = common_vendor.ref(false);
    const pinRef = common_vendor.ref(null);
    const pinError = common_vendor.ref("");
    const pinResetKey = common_vendor.ref(0);
    const isModify = common_vendor.ref(false);
    const stepIndex = common_vendor.ref(0);
    const oldPassword = common_vendor.ref("");
    const newPassword = common_vendor.ref("");
    let fromPay = false;
    const errorCount = common_vendor.ref(0);
    const steps = common_vendor.computed(
      () => isModify.value ? ["old", "new", "confirm"] : ["new", "confirm"]
    );
    const stepTitle = common_vendor.computed(() => {
      const step = steps.value[stepIndex.value];
      if (step === "old")
        return "请输入原支付密码";
      if (step === "new")
        return isModify.value ? "请输入新支付密码" : "请设置支付密码";
      return "请再次输入确认";
    });
    common_vendor.onLoad((query) => {
      fromPay = (query == null ? void 0 : query.from) === "pay";
      isModify.value = services_userSecurity.hasPayPasswordSet();
      if (fromPay && !isModify.value) {
        stepIndex.value = 0;
        errorCount.value = 0;
        pinError.value = "";
        showPad.value = true;
        return;
      }
      promptStart();
    });
    function promptStart() {
      const title = isModify.value ? "修改支付密码" : "设置支付密码";
      const content = isModify.value ? "是否修改支付密码？修改后请使用新密码支付。" : "是否设置 6 位数字支付密码？设置后可进行支付。";
      common_vendor.index.showModal({
        title,
        content,
        confirmText: "确定",
        cancelText: "取消",
        success: (res) => {
          if (res.confirm) {
            stepIndex.value = 0;
            errorCount.value = 0;
            pinError.value = "";
            showPad.value = true;
          } else {
            common_vendor.index.navigateBack();
          }
        },
        fail: () => common_vendor.index.navigateBack()
      });
    }
    function resetPinInput(errorMsg = "") {
      var _a;
      pinResetKey.value += 1;
      (_a = pinRef.value) == null ? void 0 : _a.reset();
      pinError.value = errorMsg;
    }
    function exitAfterTooManyErrors() {
      showPad.value = false;
      common_vendor.index.showModal({
        title: "提示",
        content: "错误次数过多，请稍后再试",
        showCancel: false,
        confirmText: "知道了",
        success: () => common_vendor.index.navigateBack(),
        fail: () => common_vendor.index.navigateBack()
      });
    }
    function recordPinError(msg) {
      errorCount.value += 1;
      if (errorCount.value >= MAX_PIN_ERRORS) {
        exitAfterTooManyErrors();
        return false;
      }
      const left = MAX_PIN_ERRORS - errorCount.value;
      const hint = left > 0 ? `${msg}（还可尝试 ${left} 次）` : msg;
      resetPinInput(hint);
      return true;
    }
    function nextStep() {
      resetPinInput("");
      if (stepIndex.value < steps.value.length - 1) {
        stepIndex.value += 1;
      } else {
        submitPassword();
      }
    }
    async function onPinComplete(digits) {
      if (!/^\d{6}$/.test(digits)) {
        recordPinError("请输入 6 位数字");
        return;
      }
      const step = steps.value[stepIndex.value];
      if (step === "old") {
        oldPassword.value = digits;
        nextStep();
        return;
      }
      if (step === "new") {
        if (isModify.value && digits === oldPassword.value) {
          recordPinError("新密码不能与原密码相同");
          return;
        }
        newPassword.value = digits;
        nextStep();
        return;
      }
      if (digits !== newPassword.value) {
        await common_vendor.nextTick$1();
        recordPinError("两次密码不一致，请重新输入");
        return;
      }
      submitPassword();
    }
    async function submitPassword() {
      if (submitting.value)
        return;
      submitting.value = true;
      try {
        const result = await api_modules_user.setPayPassword({
          oldPassword: isModify.value ? oldPassword.value : void 0,
          newPassword: newPassword.value
        });
        if (!result.ok) {
          const msg = result.msg || "设置失败";
          if (msg.includes("原密码")) {
            stepIndex.value = 0;
            oldPassword.value = "";
            newPassword.value = "";
          }
          recordPinError(msg);
          return;
        }
        services_userSecurity.setUserSecurity({ hasSetPassword: true });
        common_vendor.index.showToast({ title: isModify.value ? "修改成功" : "设置成功", icon: "success" });
        utils_navigation.navigateBackDelayed(350);
      } finally {
        submitting.value = false;
      }
    }
    function onCancel() {
      common_vendor.index.navigateBack();
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: showPad.value
      }, showPad.value ? {
        b: common_vendor.sr(pinRef, "e8192b7a-0", {
          "k": "pinRef"
        }),
        c: common_vendor.o(onPinComplete, "34"),
        d: common_vendor.o(onCancel, "bc"),
        e: common_vendor.p({
          title: stepTitle.value,
          ["error-hint"]: pinError.value,
          ["reset-key"]: pinResetKey.value,
          busy: submitting.value
        })
      } : {});
    };
  }
};
wx.createPage(_sfc_main);
