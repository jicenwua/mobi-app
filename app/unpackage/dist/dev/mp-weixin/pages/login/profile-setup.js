"use strict";
const common_vendor = require("../../common/vendor.js");
const config_env = require("../../config/env.js");
require("../../utils/crypto-gateway.js");
const api_modules_authToken = require("../../api/modules/auth-token.js");
require("../../utils/wx-privacy.js");
const utils_avatar = require("../../utils/avatar.js");
const api_modules_user = require("../../api/modules/user.js");
const services_userProfile = require("../../services/user-profile.js");
const services_wxUserProfile = require("../../services/wx-user-profile.js");
const _sfc_main = {
  __name: "profile-setup",
  setup(__props) {
    const statusBarHeight = common_vendor.ref(0);
    const navBarStyle = common_vendor.computed(() => ({
      paddingTop: `${statusBarHeight.value}px`
    }));
    const avatarPath = common_vendor.ref("");
    const nickname = common_vendor.ref("");
    const saving = common_vendor.ref(false);
    const errorMsg = common_vendor.ref("");
    const avatarDisplayUrl = common_vendor.computed(() => {
      if (avatarPath.value)
        return avatarPath.value;
      return config_env.DEFAULT_AVATAR_URL;
    });
    common_vendor.onLoad(() => {
      statusBarHeight.value = common_vendor.index.getSystemInfoSync().statusBarHeight || 20;
      if (!api_modules_authToken.getToken()) {
        common_vendor.index.reLaunch({ url: "/pages/login/login" });
        return;
      }
      if (services_userProfile.hasCompletedProfileSetup()) {
        common_vendor.index.reLaunch({ url: "/pages/main/main" });
      }
    });
    function onChooseAvatar(e) {
      var _a;
      const url = (((_a = e.detail) == null ? void 0 : _a.avatarUrl) || "").trim();
      if (url)
        avatarPath.value = url;
    }
    function onNicknameInput(e) {
      var _a;
      nickname.value = ((_a = e.detail) == null ? void 0 : _a.value) ?? "";
    }
    function onNicknameBlur() {
      nickname.value = (nickname.value || "").trim();
    }
    async function onSubmit() {
      if (saving.value)
        return;
      errorMsg.value = "";
      const nickName = (nickname.value || "").trim();
      const localAvatar = (avatarPath.value || "").trim();
      const profile = { nickName, avatarUrl: localAvatar };
      if (!localAvatar) {
        errorMsg.value = "请先设置头像";
        common_vendor.index.showToast({ title: errorMsg.value, icon: "none" });
        return;
      }
      if (!nickName) {
        errorMsg.value = "请填写昵称";
        common_vendor.index.showToast({ title: errorMsg.value, icon: "none" });
        return;
      }
      if (services_wxUserProfile.isWxPlaceholderProfile(profile)) {
        errorMsg.value = "请填写真实昵称，不能使用「微信用户」";
        common_vendor.index.showToast({ title: errorMsg.value, icon: "none" });
        return;
      }
      saving.value = true;
      try {
        const result = await api_modules_user.updateUserInfo({ nickName, avatarPath: localAvatar });
        if (!result.ok) {
          errorMsg.value = result.msg || "保存失败";
          common_vendor.index.showToast({ title: errorMsg.value, icon: "none" });
          return;
        }
        const displayAvatar = utils_avatar.resolveAvatarUrl(localAvatar);
        services_userProfile.applyManualProfile({
          nickname: nickName,
          avatar: displayAvatar,
          isDefault: false
        });
        common_vendor.index.showToast({ title: "设置成功", icon: "success" });
        setTimeout(() => {
          common_vendor.index.reLaunch({ url: "/pages/main/main" });
        }, 350);
      } finally {
        saving.value = false;
      }
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.s(navBarStyle.value),
        b: avatarDisplayUrl.value
      }, avatarDisplayUrl.value ? {
        c: avatarDisplayUrl.value
      } : {}, {
        d: common_vendor.t(avatarPath.value ? "点击更换头像" : "点击设置头像"),
        e: common_vendor.o(onChooseAvatar, "10"),
        f: nickname.value,
        g: saving.value,
        h: common_vendor.o(onNicknameInput, "ab"),
        i: common_vendor.o(onNicknameBlur, "0e"),
        j: common_vendor.t(saving.value ? "保存中…" : "完成设置"),
        k: saving.value,
        l: saving.value,
        m: common_vendor.o(onSubmit, "52"),
        n: errorMsg.value
      }, errorMsg.value ? {
        o: common_vendor.t(errorMsg.value)
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e9f7049f"]]);
wx.createPage(MiniProgramPage);
