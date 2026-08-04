"use strict";
const common_vendor = require("../common/vendor.js");
const services_notifySocket = require("../services/notify-socket.js");
function useTicketNotify(handler) {
  let offNotify = null;
  function subscribe() {
    offNotify == null ? void 0 : offNotify();
    offNotify = services_notifySocket.onNotifyMessage(handler);
  }
  function unsubscribe() {
    offNotify == null ? void 0 : offNotify();
    offNotify = null;
  }
  common_vendor.onUnload(unsubscribe);
  return { subscribe, unsubscribe };
}
exports.useTicketNotify = useTicketNotify;
