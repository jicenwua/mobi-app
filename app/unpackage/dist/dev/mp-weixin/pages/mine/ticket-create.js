"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_ticket = require("../../api/modules/ticket.js");
const _sfc_main = {
  __name: "ticket-create",
  setup(__props) {
    const title = common_vendor.ref("");
    const description = common_vendor.ref("");
    const submitting = common_vendor.ref(false);
    const canSubmit = common_vendor.computed(() => title.value.trim().length > 0 && description.value.trim().length > 0);
    async function submit() {
      const trimmedTitle = title.value.trim();
      const trimmedDesc = description.value.trim();
      if (!trimmedTitle) {
        common_vendor.index.showToast({ title: "请输入工单标题", icon: "none" });
        return;
      }
      if (!trimmedDesc) {
        common_vendor.index.showToast({ title: "请输入问题描述", icon: "none" });
        return;
      }
      if (submitting.value)
        return;
      submitting.value = true;
      const res = await api_modules_ticket.createTicket({ title: trimmedTitle, description: trimmedDesc });
      submitting.value = false;
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "提交失败", icon: "none" });
        return;
      }
      common_vendor.index.showToast({ title: "提交成功", icon: "success" });
      setTimeout(() => {
        common_vendor.index.redirectTo({
          url: `/pages/mine/ticket-detail?id=${res.data}`,
          animationType: "slide-in-right",
          animationDuration: 200
        });
      }, 400);
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.t(title.value.length),
        b: submitting.value,
        c: title.value,
        d: common_vendor.o(($event) => title.value = $event.detail.value, "e0"),
        e: common_vendor.t(description.value.length),
        f: submitting.value,
        g: description.value,
        h: common_vendor.o(($event) => description.value = $event.detail.value, "02"),
        i: !canSubmit.value ? 1 : "",
        j: submitting.value,
        k: submitting.value || !canSubmit.value,
        l: common_vendor.o(submit, "69")
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-0643c6b3"]]);
wx.createPage(MiniProgramPage);
