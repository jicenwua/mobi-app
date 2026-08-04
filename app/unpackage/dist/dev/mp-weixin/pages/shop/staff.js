"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_shop = require("../../api/modules/shop.js");
const utils_shopRole = require("../../utils/shop-role.js");
const utils_wxPerm = require("../../utils/wx-perm.js");
const utils_staffScan = require("../../utils/staff-scan.js");
const utils_datetimeFormat = require("../../utils/datetime-format.js");
const utils_navigation = require("../../utils/navigation.js");
const utils_shopPageContext = require("../../utils/shop-page-context.js");
const _sfc_main = {
  __name: "staff",
  setup(__props) {
    const shopId = common_vendor.ref("");
    const shopInfo = common_vendor.ref(null);
    const rows = common_vendor.ref([]);
    const listLoading = common_vendor.ref(false);
    const refreshing = common_vendor.ref(false);
    const needReload = common_vendor.ref(false);
    common_vendor.onLoad((options) => {
      if (!utils_wxPerm.canManageShop()) {
        common_vendor.index.showToast({ title: "无店铺管理权限", icon: "none" });
        utils_navigation.navigateBackDelayed(800);
        return;
      }
      shopId.value = (options == null ? void 0 : options.shopId) ? String(options.shopId) : "";
      if (!shopId.value) {
        common_vendor.index.showToast({ title: "缺少店铺 ID", icon: "none" });
        return;
      }
      utils_shopPageContext.bindOpenerShop((data) => {
        shopInfo.value = data;
        if (!utils_shopRole.isShopManager(data)) {
          common_vendor.index.showToast({ title: "仅店长可管理店员", icon: "none" });
          utils_navigation.navigateBackDelayed(800);
        }
      });
      void reloadList();
    });
    common_vendor.onShow(() => {
      if (needReload.value && shopId.value) {
        needReload.value = false;
        void reloadList();
      }
    });
    async function reloadList() {
      listLoading.value = true;
      const res = await api_modules_shop.fetchShopStaffList(Number(shopId.value));
      listLoading.value = false;
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "加载失败", icon: "none" });
        return;
      }
      rows.value = res.rows || [];
    }
    async function onRefresh() {
      refreshing.value = true;
      await reloadList();
      refreshing.value = false;
    }
    function scanForAddStaff() {
      if (!shopId.value)
        return;
      utils_staffScan.scanStaffInviteCode({
        shopId: shopId.value,
        onAdded: () => {
          needReload.value = true;
          void reloadList();
        }
      });
    }
    function confirmRemove(item) {
      if (!(item == null ? void 0 : item.userId))
        return;
      common_vendor.index.showModal({
        title: "退出店铺",
        content: `确定将「${item.nickname || "该店员"}」移出店铺吗？移除后将失去店员权限。`,
        success: async (r) => {
          if (!r.confirm)
            return;
          const res = await api_modules_shop.removeShopStaff({
            shopId: Number(shopId.value),
            userId: item.userId
          });
          if (!res.ok) {
            common_vendor.index.showToast({ title: res.msg || "操作失败", icon: "none" });
            return;
          }
          common_vendor.index.showToast({ title: "已移除店员", icon: "success" });
          void reloadList();
        }
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(scanForAddStaff, "22"),
        b: listLoading.value && !rows.value.length
      }, listLoading.value && !rows.value.length ? {} : !rows.value.length ? {} : {}, {
        c: !rows.value.length,
        d: common_vendor.f(rows.value, (item, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(item.nickname || "店员"),
            b: item.phone
          }, item.phone ? {
            c: common_vendor.t(item.phone)
          } : {}, {
            d: item.createTime
          }, item.createTime ? {
            e: common_vendor.t(common_vendor.unref(utils_datetimeFormat.formatDateTime)(item.createTime))
          } : {}, {
            f: common_vendor.o(($event) => confirmRemove(item), item.userId),
            g: item.userId
          });
        }),
        e: refreshing.value,
        f: common_vendor.o(onRefresh, "ec")
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-994cbb2a"]]);
wx.createPage(MiniProgramPage);
