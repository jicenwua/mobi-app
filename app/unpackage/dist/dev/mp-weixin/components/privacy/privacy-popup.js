"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_wxPrivacy = require("../../utils/wx-privacy.js");
const _sfc_main = {
  __name: "privacy-popup",
  setup(__props, { expose: __expose }) {
    const contractName = common_vendor.ref("用户隐私保护指引");
    const contractLabel = common_vendor.computed(() => utils_wxPrivacy.formatPrivacyContractLabel(contractName.value));
    const showPrivacyDialog = () => {
      utils_wxPrivacy.privacyPopupVisible.value = true;
    };
    __expose({
      showPrivacyDialog
    });
    common_vendor.watch(
      utils_wxPrivacy.privacyPopupVisible,
      (visible) => {
        if (!visible)
          return;
        utils_wxPrivacy.getPrivacySettingState().then((state) => {
          if (state.privacyContractName) {
            contractName.value = state.privacyContractName;
          }
        });
      },
      { immediate: true }
    );
    function handleAgree() {
      utils_wxPrivacy.onPrivacyAgree();
    }
    function handleDisagree() {
      utils_wxPrivacy.onPrivacyDisagree();
      common_vendor.index.showModal({
        title: "提示",
        content: "您需要同意隐私政策后才能使用本小程序",
        showCancel: false,
        confirmText: "退出",
        success: () => {
          common_vendor.index.exitMiniProgram();
        }
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.unref(utils_wxPrivacy.privacyPopupVisible)
      }, common_vendor.unref(utils_wxPrivacy.privacyPopupVisible) ? {
        b: common_vendor.t(contractLabel.value),
        c: common_vendor.o((...args) => common_vendor.unref(utils_wxPrivacy.openPrivacyContract) && common_vendor.unref(utils_wxPrivacy.openPrivacyContract)(...args), "ee"),
        d: common_vendor.o(handleDisagree, "88"),
        e: common_vendor.o(handleAgree, "e3"),
        f: common_vendor.o(() => {
        }, "28"),
        g: common_vendor.o(() => {
        }, "2d")
      } : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-25393fda"]]);
wx.createComponent(Component);
