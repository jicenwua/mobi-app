"use strict";
const common_vendor = require("../../common/vendor.js");
if (!Math) {
  PageLoading();
}
const PageLoading = () => "../common/page-loading.js";
const _sfc_main = {
  __name: "pay-pin-pad",
  props: {
    title: { type: String, default: "请输入支付密码" },
    /** 外部校验失败时展示 */
    errorHint: { type: String, default: "" },
    /** 递增以强制清空输入格（避免错误文案相同时 watch 不触发） */
    resetKey: { type: Number, default: 0 },
    /** 提交接口请求中，禁用键盘并展示等待动画 */
    busy: { type: Boolean, default: false },
    /** 等待遮罩文案 */
    busyText: { type: String, default: "支付处理中…" }
  },
  emits: ["complete", "cancel"],
  setup(__props, { expose: __expose, emit: __emit }) {
    const props = __props;
    const emit = __emit;
    const digits = common_vendor.ref("");
    const keypadRows = [
      [
        { id: "1", label: "1" },
        { id: "2", label: "2" },
        { id: "3", label: "3" }
      ],
      [
        { id: "4", label: "4" },
        { id: "5", label: "5" },
        { id: "6", label: "6" }
      ],
      [
        { id: "7", label: "7" },
        { id: "8", label: "8" },
        { id: "9", label: "9" }
      ],
      [
        { id: "blank", blank: true },
        { id: "0", label: "0" },
        { id: "del", action: true, label: "⌫" }
      ]
    ];
    function clearDigits() {
      digits.value = "";
    }
    common_vendor.watch(
      () => props.resetKey,
      () => {
        clearDigits();
      }
    );
    common_vendor.watch(
      () => props.errorHint,
      (hint) => {
        if (hint) {
          clearDigits();
        }
      }
    );
    function onKeyTap(key) {
      if (props.busy)
        return;
      if (key.blank)
        return;
      if (key.action) {
        digits.value = digits.value.slice(0, -1);
        return;
      }
      if (digits.value.length >= 6)
        return;
      digits.value += key.label;
      if (digits.value.length === 6) {
        emit("complete", digits.value);
      }
    }
    function onCancel() {
      emit("cancel");
    }
    function reset() {
      clearDigits();
    }
    __expose({ reset });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(onCancel, "fe"),
        b: common_vendor.t(__props.title),
        c: common_vendor.f(6, (i, k0, i0) => {
          return {
            a: i,
            b: digits.value.length >= i ? 1 : ""
          };
        }),
        d: __props.errorHint
      }, __props.errorHint ? {
        e: common_vendor.t(__props.errorHint)
      } : {}, {
        f: common_vendor.f(keypadRows, (row, ri, i0) => {
          return {
            a: common_vendor.f(row, (key, k1, i1) => {
              return common_vendor.e({
                a: key.label
              }, key.label ? {
                b: common_vendor.t(key.label)
              } : {}, {
                c: key.id,
                d: key.blank ? 1 : "",
                e: key.action ? 1 : "",
                f: common_vendor.o(($event) => onKeyTap(key), key.id)
              });
            }),
            b: ri
          };
        }),
        g: __props.busy ? 1 : "",
        h: __props.busy
      }, __props.busy ? {
        i: common_vendor.p({
          text: __props.busyText,
          inline: false,
          size: 28
        })
      } : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-71a134d9"]]);
wx.createComponent(Component);
