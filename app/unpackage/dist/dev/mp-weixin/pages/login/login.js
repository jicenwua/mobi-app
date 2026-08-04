"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
require("../../utils/crypto-gateway.js");
const api_modules_authToken = require("../../api/modules/auth-token.js");
const utils_wxPrivacy = require("../../utils/wx-privacy.js");
const services_loginFlow = require("../../services/login-flow.js");
const services_appSession = require("../../services/app-session.js");
const services_userProfile = require("../../services/user-profile.js");
const utils_qrcodeScan = require("../../utils/qrcode-scan.js");
if (!Math) {
  PrivacyPopup();
}
const PrivacyPopup = () => "../../components/privacy/privacy-popup.js";
const _sfc_main = {
  __name: "login",
  setup(__props) {
    const loggingIn = common_vendor.ref(false);
    const errorMsg = common_vendor.ref("");
    const agreedToPrivacy = common_vendor.ref(false);
    const privacyContractName = common_vendor.ref("用户隐私保护指引");
    const privacyContractLabel = common_vendor.computed(
      () => utils_wxPrivacy.formatPrivacyContractLabel(privacyContractName.value)
    );
    common_vendor.onLoad((options) => {
      const launchInvite = utils_qrcodeScan.parseLaunchShopInvite(options);
      if (launchInvite == null ? void 0 : launchInvite.token) {
        utils_qrcodeScan.savePendingShopInvite(launchInvite.token);
      } else if (launchInvite == null ? void 0 : launchInvite.shopCode) {
        utils_qrcodeScan.savePendingShopCode(launchInvite.shopCode);
      }
      if (!api_modules_authToken.getToken())
        return;
      const target = services_userProfile.hasCompletedProfileSetup() ? "/pages/main/main" : "/pages/login/profile-setup";
      common_vendor.index.reLaunch({ url: target });
    });
    common_vendor.onReady(() => {
      if (api_modules_authToken.getToken())
        return;
      utils_wxPrivacy.getPrivacySettingState().then((state) => {
        privacyContractName.value = state.privacyContractName;
        if (!state.needAuthorization) {
          utils_wxPrivacy.wxPrivacyAuthorized.value = true;
        }
        if (utils_wxPrivacy.wxPrivacyAuthorized.value) {
          agreedToPrivacy.value = true;
          return;
        }
        if (state.needAuthorization) {
          utils_wxPrivacy.showPrivacyPopupManually();
        }
      });
    });
    common_vendor.watch(utils_wxPrivacy.wxPrivacyAuthorized, (newVal) => {
      if (newVal) {
        agreedToPrivacy.value = true;
      }
    });
    function onPrivacyToggle() {
      if (agreedToPrivacy.value) {
        agreedToPrivacy.value = false;
        return;
      }
      if (!utils_wxPrivacy.wxPrivacyAuthorized.value) {
        utils_wxPrivacy.showPrivacyPopupManually();
        return;
      }
      agreedToPrivacy.value = true;
    }
    function openPrivacyGuide() {
      utils_wxPrivacy.openPrivacyContract();
    }
    function onWxLoginTap() {
      if (loggingIn.value)
        return;
      errorMsg.value = "";
      if (!agreedToPrivacy.value) {
        common_vendor.index.showToast({ title: "请先勾选同意隐私政策", icon: "none" });
        return;
      }
      if (!utils_wxPrivacy.wxPrivacyAuthorized.value) {
        utils_wxPrivacy.showPrivacyPopupManually();
        common_vendor.index.showToast({ title: "请先同意隐私保护指引", icon: "none" });
        return;
      }
      runLogin();
    }
    async function runLogin() {
      if (loggingIn.value)
        return;
      loggingIn.value = true;
      try {
        const result = await services_loginFlow.performQuickLogin();
        if (!result.ok) {
          errorMsg.value = result.msg || "登录失败";
          common_vendor.index.showToast({ title: errorMsg.value, icon: "none" });
          return;
        }
        services_appSession.resetAppSession();
        common_vendor.index.reLaunch({ url: services_loginFlow.resolvePostLoginPath(result.register) });
      } catch (e) {
        errorMsg.value = e.message || "登录失败";
        common_vendor.index.showToast({ title: errorMsg.value, icon: "none" });
      } finally {
        loggingIn.value = false;
      }
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_assets._imports_0,
        b: common_vendor.t(loggingIn.value ? "登录中…" : "微信一键登录"),
        c: loggingIn.value,
        d: loggingIn.value || !agreedToPrivacy.value,
        e: common_vendor.o(onWxLoginTap, "a6"),
        f: agreedToPrivacy.value
      }, agreedToPrivacy.value ? {} : {}, {
        g: agreedToPrivacy.value ? 1 : "",
        h: common_vendor.t(privacyContractLabel.value),
        i: common_vendor.o(openPrivacyGuide, "d4"),
        j: common_vendor.o(onPrivacyToggle, "9f"),
        k: errorMsg.value
      }, errorMsg.value ? {
        l: common_vendor.t(errorMsg.value)
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e4e4508d"]]);
wx.createPage(MiniProgramPage);
