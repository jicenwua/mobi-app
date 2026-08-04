"use strict";
const common_vendor = require("../common/vendor.js");
const DEFAULT_THRESHOLD = 80;
function useMpChatAutoScroll(options = {}) {
  const threshold = options.threshold ?? DEFAULT_THRESHOLD;
  const scrollTop = common_vendor.ref(0);
  const stickToBottom = common_vendor.ref(true);
  let viewHeight = 0;
  let programmaticScroll = false;
  let scrollToken = 0;
  function handleScroll(e) {
    if (programmaticScroll) {
      return;
    }
    const { scrollTop: top = 0, scrollHeight = 0 } = e.detail || {};
    if (viewHeight <= 0) {
      stickToBottom.value = true;
      return;
    }
    stickToBottom.value = scrollHeight - top - viewHeight <= threshold;
  }
  function measure(context, scrollSelector = ".message-scroll", listSelector = ".message-list") {
    return new Promise((resolve) => {
      if (!context) {
        resolve({ viewHeight: 0, listHeight: 0 });
        return;
      }
      const query = common_vendor.index.createSelectorQuery().in(context);
      query.select(scrollSelector).boundingClientRect();
      query.select(listSelector).boundingClientRect();
      query.exec((res) => {
        var _a, _b;
        viewHeight = ((_a = res == null ? void 0 : res[0]) == null ? void 0 : _a.height) || 0;
        resolve({
          viewHeight,
          listHeight: ((_b = res == null ? void 0 : res[1]) == null ? void 0 : _b.height) || 0
        });
      });
    });
  }
  function applyScrollTop(target) {
    const normalized = Math.max(0, Math.ceil(target));
    scrollTop.value = scrollTop.value === normalized ? normalized + 1 : normalized;
  }
  async function scrollToBottom(force = false, context) {
    if (!force && !stickToBottom.value) {
      return;
    }
    programmaticScroll = true;
    stickToBottom.value = true;
    const token = ++scrollToken;
    const attempt = async (delay = 0) => {
      if (token !== scrollToken) {
        return;
      }
      if (delay > 0) {
        await new Promise((resolve) => setTimeout(resolve, delay));
      }
      await common_vendor.nextTick$1();
      const { viewHeight: measuredViewHeight, listHeight } = await measure(context);
      if (measuredViewHeight > 0 && listHeight > 0) {
        applyScrollTop(listHeight - measuredViewHeight + 24);
        return;
      }
      applyScrollTop(999999);
    };
    await attempt(0);
    await attempt(100);
    await attempt(280);
    setTimeout(() => {
      if (token === scrollToken) {
        programmaticScroll = false;
      }
    }, 500);
  }
  return {
    scrollTop,
    stickToBottom,
    handleScroll,
    measure,
    scrollToBottom
  };
}
exports.useMpChatAutoScroll = useMpChatAutoScroll;
