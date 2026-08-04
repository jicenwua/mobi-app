"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_ticket = require("../../api/modules/ticket.js");
const services_notifySocket = require("../../services/notify-socket.js");
const composables_useTicketNotify = require("../../composables/use-ticket-notify.js");
const utils_chatAutoScroll = require("../../utils/chat-auto-scroll.js");
const utils_datetimeFormat = require("../../utils/datetime-format.js");
const utils_ticketStatus = require("../../utils/ticket-status.js");
const utils_avatar = require("../../utils/avatar.js");
const config_env = require("../../config/env.js");
const services_userProfile = require("../../services/user-profile.js");
const _sfc_main = {
  __name: "ticket-detail",
  setup(__props) {
    const STAFF_AVATAR_URL = config_env.SERVER_DEFAULT_AVATAR_URL;
    const ticketId = common_vendor.ref(null);
    const detail = common_vendor.ref(null);
    const loading = common_vendor.ref(false);
    const sending = common_vendor.ref(false);
    const replyContent = common_vendor.ref("");
    const scrollWithAnimation = common_vendor.ref(false);
    const pendingMessages = [];
    const instance = common_vendor.getCurrentInstance();
    const pageContext = common_vendor.computed(() => instance == null ? void 0 : instance.proxy);
    const { scrollTop, handleScroll, measure, scrollToBottom } = utils_chatAutoScroll.useMpChatAutoScroll();
    const { subscribe: subscribeTicketNotify } = composables_useTicketNotify.useTicketNotify(handleNotifyPayload);
    const canReply = common_vendor.computed(() => detail.value && detail.value.status !== 2);
    common_vendor.onLoad((options) => {
      ticketId.value = Number(options == null ? void 0 : options.id);
      if (!ticketId.value) {
        common_vendor.index.showToast({ title: "工单不存在", icon: "none" });
        return;
      }
      services_notifySocket.connectNotifySocket();
      subscribeTicketNotify();
      void loadDetail();
    });
    common_vendor.onReady(() => {
      if (detail.value) {
        void forceScrollToBottom();
      }
    });
    common_vendor.onShow(() => {
      services_notifySocket.connectNotifySocket();
      if (detail.value) {
        void forceScrollToBottom();
      }
    });
    common_vendor.watch(
      () => {
        var _a, _b;
        return ((_b = (_a = detail.value) == null ? void 0 : _a.messages) == null ? void 0 : _b.length) ?? 0;
      },
      () => {
        if (!detail.value || loading.value) {
          return;
        }
        scrollWithAnimation.value = true;
        void scrollToBottom(false, pageContext.value);
      }
    );
    common_vendor.watch(loading, (isLoading, wasLoading) => {
      if (wasLoading && !isLoading && detail.value) {
        void forceScrollToBottom();
      }
    });
    async function forceScrollToBottom() {
      scrollWithAnimation.value = false;
      await measure(pageContext.value);
      await scrollToBottom(true, pageContext.value);
    }
    function statusClass(status) {
      return utils_ticketStatus.ticketStatusClass(status);
    }
    function isSelfMessage(msg) {
      return msg.senderType === 1;
    }
    function resolveAvatar(msg) {
      if (msg.senderType === 2) {
        return msg.senderAvatar || STAFF_AVATAR_URL;
      }
      return utils_avatar.resolveAvatarUrl(msg.senderAvatar);
    }
    function normalizeCreateTime(value) {
      if (Array.isArray(value) && value.length >= 3) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = value;
        const pad = (n) => String(n).padStart(2, "0");
        return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}:${pad(second)}`;
      }
      return value;
    }
    function appendMessage(payload) {
      var _a;
      if (!(payload == null ? void 0 : payload.messageId) || Number(payload.ticketId) !== Number(ticketId.value)) {
        return;
      }
      if (!detail.value) {
        pendingMessages.push(payload);
        return;
      }
      const exists = (_a = detail.value.messages) == null ? void 0 : _a.some((item) => item.messageId === payload.messageId);
      if (exists) {
        return;
      }
      detail.value.messages = [
        ...detail.value.messages || [],
        {
          messageId: payload.messageId,
          senderType: payload.senderType,
          senderLabel: payload.senderLabel,
          senderId: payload.senderId,
          senderAvatar: payload.senderAvatar,
          content: payload.content,
          createTime: normalizeCreateTime(payload.createTime)
        }
      ];
    }
    function handleNotifyPayload(payload) {
      if (Number(payload == null ? void 0 : payload.ticketId) !== Number(ticketId.value)) {
        return;
      }
      if ((payload == null ? void 0 : payload.type) === "ticket_message") {
        appendMessage(payload);
        return;
      }
      if ((payload == null ? void 0 : payload.type) === "ticket_status_changed") {
        applyStatusChanged(payload);
      }
    }
    function applyStatusChanged(payload) {
      if (!detail.value) {
        return;
      }
      if (payload.status != null) {
        detail.value.status = payload.status;
      }
      if (payload.statusLabel) {
        detail.value.statusLabel = payload.statusLabel;
      }
    }
    function flushPendingMessages() {
      if (!detail.value || !pendingMessages.length) {
        return;
      }
      const queue = pendingMessages.splice(0, pendingMessages.length);
      queue.forEach((payload) => appendMessage(payload));
    }
    async function loadDetail() {
      loading.value = true;
      const res = await api_modules_ticket.fetchTicketDetail(ticketId.value);
      loading.value = false;
      if (!res.ok || !res.data) {
        common_vendor.index.showToast({ title: res.msg || "加载失败", icon: "none" });
        return;
      }
      detail.value = res.data;
      flushPendingMessages();
    }
    async function sendReply() {
      const content = replyContent.value.trim();
      if (!content) {
        common_vendor.index.showToast({ title: "请输入内容", icon: "none" });
        return;
      }
      if (sending.value)
        return;
      sending.value = true;
      const res = await api_modules_ticket.replyTicket(ticketId.value, content);
      sending.value = false;
      if (!res.ok) {
        common_vendor.index.showToast({ title: res.msg || "发送失败", icon: "none" });
        return;
      }
      replyContent.value = "";
      const profile = services_userProfile.getUserProfile();
      const messageId = Number(res.data);
      if (messageId) {
        appendMessage({
          ticketId: ticketId.value,
          messageId,
          senderType: 1,
          senderLabel: (profile == null ? void 0 : profile.nickname) || "我",
          senderId: profile == null ? void 0 : profile.userId,
          senderAvatar: profile == null ? void 0 : profile.avatar,
          content,
          createTime: (/* @__PURE__ */ new Date()).toISOString().slice(0, 19).replace("T", " ")
        });
        scrollWithAnimation.value = true;
        await scrollToBottom(true, pageContext.value);
        return;
      }
      await loadDetail();
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: loading.value
      }, loading.value ? {} : detail.value ? common_vendor.e({
        c: common_vendor.t(detail.value.title),
        d: common_vendor.t(detail.value.statusLabel),
        e: common_vendor.n(statusClass(detail.value.status)),
        f: common_vendor.f(detail.value.messages, (msg, k0, i0) => {
          return common_vendor.e({
            a: !isSelfMessage(msg)
          }, !isSelfMessage(msg) ? {
            b: common_vendor.t(msg.senderLabel),
            c: resolveAvatar(msg)
          } : {}, {
            d: common_vendor.t(msg.content),
            e: common_vendor.t(common_vendor.unref(utils_datetimeFormat.formatDateTime)(msg.createTime)),
            f: isSelfMessage(msg)
          }, isSelfMessage(msg) ? {
            g: common_vendor.t(msg.senderLabel),
            h: resolveAvatar(msg)
          } : {}, {
            i: msg.messageId,
            j: common_vendor.n(isSelfMessage(msg) ? "message-row--self" : "message-row--other")
          });
        }),
        g: common_vendor.unref(scrollTop),
        h: scrollWithAnimation.value,
        i: common_vendor.o((...args) => common_vendor.unref(handleScroll) && common_vendor.unref(handleScroll)(...args), "c1"),
        j: canReply.value
      }, canReply.value ? {
        k: common_vendor.o(sendReply, "b3"),
        l: replyContent.value,
        m: common_vendor.o(($event) => replyContent.value = $event.detail.value, "f8"),
        n: sending.value,
        o: sending.value,
        p: common_vendor.o(sendReply, "47")
      } : {}) : {}, {
        b: detail.value
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-d37a2e95"]]);
wx.createPage(MiniProgramPage);
