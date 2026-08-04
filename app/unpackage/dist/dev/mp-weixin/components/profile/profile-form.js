"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_avatar = require("../../utils/avatar.js");
const _sfc_main = {
  __name: "profile-form",
  props: {
    submitText: { type: String, default: "确认" },
    loading: { type: Boolean, default: false },
    initialAvatar: { type: String, default: "" },
    initialNickname: { type: String, default: "" }
  },
  emits: ["submit"],
  setup(__props, { emit: __emit }) {
    const props = __props;
    const emit = __emit;
    const nickname = common_vendor.ref((props.initialNickname || "").trim());
    const avatarPath = common_vendor.ref("");
    const initialAvatar = common_vendor.toRef(props, "initialAvatar");
    common_vendor.watch(
      () => props.initialNickname,
      (v) => {
        nickname.value = (v || "").trim();
      }
    );
    common_vendor.watch(
      () => props.initialAvatar,
      () => {
        avatarPath.value = "";
      }
    );
    const avatarLetter = common_vendor.computed(() => {
      const name = (nickname.value || "").trim();
      return name ? name.slice(0, 1) : "我";
    });
    const avatarDisplayUrl = common_vendor.computed(() => {
      const picked = (avatarPath.value || "").trim();
      if (picked)
        return picked;
      const url = (initialAvatar.value || "").trim();
      if (url && !utils_avatar.isDefaultAvatarUrl(url))
        return url;
      return "";
    });
    const avatarChanged = common_vendor.computed(() => !!(avatarPath.value || "").trim());
    const avatarHint = common_vendor.computed(() => {
      if (avatarChanged.value)
        return "已选择新头像，保存后生效";
      if (avatarDisplayUrl.value)
        return "点击头像可更换";
      return "点击设置头像";
    });
    const canSubmit = common_vendor.computed(() => !!(nickname.value || "").trim());
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
    function onFormSubmit() {
      const name = (nickname.value || "").trim();
      if (!name) {
        common_vendor.index.showToast({ title: "请输入昵称", icon: "none" });
        return;
      }
      const picked = (avatarPath.value || "").trim();
      const avatar = picked || (initialAvatar.value || "").trim();
      emit("submit", {
        nickname: name,
        avatarPath: avatar
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: avatarDisplayUrl.value
      }, avatarDisplayUrl.value ? {
        b: avatarDisplayUrl.value
      } : {
        c: common_vendor.t(avatarLetter.value)
      }, {
        d: common_vendor.t(avatarHint.value),
        e: __props.loading,
        f: common_vendor.o(onChooseAvatar, "bf"),
        g: nickname.value,
        h: __props.loading,
        i: common_vendor.o(onNicknameInput, "2d"),
        j: common_vendor.o(onNicknameBlur, "05"),
        k: common_vendor.t(__props.loading ? "保存中…" : __props.submitText),
        l: __props.loading,
        m: __props.loading || !canSubmit.value,
        n: common_vendor.o(onFormSubmit, "10")
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-332bca4b"]]);
wx.createComponent(Component);
