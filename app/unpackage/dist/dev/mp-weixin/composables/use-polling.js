"use strict";
const common_vendor = require("../common/vendor.js");
function usePolling(shouldPoll, tick, intervalMs) {
  let timer = null;
  function stop() {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
  }
  function start() {
    stop();
    if (!shouldPoll())
      return;
    timer = setInterval(() => {
      if (!shouldPoll()) {
        stop();
        return;
      }
      void tick();
    }, intervalMs);
  }
  common_vendor.onShow(() => start());
  common_vendor.onHide(() => stop());
  common_vendor.onUnload(() => stop());
  return { start, stop };
}
exports.usePolling = usePolling;
