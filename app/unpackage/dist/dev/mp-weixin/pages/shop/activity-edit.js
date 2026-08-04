"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_shopManage = require("../../api/modules/shop-manage.js");
const api_modules_shop = require("../../api/modules/shop.js");
const utils_shopRole = require("../../utils/shop-role.js");
const utils_navigation = require("../../utils/navigation.js");
const _sfc_main = {
  __name: "activity-edit",
  setup(__props) {
    const shopId = common_vendor.ref("");
    const activityId = common_vendor.ref("");
    const kindLabels = ["充值满赠", "消费满赠", "公告"];
    const kindIndex = common_vendor.ref(0);
    const isAnnouncement = common_vendor.computed(() => kindIndex.value === 2);
    const pageTitle = common_vendor.computed(() => activityId.value ? "修改活动" : "新建活动");
    const submitLabel = common_vendor.computed(() => activityId.value ? "保存修改" : "创建活动");
    const form = common_vendor.reactive({
      activityName: "",
      description: "",
      startDate: "",
      endDate: "",
      rules: [{ thresholdPoints: "", giftPoints: "" }]
    });
    function denyAndBack(message) {
      common_vendor.index.showToast({ title: message, icon: "none" });
      utils_navigation.navigateBackDelayed(800, 1);
    }
    common_vendor.onLoad(async (options) => {
      shopId.value = (options == null ? void 0 : options.shopId) || "";
      activityId.value = (options == null ? void 0 : options.activityId) || "";
      if (!shopId.value) {
        denyAndBack("缺少店铺 ID");
        return;
      }
      const shopRes = await api_modules_shop.fetchShopFromUserList(shopId.value);
      if (!shopRes.ok || !utils_shopRole.isShopManager(shopRes.data)) {
        denyAndBack("仅店长可编辑活动");
        return;
      }
      if (!activityId.value) {
        common_vendor.index.setNavigationBarTitle({ title: "新建活动" });
      }
      if (activityId.value) {
        common_vendor.index.setNavigationBarTitle({ title: "修改活动" });
        const res = await api_modules_shopManage.fetchManageActivityDetail(Number(activityId.value));
        if (!res.ok || !res.data) {
          common_vendor.index.showToast({ title: res.msg || "加载失败", icon: "none" });
          return;
        }
        const d = res.data;
        form.activityName = d.activityName || "";
        form.description = d.description || "";
        if (d.kind === 2) {
          kindIndex.value = 2;
        } else {
          kindIndex.value = d.activityType === 2 ? 1 : 0;
        }
        form.startDate = toDateStr(d.startTime);
        form.endDate = toDateStr(d.endTime);
        form.rules = (d.rules || []).map((r) => ({
          thresholdPoints: r.thresholdAmount != null ? String(r.thresholdAmount) : "",
          giftPoints: r.giftPoints != null ? String(r.giftPoints) : ""
        }));
        if (!form.rules.length) {
          form.rules = [{ thresholdPoints: "", giftPoints: "" }];
        }
      }
    });
    function onKindChange(e) {
      kindIndex.value = Number(e.detail.value);
    }
    function addRule() {
      form.rules.push({ thresholdPoints: "", giftPoints: "" });
    }
    function removeRule(idx) {
      form.rules.splice(idx, 1);
    }
    function toDateStr(iso) {
      if (!iso)
        return "";
      return String(iso).slice(0, 10);
    }
    function toDateTime(dateStr, endOfDay) {
      if (!dateStr)
        return null;
      return endOfDay ? `${dateStr}T23:59:59` : `${dateStr}T00:00:00`;
    }
    async function onSubmit() {
      if (!form.activityName.trim()) {
        common_vendor.index.showToast({ title: "请输入活动名称", icon: "none" });
        return;
      }
      if (isAnnouncement.value && !form.description.trim()) {
        common_vendor.index.showToast({ title: "请输入公告正文", icon: "none" });
        return;
      }
      if (form.startDate && form.endDate && form.startDate > form.endDate) {
        common_vendor.index.showToast({ title: "结束时间不能早于开始时间", icon: "none" });
        return;
      }
      const rules = [];
      if (!isAnnouncement.value) {
        for (const r of form.rules) {
          const threshold = api_modules_shopManage.parseProductPoints(r.thresholdPoints);
          const pts = api_modules_shopManage.parseProductPoints(r.giftPoints);
          if (!threshold || !pts) {
            common_vendor.index.showToast({ title: "请填写有效的规则（积分为正整数）", icon: "none" });
            return;
          }
          rules.push({ thresholdAmount: threshold, giftPoints: pts });
        }
      }
      const payload = {
        activityId: activityId.value ? Number(activityId.value) : void 0,
        shopId: Number(shopId.value),
        activityName: form.activityName.trim(),
        description: form.description.trim() || null,
        kind: isAnnouncement.value ? 2 : 1,
        activityType: isAnnouncement.value ? void 0 : kindIndex.value === 1 ? 2 : 1,
        startTime: toDateTime(form.startDate, false),
        endTime: toDateTime(form.endDate, true),
        rules: isAnnouncement.value ? [] : rules
      };
      common_vendor.index.showLoading({ title: "保存中…", mask: true });
      const res = await api_modules_shopManage.saveManageActivity(payload);
      common_vendor.index.hideLoading();
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "保存失败", icon: "none" });
        return;
      }
      common_vendor.index.showToast({ title: "保存成功", icon: "success" });
      utils_navigation.navigateBackDelayed(500);
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.t(pageTitle.value),
        b: common_vendor.t(isAnnouncement.value ? "配置公告内容与展示周期" : "配置活动信息与满赠规则"),
        c: form.activityName,
        d: common_vendor.o(($event) => form.activityName = $event.detail.value, "73"),
        e: common_vendor.t(kindLabels[kindIndex.value]),
        f: kindLabels,
        g: kindIndex.value,
        h: common_vendor.o(onKindChange, "32"),
        i: common_vendor.t(isAnnouncement.value ? "公告正文" : "活动描述"),
        j: common_vendor.t(isAnnouncement.value ? "必填，将展示给店铺成员" : "选填，补充活动说明"),
        k: isAnnouncement.value ? "请输入公告内容" : "请输入活动描述（选填）",
        l: form.description,
        m: common_vendor.o(($event) => form.description = $event.detail.value, "0f"),
        n: common_vendor.t(form.startDate || "立即开始"),
        o: !form.startDate ? 1 : "",
        p: form.startDate,
        q: common_vendor.o(($event) => form.startDate = $event.detail.value, "a5"),
        r: common_vendor.t(form.endDate || "永久有效"),
        s: !form.endDate ? 1 : "",
        t: form.endDate,
        v: common_vendor.o(($event) => form.endDate = $event.detail.value, "ae"),
        w: form.startDate || form.endDate
      }, form.startDate || form.endDate ? common_vendor.e({
        x: form.startDate
      }, form.startDate ? {
        y: common_vendor.o(($event) => form.startDate = "", "e1")
      } : {}, {
        z: form.endDate
      }, form.endDate ? {
        A: common_vendor.o(($event) => form.endDate = "", "2e")
      } : {}) : {}, {
        B: !isAnnouncement.value
      }, !isAnnouncement.value ? common_vendor.e({
        C: common_vendor.o(addRule, "e3"),
        D: !form.rules.length
      }, !form.rules.length ? {} : {
        E: common_vendor.f(form.rules, (rule, idx, i0) => {
          return common_vendor.e({
            a: common_vendor.t(idx + 1)
          }, form.rules.length > 1 ? {
            b: common_vendor.o(($event) => removeRule(idx), idx)
          } : {}, {
            c: rule.thresholdPoints,
            d: common_vendor.o(($event) => rule.thresholdPoints = $event.detail.value, idx),
            e: rule.giftPoints,
            f: common_vendor.o(($event) => rule.giftPoints = $event.detail.value, idx),
            g: idx
          });
        }),
        F: form.rules.length > 1
      }) : {}, {
        G: common_vendor.t(submitLabel.value),
        H: common_vendor.o(onSubmit, "c5")
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-af6b3570"]]);
wx.createPage(MiniProgramPage);
