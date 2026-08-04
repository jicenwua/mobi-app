"use strict";
require("../common/vendor.js");
function useDebounceFn(fn, delayMs = 300) {
  let timer = null;
  function cancel() {
    if (timer) {
      clearTimeout(timer);
      timer = null;
    }
  }
  function run(...args) {
    cancel();
    timer = setTimeout(() => {
      timer = null;
      fn(...args);
    }, delayMs);
  }
  return { run, cancel };
}
exports.useDebounceFn = useDebounceFn;
