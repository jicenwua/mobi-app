"use strict";
const common_vendor = require("../../common/vendor.js");
const composables_useExpiringQrcode = require("../../composables/use-expiring-qrcode.js");
const api_modules_user = require("../../api/modules/user.js");
const api_modules_qrcode = require("../../api/modules/qrcode.js");
const services_userProfile = require("../../services/user-profile.js");
const services_profileCache = require("../../services/profile-cache.js");
const utils_avatar = require("../../utils/avatar.js");
const utils_navigation = require("../../utils/navigation.js");
if (!Math) {
  (ProfileForm + PageLoading + QrcodeCanvas)();
}
const ProfileForm = () => "../../components/profile/profile-form.js";
const QrcodeCanvas = () => "../../components/common/qrcode-canvas.js";
const PageLoading = () => "../../components/common/page-loading.js";
const _sfc_main = {
  __name: "profile",
  setup(__props) {
    const cached = common_vendor.ref(null);
    const initialAvatar = common_vendor.ref("");
    const initialNickname = common_vendor.ref("");
    const saving = common_vendor.ref(false);
    const staffQrContent = common_vendor.ref("");
    const staffQrLoading = common_vendor.ref(true);
    const staffQrError = common_vendor.ref("");
    let staffQrInFlight = false;
    let profilePageReady = false;
    const {
      countdownLeft: staffCountdownLeft,
      expireAt: staffExpireAt,
      stopCountdown: stopStaffCountdown,
      syncFromExpireAt: syncStaffCountdownFromExpireAt
    } = composables_useExpiringQrcode.useExpiringQrcode(() => {
      void refreshStaffQr();
    });
    const staffExpireHint = common_vendor.computed(() => {
      if (!staffQrContent.value || staffCountdownLeft.value <= 0)
        return "";
      return `${staffCountdownLeft.value} 秒后自动刷新`;
    });
    function initProfilePage() {
      var _a, _b, _c;
      if (profilePageReady)
        return;
      profilePageReady = true;
      cached.value = services_profileCache.takeProfileEditCache();
      initialAvatar.value = ((_a = cached.value) == null ? void 0 : _a.avatar) || ((_b = cached.value) == null ? void 0 : _b.avatarUrl) || "";
      initialNickname.value = ((_c = cached.value) == null ? void 0 : _c.nickname) || "";
    }
    function ensureStaffQrLoaded() {
      if (staffQrInFlight)
        return;
      if (staffQrContent.value && staffCountdownLeft.value > 0)
        return;
      void refreshStaffQr();
    }
    common_vendor.onLoad(() => {
      initProfilePage();
      ensureStaffQrLoaded();
    });
    common_vendor.onShow(() => {
      initProfilePage();
      ensureStaffQrLoaded();
    });
    common_vendor.onHide(() => {
      stopStaffCountdown();
    });
    common_vendor.onUnload(() => {
      stopStaffCountdown();
    });
    async function refreshStaffQr() {
      var _a;
      if (staffQrInFlight)
        return;
      staffQrInFlight = true;
      staffQrLoading.value = true;
      staffQrError.value = "";
      try {
        const res = await api_modules_qrcode.generateStaffInviteQrcode();
        if (!res.ok || !((_a = res.data) == null ? void 0 : _a.qrContent)) {
          staffQrContent.value = "";
          staffQrError.value = res.msg || "生成失败";
          stopStaffCountdown();
          return;
        }
        staffQrContent.value = res.data.qrContent;
        staffExpireAt.value = res.data.expireAt || 0;
        syncStaffCountdownFromExpireAt(staffExpireAt.value);
      } finally {
        staffQrInFlight = false;
        staffQrLoading.value = false;
      }
    }
    async function onFormSubmit({ nickname, avatarPath }) {
      var _a, _b, _c, _d;
      const name = (nickname || "").trim();
      if (!name)
        return;
      const nextAvatar = utils_avatar.toServerAvatar(avatarPath);
      const prevAvatar = utils_avatar.toServerAvatar(((_a = cached.value) == null ? void 0 : _a.avatar) || ((_b = cached.value) == null ? void 0 : _b.avatarUrl) || "");
      const unchanged = name === (((_c = cached.value) == null ? void 0 : _c.nickname) || "").trim() && nextAvatar === prevAvatar;
      if (unchanged) {
        common_vendor.index.navigateBack();
        return;
      }
      saving.value = true;
      try {
        const result = await api_modules_user.updateUserInfo({ nickName: name, avatarPath });
        if (!result.ok) {
          common_vendor.index.showToast({ title: result.msg || "保存失败", icon: "none" });
          return;
        }
        const displayAvatar = utils_avatar.resolveAvatarUrl(nextAvatar || ((_d = cached.value) == null ? void 0 : _d.avatar) || "");
        services_userProfile.applyManualProfile({
          nickname: name,
          avatar: displayAvatar,
          isDefault: !nextAvatar
        });
        common_vendor.index.showToast({ title: "已保存", icon: "success" });
        utils_navigation.navigateBackDelayed(350);
      } finally {
        saving.value = false;
      }
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(onFormSubmit, "62"),
        b: common_vendor.p({
          ["submit-text"]: "保存",
          loading: saving.value,
          ["initial-avatar"]: initialAvatar.value,
          ["initial-nickname"]: initialNickname.value
        }),
        c: staffQrLoading.value
      }, staffQrLoading.value ? {
        d: common_vendor.p({
          text: "生成中…",
          size: 32,
          color: "#07c160"
        })
      } : staffQrContent.value ? {
        f: common_vendor.p({
          text: staffQrContent.value,
          size: 200,
          ["canvas-id"]: "staff-invite-qrcode"
        }),
        g: common_vendor.t(staffExpireHint.value)
      } : {
        h: common_vendor.t(staffQrError.value || "无法生成邀请码")
      }, {
        e: staffQrContent.value,
        i: staffQrLoading.value ? 1 : "",
        j: common_vendor.o(refreshStaffQr, "ed")
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-935803c6"]]);
wx.createPage(MiniProgramPage);
