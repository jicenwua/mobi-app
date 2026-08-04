"use strict";
const common_vendor = require("../common/vendor.js");
function useExpiringQrcode(onExpire) {
  const countdownLeft = common_vendor.ref(0);
  const expireAt = common_vendor.ref(0);
  let tickTimer = null;
  function stopCountdown() {
    if (tickTimer) {
      clearInterval(tickTimer);
      tickTimer = null;
    }
  }
  function startCountdown(totalSeconds) {
    stopCountdown();
    const seconds = Math.max(1, Math.floor(totalSeconds));
    countdownLeft.value = seconds;
    tickTimer = setInterval(() => {
      if (countdownLeft.value <= 1) {
        countdownLeft.value = 0;
        stopCountdown();
        void (onExpire == null ? void 0 : onExpire());
        return;
      }
      countdownLeft.value -= 1;
    }, 1e3);
  }
  function syncFromExpireAt(at) {
    if (!at)
      return;
    expireAt.value = at;
    const left = Math.ceil((at - Date.now()) / 1e3);
    if (left > 0) {
      startCountdown(left);
    } else {
      countdownLeft.value = 0;
    }
  }
  common_vendor.onUnmounted(stopCountdown);
  return {
    countdownLeft,
    expireAt,
    stopCountdown,
    startCountdown,
    syncFromExpireAt
  };
}
exports.useExpiringQrcode = useExpiringQrcode;
